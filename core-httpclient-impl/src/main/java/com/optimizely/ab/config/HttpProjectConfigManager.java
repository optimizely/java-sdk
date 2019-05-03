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
import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.config.parser.ConfigParseException;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * HttpProjectConfigManager is an implementation of a {@link PollingProjectConfigManager}
 * backed by a datafile. Currently this is loosely tied to Apache HttpClient
 * implementation which is the client of choice in this package.
 */
public class HttpProjectConfigManager extends PollingProjectConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(HttpProjectConfigManager.class);

    private final OptimizelyHttpClient httpClient;
    private final URI uri;
    private String datafileLastModified;

    private HttpProjectConfigManager(long period, TimeUnit timeUnit, OptimizelyHttpClient httpClient, String url, long blockingTimeoutPeriod, TimeUnit blockingTimeoutUnit) {
        super(period, timeUnit, blockingTimeoutPeriod, blockingTimeoutUnit);
        this.httpClient = httpClient;
        this.uri = URI.create(url);
    }

    public URI getUri() {
        return uri;
    }

    @VisibleForTesting
    public String getLastModified() {
        return datafileLastModified;
    }

    public String getDatafileFromResponse(HttpResponse response) throws NullPointerException, IOException {
        StatusLine statusLine = response.getStatusLine();

        if (statusLine == null) {
            throw new ClientProtocolException("unexpected response from event endpoint, status is null");
        }

        int status = statusLine.getStatusCode();

        // Datafile has not updated
        if (status == HttpStatus.SC_NOT_MODIFIED) {
            logger.debug("Not updating ProjectConfig as datafile has not updated since " + datafileLastModified);
            return null;
        }

        if (status >= 200 && status < 300) {
            // read the response, so we can close the connection
            HttpEntity entity = response.getEntity();
            Header lastModifiedHeader = response.getFirstHeader(HttpHeaders.LAST_MODIFIED);
            if (lastModifiedHeader != null) {
                datafileLastModified = lastModifiedHeader.getValue();
            }
            return EntityUtils.toString(entity, "UTF-8");
        } else {
            throw new ClientProtocolException("unexpected response from event endpoint, status: " + status);
        }
    }

    static ProjectConfig parseProjectConfig(String datafile) throws ConfigParseException {
        return new DatafileProjectConfig.Builder().withDatafile(datafile).build();
    }

    @Override
    protected ProjectConfig poll() {
        HttpGet httpGet = new HttpGet(uri);

        if (datafileLastModified != null) {
            httpGet.setHeader(HttpHeaders.IF_MODIFIED_SINCE, datafileLastModified);
        }

        logger.info("Fetching datafile from: {}", httpGet.getURI());
        try {
            HttpResponse response = httpClient.execute(httpGet);
            String datafile = getDatafileFromResponse(response);
            if (datafile == null) {
                return null;
            }
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

        private long blockingTimeoutPeriod = 10;
        private TimeUnit blockingTimeoutUnit = TimeUnit.SECONDS;

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

        /**
         * Configure time to block before Completing the future. This timeout is used on the first call
         * to {@link PollingProjectConfigManager#getConfig()}. If the timeout is exceeded then the
         * PollingProjectConfigManager will begin returning null immediately until the call to Poll
         * succeeds.
         */
        public Builder withBlockingTimeout(long period, TimeUnit timeUnit) {
            if (timeUnit == null) {
                throw new NullPointerException("Must provide valid timeUnit");
            }

            this.blockingTimeoutPeriod = period;
            this.blockingTimeoutUnit = timeUnit;

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

            if (url == null) {
                if (sdkKey == null) {
                    throw new NullPointerException("sdkKey cannot be null");
                }

                url = String.format(format, sdkKey);
            }

            HttpProjectConfigManager httpProjectManager = new HttpProjectConfigManager(period, timeUnit, httpClient, url, blockingTimeoutPeriod, blockingTimeoutUnit);

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
}