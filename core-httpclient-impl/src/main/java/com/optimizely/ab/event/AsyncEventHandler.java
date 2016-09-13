/**
 *
 *    Copyright 2016, Optimizely
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

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
public class AsyncEventHandler implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(AsyncEventHandler.class);
    private static final ProjectConfigResponseHandler EVENT_RESPONSE_HANDLER = new ProjectConfigResponseHandler();

    private final CloseableHttpClient httpClient;
    private final ExecutorService workerExecutor;

    public AsyncEventHandler(int queueCapacity, int numWorkers) {
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queue capacity must be > 0");
        }

        this.httpClient = HttpClients.custom()
            .setDefaultRequestConfig(HttpClientUtils.DEFAULT_REQUEST_CONFIG)
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
     *
     * Note: termination of ongoing event dispatching is best-effort.
     *
     * @param timeout maximum time to wait for event dispatches to complete
     * @param unit the time unit of the timeout argument
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
    private class EventDispatcher implements Runnable {

        private final LogEvent logEvent;

        EventDispatcher(LogEvent logEvent) {
            this.logEvent = logEvent;
        }

        @Override
        public void run() {
            try {
                HttpGet request = generateRequest(logEvent);
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
        private HttpGet generateRequest(LogEvent event) throws URISyntaxException {

            URIBuilder builder = new URIBuilder(event.getEndpointUrl());
            for (Map.Entry<String, String> param : event.getRequestParams().entrySet()) {
                builder.addParameter(param.getKey(), param.getValue());
            }

            return new HttpGet(builder.build());
        }
    }

    /**
     * Handler for the event request that returns nothing (i.e., Void)
     */
    private static final class ProjectConfigResponseHandler implements ResponseHandler<Void> {

        @Override
        public @CheckForNull Void handleResponse(HttpResponse response) throws IOException {
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