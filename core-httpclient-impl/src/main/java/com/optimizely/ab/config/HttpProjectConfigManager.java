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
import com.optimizely.ab.internal.PropertyUtils;
import com.optimizely.ab.notification.NotificationCenter;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
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

    public static final String CONFIG_POLLING_DURATION  = "http.project.config.manager.polling.duration";
    public static final String CONFIG_POLLING_UNIT      = "http.project.config.manager.polling.unit";
    public static final String CONFIG_BLOCKING_DURATION = "http.project.config.manager.blocking.duration";
    public static final String CONFIG_BLOCKING_UNIT     = "http.project.config.manager.blocking.unit";
    public static final String CONFIG_SDK_KEY           = "http.project.config.manager.sdk.key";
    public static final String CONFIG_DATAFILE_AUTH_TOKEN = "http.project.config.manager.datafile.auth.token";

    public static final long DEFAULT_POLLING_DURATION  = 5;
    public static final TimeUnit DEFAULT_POLLING_UNIT  = TimeUnit.MINUTES;
    public static final long DEFAULT_BLOCKING_DURATION = 10;
    public static final TimeUnit DEFAULT_BLOCKING_UNIT = TimeUnit.SECONDS;

    private static final Logger logger = LoggerFactory.getLogger(HttpProjectConfigManager.class);

    private final OptimizelyHttpClient httpClient;
    private final URI uri;
    private final String datafileAccessToken;
    private String datafileLastModified;

    private HttpProjectConfigManager(long period,
                                     TimeUnit timeUnit,
                                     OptimizelyHttpClient httpClient,
                                     String url,
                                     String datafileAccessToken,
                                     long blockingTimeoutPeriod,
                                     TimeUnit blockingTimeoutUnit,
                                     NotificationCenter notificationCenter) {
        super(period, timeUnit, blockingTimeoutPeriod, blockingTimeoutUnit, notificationCenter);
        this.httpClient = httpClient;
        this.uri = URI.create(url);
        this.datafileAccessToken = datafileAccessToken;
    }

    public URI getUri() {
        return uri;
    }

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
            throw new ClientProtocolException("unexpected response when trying to fetch datafile, status: " + status);
        }
    }

    static ProjectConfig parseProjectConfig(String datafile) throws ConfigParseException {
        return new DatafileProjectConfig.Builder().withDatafile(datafile).build();
    }

    @Override
    protected ProjectConfig poll() {
        HttpGet httpGet = createHttpRequest();
        CloseableHttpResponse response = null;
        logger.debug("Fetching datafile from: {}", httpGet.getURI());
        try {
            response = httpClient.execute(httpGet);
            String datafile = getDatafileFromResponse(response);
            if (datafile == null) {
                return null;
            }
            return parseProjectConfig(datafile);
        } catch (ConfigParseException | IOException e) {
            logger.error("Error fetching datafile", e);
        }
        finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.warn(e.getLocalizedMessage());
                }
            }
        }

        return null;
    }

    @VisibleForTesting
    HttpGet createHttpRequest() {
        HttpGet httpGet = new HttpGet(uri);

        if (datafileAccessToken != null) {
            httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + datafileAccessToken);
        }

        if (datafileLastModified != null) {
            httpGet.setHeader(HttpHeaders.IF_MODIFIED_SINCE, datafileLastModified);
        }

        return httpGet;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String datafile;
        private String url;
        private String datafileAccessToken = null;
        private String format = "https://cdn.optimizely.com/datafiles/%s.json";
        private String authFormat = "https://config.optimizely.com/datafiles/auth/%s.json";
        private OptimizelyHttpClient httpClient;
        private NotificationCenter notificationCenter;

        String sdkKey = PropertyUtils.get(CONFIG_SDK_KEY);
        long period = PropertyUtils.getLong(CONFIG_POLLING_DURATION, DEFAULT_POLLING_DURATION);
        TimeUnit timeUnit = PropertyUtils.getEnum(CONFIG_POLLING_UNIT, TimeUnit.class, DEFAULT_POLLING_UNIT);

        long blockingTimeoutPeriod = PropertyUtils.getLong(CONFIG_BLOCKING_DURATION, DEFAULT_BLOCKING_DURATION);
        TimeUnit blockingTimeoutUnit = PropertyUtils.getEnum(CONFIG_BLOCKING_UNIT, TimeUnit.class, DEFAULT_BLOCKING_UNIT);

        public Builder withDatafile(String datafile) {
            this.datafile = datafile;
            return this;
        }

        public Builder withSdkKey(String sdkKey) {
            this.sdkKey = sdkKey;
            return this;
        }

        public Builder withDatafileAccessToken(String token) {
            this.datafileAccessToken = token;
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
        public Builder withBlockingTimeout(Long period, TimeUnit timeUnit) {
            if (timeUnit == null) {
                logger.warn("TimeUnit cannot be null. Keeping default period: {} and time unit: {}", this.blockingTimeoutPeriod, this.blockingTimeoutUnit);
                return this;
            }

            if (period == null) {
                logger.warn("Timeout cannot be null. Keeping default period: {} and time unit: {}", this.blockingTimeoutPeriod, this.blockingTimeoutUnit);
                return this;
            }

            if (period <= 0) {
                logger.warn("Timeout cannot be <= 0. Keeping default period: {} and time unit: {}", this.blockingTimeoutPeriod, this.blockingTimeoutUnit);
                return this;
            }

            this.blockingTimeoutPeriod = period;
            this.blockingTimeoutUnit = timeUnit;

            return this;
        }

        public Builder withPollingInterval(Long period, TimeUnit timeUnit) {
            if (timeUnit == null) {
                logger.warn("TimeUnit cannot be null. Keeping default period: {} and time unit: {}", this.period, this.timeUnit);
                return this;
            }

            if (period == null) {
                logger.warn("Interval cannot be null. Keeping default period: {} and time unit: {}", this.period, this.timeUnit);
                return this;
            }

            if (period <= 0) {
                logger.warn("Interval cannot be <= 0. Keeping default period: {} and time unit: {}", this.period, this.timeUnit);
                return this;
            }

            this.period = period;
            this.timeUnit = timeUnit;

            return this;
        }

        public Builder withNotificationCenter(NotificationCenter notificationCenter) {
            this.notificationCenter = notificationCenter;
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
            if (period <= 0) {
                logger.warn("Invalid polling interval {}, {}. Defaulting to {}, {}",
                    period, timeUnit, DEFAULT_POLLING_DURATION, DEFAULT_POLLING_UNIT);
                period = DEFAULT_POLLING_DURATION;
                timeUnit = DEFAULT_POLLING_UNIT;
            }

            if (blockingTimeoutPeriod <= 0) {
                logger.warn("Invalid polling interval {}, {}. Defaulting to {}, {}",
                    blockingTimeoutPeriod, blockingTimeoutUnit, DEFAULT_BLOCKING_DURATION, DEFAULT_BLOCKING_UNIT);
                blockingTimeoutPeriod = DEFAULT_BLOCKING_DURATION;
                blockingTimeoutUnit = DEFAULT_BLOCKING_UNIT;
            }

            if (httpClient == null) {
                httpClient = HttpClientUtils.getDefaultHttpClient();
            }

            if (url == null) {
                if (sdkKey == null) {
                    throw new NullPointerException("sdkKey cannot be null");
                }

                if (datafileAccessToken == null) {
                    url = String.format(format, sdkKey);
                } else {
                    url = String.format(authFormat, sdkKey);
                }
            }

            if (notificationCenter == null) {
                notificationCenter = new NotificationCenter();
            }

            HttpProjectConfigManager httpProjectManager = new HttpProjectConfigManager(
                period,
                timeUnit,
                httpClient,
                url,
                datafileAccessToken,
                blockingTimeoutPeriod,
                blockingTimeoutUnit,
                notificationCenter);

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
