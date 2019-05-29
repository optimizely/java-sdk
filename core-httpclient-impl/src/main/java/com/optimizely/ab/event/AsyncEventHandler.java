/**
 *
 *    Copyright 2016-2019, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.event;

import com.optimizely.ab.NamedThreadFactory;
import com.optimizely.ab.OptimizelyHttpClient;
import com.optimizely.ab.annotations.VisibleForTesting;

import com.optimizely.ab.internal.PropertyUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;

/**
 * {@link EventHandler} implementation that queues events and has a separate pool of threads responsible
 * for the dispatch.
 */
public class AsyncEventHandler implements EventHandler, AutoCloseable {

    public static final String CONFIG_QUEUE_CAPACITY            = "async.event.handler.queue.capacity";
    public static final String CONFIG_NUM_WORKERS               = "async.event.handler.num.workers";
    public static final String CONFIG_MAX_CONNECTIONS           = "async.event.handler.max.connections";
    public static final String CONFIG_MAX_PER_ROUTE             = "async.event.handler.event.max.per.route";
    public static final String CONFIG_VALIDATE_AFTER_INACTIVITY = "async.event.handler.validate.after";

    public static final int DEFAULT_QUEUE_CAPACITY = 10000;
    public static final int DEFAULT_NUM_WORKERS = 2;
    public static final int DEFAULT_MAX_CONNECTIONS = 200;
    public static final int DEFAULT_MAX_PER_ROUTE = 20;
    public static final int DEFAULT_VALIDATE_AFTER_INACTIVITY = 5000;

    private static final Logger logger = LoggerFactory.getLogger(AsyncEventHandler.class);
    private static final ProjectConfigResponseHandler EVENT_RESPONSE_HANDLER = new ProjectConfigResponseHandler();

    private final OptimizelyHttpClient httpClient;
    private final ExecutorService workerExecutor;

    private final long closeTimeout;
    private final TimeUnit closeTimeoutUnit;

    /**
     * @deprecated Use the builder {@link Builder}
     */
    @Deprecated
    public AsyncEventHandler(int queueCapacity,
                             int numWorkers) {
        this(queueCapacity, numWorkers, 200, 20, 5000);
    }

    /**
     * @deprecated Use the builder {@link Builder}
     */
    @Deprecated
    public AsyncEventHandler(int queueCapacity,
                             int numWorkers,
                             int maxConnections,
                             int connectionsPerRoute,
                             int validateAfter) {
        this(queueCapacity, numWorkers, maxConnections, connectionsPerRoute, validateAfter, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public AsyncEventHandler(int queueCapacity,
                             int numWorkers,
                             int maxConnections,
                             int connectionsPerRoute,
                             int validateAfter,
                             long closeTimeout,
                             TimeUnit closeTimeoutUnit) {

        queueCapacity       = validateInput("queueCapacity", queueCapacity, DEFAULT_QUEUE_CAPACITY);
        numWorkers          = validateInput("numWorkers", numWorkers, DEFAULT_NUM_WORKERS);
        maxConnections      = validateInput("maxConnections", maxConnections, DEFAULT_MAX_CONNECTIONS);
        connectionsPerRoute = validateInput("connectionsPerRoute", connectionsPerRoute, DEFAULT_MAX_PER_ROUTE);
        validateAfter       = validateInput("validateAfter", validateAfter, DEFAULT_VALIDATE_AFTER_INACTIVITY);

        this.httpClient = OptimizelyHttpClient.builder()
            .withMaxTotalConnections(maxConnections)
            .withMaxPerRoute(connectionsPerRoute)
            .withValidateAfterInactivity(validateAfter)
            .build();

        this.workerExecutor = new ThreadPoolExecutor(numWorkers, numWorkers,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(queueCapacity),
            new NamedThreadFactory("optimizely-event-dispatcher-thread-%s", true));

        this.closeTimeout = closeTimeout;
        this.closeTimeoutUnit = closeTimeoutUnit;
    }

    @VisibleForTesting
    public AsyncEventHandler(OptimizelyHttpClient httpClient, ExecutorService workerExecutor) {
        this.httpClient = httpClient;
        this.workerExecutor = workerExecutor;
        this.closeTimeout = Long.MAX_VALUE;
        this.closeTimeoutUnit = TimeUnit.MILLISECONDS;
    }

    @Override
    public void dispatchEvent(LogEvent logEvent) {
        try {
            // attempt to enqueue the log event for processing
            workerExecutor.execute(new EventDispatcher(logEvent));
        } catch (RejectedExecutionException e) {
            logger.error("event dispatch rejected");
        }
    }

    /**
     * Attempts to gracefully terminate all event dispatch workers and close all resources.
     * This method blocks, awaiting the completion of any queued or ongoing event dispatches.
     * <p>
     * Note: termination of ongoing event dispatching is best-effort.
     *
     * @param timeout maximum time to wait for event dispatches to complete
     * @param unit    the time unit of the timeout argument
     */
    public void shutdownAndAwaitTermination(long timeout, TimeUnit unit) {

        // Disable new tasks from being submitted
        logger.info("event handler shutting down. Attempting to dispatch previously submitted events");
        workerExecutor.shutdown();

        try {
            // Wait a while for existing tasks to terminate
            if (!workerExecutor.awaitTermination(timeout, unit)) {
                int unprocessedCount = workerExecutor.shutdownNow().size();
                logger.warn("timed out waiting for previously submitted events to be dispatched. "
                    + "{} events were dropped. "
                    + "Interrupting dispatch worker(s)", unprocessedCount);
                // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!workerExecutor.awaitTermination(timeout, unit)) {
                    logger.error("unable to gracefully shutdown event handler");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            workerExecutor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.error("unable to close event dispatcher http client", e);
            }
        }

        logger.info("event handler shutdown complete");
    }

    @Override
    public void close() {
        shutdownAndAwaitTermination(closeTimeout, closeTimeoutUnit);
    }

    //======== Helper classes ========//

    /**
     * Wrapper runnable for the actual event dispatch.
     */
    private class EventDispatcher implements Runnable {

        private final LogEvent logEvent;

        EventDispatcher(LogEvent logEvent) {
            this.logEvent = logEvent;
        }

        @Override
        public void run() {
            try {
                HttpRequestBase request;
                if (logEvent.getRequestMethod() == LogEvent.RequestMethod.GET) {
                    request = generateGetRequest(logEvent);
                } else {
                    request = generatePostRequest(logEvent);
                }
                httpClient.execute(request, EVENT_RESPONSE_HANDLER);
            } catch (IOException e) {
                logger.error("event dispatch failed", e);
            } catch (URISyntaxException e) {
                logger.error("unable to parse generated URI", e);
            }
        }

        /**
         * Helper method that generates the event request for the given {@link LogEvent}.
         */
        private HttpGet generateGetRequest(LogEvent event) throws URISyntaxException {

            URIBuilder builder = new URIBuilder(event.getEndpointUrl());
            for (Map.Entry<String, String> param : event.getRequestParams().entrySet()) {
                builder.addParameter(param.getKey(), param.getValue());
            }

            return new HttpGet(builder.build());
        }

        private HttpPost generatePostRequest(LogEvent event) throws UnsupportedEncodingException {
            HttpPost post = new HttpPost(event.getEndpointUrl());
            post.setEntity(new StringEntity(event.getBody()));
            post.addHeader("Content-Type", "application/json");
            return post;
        }
    }

    /**
     * Handler for the event request.
     */
    private static final class ProjectConfigResponseHandler implements ResponseHandler<Void> {

        @Override
        @CheckForNull
        public Void handleResponse(HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                // read the response, so we can close the connection
                response.getEntity();
                return null;
            } else {
                throw new ClientProtocolException("unexpected response from event endpoint, status: " + status);
            }
        }
    }

    //======== Builder ========//

    public static Builder builder() { return new Builder(); }

    public static class Builder {

        int queueCapacity = PropertyUtils.getInteger(CONFIG_QUEUE_CAPACITY, DEFAULT_QUEUE_CAPACITY);
        int numWorkers = PropertyUtils.getInteger(CONFIG_NUM_WORKERS, DEFAULT_NUM_WORKERS);
        int maxTotalConnections = PropertyUtils.getInteger(CONFIG_MAX_CONNECTIONS, DEFAULT_MAX_CONNECTIONS);
        int maxPerRoute = PropertyUtils.getInteger(CONFIG_MAX_PER_ROUTE, DEFAULT_MAX_PER_ROUTE);
        int validateAfterInactivity = PropertyUtils.getInteger(CONFIG_VALIDATE_AFTER_INACTIVITY, DEFAULT_VALIDATE_AFTER_INACTIVITY);
        private long closeTimeout = Long.MAX_VALUE;
        private TimeUnit closeTimeoutUnit = TimeUnit.MILLISECONDS;

        public Builder withQueueCapacity(int queueCapacity) {
            if (queueCapacity <= 0) {
                logger.warn("Queue capacity cannot be <= 0. Keeping default value: {}", this.queueCapacity);
                return this;
            }

            this.queueCapacity = queueCapacity;
            return this;
        }

        public Builder withNumWorkers(int numWorkers) {
            if (numWorkers <= 0) {
                logger.warn("Number of workers cannot be <= 0. Keeping default value: {}", this.numWorkers);
                return this;
            }

            this.numWorkers = numWorkers;
            return this;
        }

        public Builder withMaxTotalConnections(int maxTotalConnections) {
            this.maxTotalConnections = maxTotalConnections;
            return this;
        }

        public Builder withMaxPerRoute(int maxPerRoute) {
            this.maxPerRoute = maxPerRoute;
            return this;
        }

        public Builder withValidateAfterInactivity(int validateAfterInactivity) {
            this.validateAfterInactivity = validateAfterInactivity;
            return this;
        }

        public Builder withCloseTimeout(long closeTimeout, TimeUnit unit) {
            this.closeTimeout = closeTimeout;
            this.closeTimeoutUnit = unit;
            return this;
        }

        public AsyncEventHandler build() {
            return new AsyncEventHandler(
                queueCapacity,
                numWorkers,
                maxTotalConnections,
                maxPerRoute,
                validateAfterInactivity,
                closeTimeout,
                closeTimeoutUnit
            );
        }
    }

    private int validateInput(String name, int input, int fallback) {
        if (input <= 0) {
            logger.warn("Invalid value for {}: {}. Defaulting to {}", name, input, fallback);
            return fallback;
        }

        return input;
    }
}
