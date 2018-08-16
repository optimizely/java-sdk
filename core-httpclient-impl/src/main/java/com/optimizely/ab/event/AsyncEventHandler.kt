/**
 *
 * Copyright 2016-2017, Optimizely and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.event

import com.optimizely.ab.HttpClientUtils
import com.optimizely.ab.NamedThreadFactory

import com.optimizely.ab.event.internal.serializer.DefaultJsonSerializer
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.Closeable
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URISyntaxException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * [EventHandler] implementation that queues events and has a separate pool of threads responsible
 * for the dispatch.
 */
class AsyncEventHandler @JvmOverloads constructor(queueCapacity: Int, numWorkers: Int, maxConnections: Int = 200, connectionsPerRoute: Int = 20, validateAfter: Int = 5000) : EventHandler, Closeable {

    // The following static values are public so that they can be tweaked if necessary.
    // These are the recommended settings for http protocol.  https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
    // The maximum number of connections allowed across all routes.
    private var maxTotalConnections = 200
    // The maximum number of connections allowed for a route
    private var maxPerRoute = 20
    // Defines period of inactivity in milliseconds after which persistent connections must be re-validated prior to being leased to the consumer.
    private var validateAfterInactivity = 5000

    private val httpClient: CloseableHttpClient
    private val workerExecutor: ExecutorService
    private val logEventQueue: BlockingQueue<LogEvent>

    init {
        if (queueCapacity <= 0) {
            throw IllegalArgumentException("queue capacity must be > 0")
        }

        this.maxTotalConnections = maxConnections
        this.maxPerRoute = connectionsPerRoute
        this.validateAfterInactivity = validateAfter

        this.logEventQueue = ArrayBlockingQueue(queueCapacity)
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(HttpClientUtils.DEFAULT_REQUEST_CONFIG)
                .setConnectionManager(poolingHttpClientConnectionManager())
                .disableCookieManagement()
                .build()

        this.workerExecutor = Executors.newFixedThreadPool(
                numWorkers, NamedThreadFactory("optimizely-event-dispatcher-thread-%s", true))

        // create dispatch workers
        for (i in 0 until numWorkers) {
            val worker = EventDispatchWorker()
            workerExecutor.submit(worker)
        }
    }

    private fun poolingHttpClientConnectionManager(): PoolingHttpClientConnectionManager {
        val poolingHttpClientConnectionManager = PoolingHttpClientConnectionManager()
        poolingHttpClientConnectionManager.maxTotal = maxTotalConnections
        poolingHttpClientConnectionManager.defaultMaxPerRoute = maxPerRoute
        poolingHttpClientConnectionManager.validateAfterInactivity = validateAfterInactivity
        return poolingHttpClientConnectionManager
    }

    override fun dispatchEvent(logEvent: LogEvent) {
        // attempt to enqueue the log event for processing
        val submitted = logEventQueue.offer(logEvent)
        if (!submitted) {
            logger.error("unable to enqueue event because queue is full")
        }
    }

    @Throws(IOException::class)
    override fun close() {
        logger.info("closing event dispatcher")

        // "close" all workers and the http client
        try {
            httpClient.close()
        } catch (e: IOException) {
            logger.error("unable to close the event handler httpclient cleanly", e)
        } finally {
            workerExecutor.shutdownNow()
        }
    }

    //======== Helper classes ========//

    private inner class EventDispatchWorker : Runnable {

        override fun run() {
            var terminate = false

            logger.info("starting event dispatch worker")
            // event loop that'll block waiting for events to appear in the queue

            while (!terminate) {
                try {
                    val event = logEventQueue.take()
                    val request: HttpRequestBase
                    if (event.requestMethod == LogEvent.RequestMethod.GET) {
                        request = generateGetRequest(event)
                    } else {
                        request = generatePostRequest(event)
                    }
                    httpClient.execute(request, EVENT_RESPONSE_HANDLER)
                } catch (e: InterruptedException) {
                    logger.info("terminating event dispatcher event loop")
                    terminate = true
                } catch (t: Throwable) {
                    logger.error("event dispatcher threw exception but will continue", t)
                }

            }
        }

        /**
         * Helper method that generates the event request for the given [LogEvent].
         */
        @Throws(URISyntaxException::class)
        private fun generateGetRequest(event: LogEvent): HttpGet {

            val builder = URIBuilder(event.endpointUrl)
            for ((key, value) in event.requestParams) {
                builder.addParameter(key, value)
            }

            return HttpGet(builder.build())
        }

        @Throws(UnsupportedEncodingException::class)
        private fun generatePostRequest(event: LogEvent): HttpPost {
            val post = HttpPost(event.endpointUrl)
            post.entity = StringEntity(event.body)
            post.addHeader("Content-Type", "application/json")
            return post
        }
    }

    /**
     * Handler for the event request that returns nothing (i.e., Void)
     */
    private class ProjectConfigResponseHandler : ResponseHandler<Void> {

        @Throws(IOException::class)
        override fun handleResponse(response: HttpResponse): Void? {
            val status = response.statusLine.statusCode
            if (status >= 200 && status < 300) {
                // read the response, so we can close the connection
                response.entity
                return null
            } else {
                throw ClientProtocolException("unexpected response from event endpoint, status: $status")
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AsyncEventHandler::class.java!!)
        private val EVENT_RESPONSE_HANDLER = ProjectConfigResponseHandler()
    }
}