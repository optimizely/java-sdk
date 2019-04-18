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
package com.optimizely.ab.config;

import com.optimizely.ab.HttpClientUtils;
import com.optimizely.ab.OptimizelyHttpClient;
import com.optimizely.ab.OptimizelyRuntimeException;
import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.config.parser.ConfigParseException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * HttpProjectConfigManager is an implementation of a ProjectConfigManager
 * backed by a datafile. Currently this is loosely tied to Apache HttpClient
 * implementation which is the client of choice in this package.
 *
 * Note that this implementation is blocking and stateless. This is best used in
 * conjunction with the {@link PollingProjectConfigManager} to provide caching
 * and asynchronous fetching.
 */
public class HttpProjectConfigManager extends PollingProjectConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(HttpProjectConfigManager.class);

    private final OptimizelyHttpClient httpClient;
    private final URI uri;
    private final ResponseHandler<String> responseHandler = new ProjectConfigResponseHandler();

    private HttpProjectConfigManager(long period, TimeUnit timeUnit, OptimizelyHttpClient httpClient, String url) {
        super(period, timeUnit);
        this.httpClient = httpClient;
        this.uri = URI.create(url);
    }

    public URI getUri() {
        return uri;
    }

    static ProjectConfig parseProjectConfig(String datafile) throws ConfigParseException {
        return new DatafileProjectConfig.Builder().withDatafile(datafile).build();
    }

    @Override
    protected ProjectConfig poll() {
        HttpGet httpGet = new HttpGet(uri);
        logger.info("Fetching datafile from: {}", httpGet.getURI());
        try {
            String datafile = httpClient.execute(httpGet, responseHandler);
            return parseProjectConfig(datafile);
        } catch (ConfigParseException | IOException e) {
            logger.error("Error fetching datafile", e);
        }

        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String datafile;
        private String sdkKey;
        private String url;
        private String format = "https://cdn.optimizely.com/datafiles/%s.json";
        private OptimizelyHttpClient httpClient;
        private long period = 5;
        private TimeUnit timeUnit = TimeUnit.MINUTES;

        public Builder withDatafile(String datafile) {
            this.datafile = datafile;
            return this;
        }

        public Builder withSdkKey(String sdkKey) {
            this.sdkKey = sdkKey;
            return this;
        }

        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder withFormat(String format) {
            this.format = format;
            return this;
        }

        public Builder withOptimizelyHttpClient(OptimizelyHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder withPollingInterval(long period, TimeUnit timeUnit) {
            if (timeUnit == null) {
                throw new NullPointerException("Must provide valid timeUnit");
            }

            this.period = period;
            this.timeUnit = timeUnit;

            return this;
        }

        /**
         * HttpProjectConfigManager.Builder that builds and starts a HttpProjectConfigManager.
         * This is the default builder which will block until a config is available.
         */
        public HttpProjectConfigManager build() {
            return build(false);
        }

        /**
         * HttpProjectConfigManager.Builder that builds and starts a HttpProjectConfigManager.
         *
         * @param defer When true, we will not wait for the configuration to be available
         *              before returning the HttpProjectConfigManager instance.
         */
        public HttpProjectConfigManager build(boolean defer) {
            if (httpClient == null) {
                httpClient = HttpClientUtils.getDefaultHttpClient();
            }

            if (url != null) {
                return new HttpProjectConfigManager(period, timeUnit, httpClient, url);
            }

            if (sdkKey == null) {
                throw new NullPointerException("sdkKey cannot be null");
            }

            url = String.format(format, sdkKey);

            HttpProjectConfigManager httpProjectManager = new HttpProjectConfigManager(period, timeUnit, httpClient, url);

            if (datafile != null) {
                try {
                    ProjectConfig projectConfig = HttpProjectConfigManager.parseProjectConfig(datafile);
                    httpProjectManager.setConfig(projectConfig);
                } catch (ConfigParseException e) {
                    logger.warn("Error parsing fallback datafile.", e);
                }
            }

            httpProjectManager.start();

            // Optionally block until config is available.
            if (!defer) {
                httpProjectManager.getConfig();
            }

            return httpProjectManager;
        }
    }

    /**
     * Handler for the event request that returns nothing (i.e., Void)
     */
    static final class ProjectConfigResponseHandler implements ResponseHandler<String> {

        @Override
        @CheckForNull
        public String handleResponse(HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                // read the response, so we can close the connection
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, "UTF-8");
            } else {
                // TODO handle unmodifed response.
                throw new ClientProtocolException("unexpected response from event endpoint, status: " + status);
            }
        }
    }
}
