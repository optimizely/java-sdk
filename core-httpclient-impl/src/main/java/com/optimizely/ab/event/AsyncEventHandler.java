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

import com.optimizely.ab.HttpClientUtils;
import com.optimizely.ab.NamedThreadFactory;
import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.common.lifecycle.LifecycleAware;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * {@link EventHandler} implementation that queues events and has a separate pool of threads responsible
 * for the dispatch.
 */
public class AsyncEventHandler implements EventHandler, LifecycleAware {

    // The following static values are public so that they can be tweaked if necessary.
    // These are the recommended settings for http protocol.  https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
    // The maximum number of connections allowed across all routes.
    private int maxTotalConnections = 200;
    // The maximum number of connections allowed for a route
    private int maxPerRoute = 20;
    // Defines period of inactivity in milliseconds after which persistent connections must be re-validated prior to being leased to the consumer.
    private int validateAfterInactivity = 5000;

    private static final Logger logger = LoggerFactory.getLogger(AsyncEventHandler.class);

    private final CloseableHttpClient httpClient;
    private final ExecutorService workerExecutor;

    public AsyncEventHandler(int queueCapacity, int numWorkers) {
        this(queueCapacity, numWorkers, 200, 20, 5000);
    }

    public AsyncEventHandler(int queueCapacity, int numWorkers, int maxConnections, int connectionsPerRoute, int validateAfter) {
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queue capacity must be > 0");
        }

        this.maxTotalConnections = maxConnections;
        this.maxPerRoute = connectionsPerRoute;
        this.validateAfterInactivity = validateAfter;

        this.httpClient = HttpClients.custom()
            .setDefaultRequestConfig(HttpClientUtils.DEFAULT_REQUEST_CONFIG)
            .setConnectionManager(poolingHttpClientConnectionManager())
            .disableCookieManagement()
            .build();

        this.workerExecutor = new ThreadPoolExecutor(numWorkers, numWorkers,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(queueCapacity),
            new NamedThreadFactory("optimizely-event-dispatcher-thread-%s", true));
    }

    @VisibleForTesting
    public AsyncEventHandler(CloseableHttpClient httpClient, ExecutorService workerExecutor) {
        this.httpClient = httpClient;
        this.workerExecutor = workerExecutor;
    }

    private PoolingHttpClientConnectionManager poolingHttpClientConnectionManager() {
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(maxTotalConnections);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxPerRoute);
        poolingHttpClientConnectionManager.setValidateAfterInactivity(validateAfterInactivity);
        return poolingHttpClientConnectionManager;
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

    @Override
    public void onStart() {
        // no-op
    }

    @Override
    public boolean onStop(long timeout, TimeUnit unit) {
        workerExecutor.shutdown();
        try {
            workerExecutor.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            if (!workerExecutor.isTerminated()) {
                logger.warn("Interrupting worker tasks");
                List<Runnable> r = workerExecutor.shutdownNow();
                logger.warn("{} workers did not execute", r.size());
                return false;
            }
        }
        return true;
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

    //======== Helper classes ========//

    /**
     * Wrapper runnable for the actual event dispatch.
     */
    private class EventDispatcher implements Runnable, ResponseHandler<Boolean>, Supplier<Boolean> {
        private final LogEvent logEvent;

        public EventDispatcher(LogEvent logEvent) {
            this.logEvent = logEvent;
        }

        @Override
        public void run() {
            get();
        }

        @Override
        public Boolean get() {
            try {
                HttpRequestBase request;
                if (logEvent.getRequestMethod() == LogEvent.RequestMethod.GET) {
                    request = generateGetRequest(logEvent);
                } else {
                    request = generatePostRequest(logEvent);
                }
                return httpClient.execute(request, this);
            } catch (IOException e) {
                logger.error("event dispatch failed", e);
            } catch (URISyntaxException e) {
                logger.error("unable to parse generated URI", e);
            }
            return false;
        }

        @Override
        public Boolean handleResponse(HttpResponse response) throws IOException {
            StatusLine statusLine = response.getStatusLine();
            int status = statusLine.getStatusCode();

            // release resources
            EntityUtils.consumeQuietly(response.getEntity());

            if (200 > status || status > 299) {
                // HttpResponseException would be a better exception type to throw here,
                // but throwing ClientProtocolException here to maintain original behavior.
                ClientProtocolException ex = new ClientProtocolException("unexpected response from event endpoint, status: " + status);
                logEvent.markFailure(ex);
                throw ex;
            }

            logEvent.markSuccess();

            return true;
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
     * Handler for the event request that returns nothing (i.e., Void)
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
}
