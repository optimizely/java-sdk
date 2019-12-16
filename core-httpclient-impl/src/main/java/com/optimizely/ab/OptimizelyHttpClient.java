/**
 *
 *    Copyright 2019, Optimizely
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
package com.optimizely.ab;

import com.optimizely.ab.annotations.VisibleForTesting;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.Closeable;
import java.io.IOException;

/**
 * Basic HttpClient wrapper to be utilized for fetching the datafile
 * and for posting events through the EventHandler
 *
 * TODO abstract out interface and move into core?
 */
public class OptimizelyHttpClient implements Closeable {

    private final CloseableHttpClient httpClient;

    OptimizelyHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @VisibleForTesting
    HttpClient getHttpClient() {
        return httpClient;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void close() throws IOException {
        this.httpClient.close();
    }

    public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler) throws IOException {
        return httpClient.execute(request, responseHandler);
    }

    public CloseableHttpResponse execute(final HttpUriRequest request) throws IOException {
        return httpClient.execute(request);
    }

    public static class Builder {
        // The following static values are public so that they can be tweaked if necessary.
        // These are the recommended settings for http protocol.  https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
        // The maximum number of connections allowed across all routes.
        private int maxTotalConnections = 200;
        // The maximum number of connections allowed for a route
        private int maxPerRoute = 20;
        // Defines period of inactivity in milliseconds after which persistent connections must be re-validated prior to being leased to the consumer.
        private int validateAfterInactivity = 5000;

        private Builder() {

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

        public OptimizelyHttpClient build() {
            PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
            poolingHttpClientConnectionManager.setMaxTotal(maxTotalConnections);
            poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxPerRoute);
            poolingHttpClientConnectionManager.setValidateAfterInactivity(validateAfterInactivity);

            CloseableHttpClient closableHttpClient = HttpClients.custom()
                .setDefaultRequestConfig(HttpClientUtils.DEFAULT_REQUEST_CONFIG)
                .setConnectionManager(poolingHttpClientConnectionManager)
                .disableCookieManagement()
                .useSystemProperties()
                .build();

            return new OptimizelyHttpClient(closableHttpClient);
        }
    }

}
