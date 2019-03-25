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
import java.util.concurrent.atomic.AtomicReference;

/**
 * HttpProjectConfigManager is an implementation of a ProjectConfigManager
 * backed by a datafile. Currently this is loosely tied to Apache HttpClient
 * implementation which is the client of choice in this package.
 */
public class HttpProjectConfigManager implements ProjectConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(HttpProjectConfigManager.class);

    private final OptimizelyHttpClient httpClient;
    private final HttpGet httpGet;
    private final ResponseHandler<ProjectConfig> responseHandler;
    private final AtomicReference<ProjectConfig> projectConfig = new AtomicReference<>();

    private HttpProjectConfigManager(OptimizelyHttpClient httpClient, String url) {
        this.httpClient = httpClient;
        this.httpGet = new HttpGet(url);
        this.responseHandler = new ProjectConfigResponseHandler();
    }

    @VisibleForTesting
    HttpGet getHttpGet() {
        return httpGet;
    }

    @VisibleForTesting
    ResponseHandler<ProjectConfig> getResponseHandler() {
        return responseHandler;
    }

    @Override
    public ProjectConfig getConfig() {
        try {
            ProjectConfig newProjectConfig = httpClient.execute(httpGet, responseHandler);
            if (newProjectConfig != null) {
                projectConfig.set(newProjectConfig);
            }
        } catch (IOException e) {
            logger.error("Error fetching datafile", e);
        }

        return projectConfig.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sdkKey;
        private String url;
        private String format = "https://cdn.optimizely.com/datafiles/%s.json";
        private OptimizelyHttpClient httpClient;

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

        public HttpProjectConfigManager build() {
            if (httpClient == null) {
                httpClient = HttpClientUtils.getDefaultHttpClient();
            }

            if (url != null) {
                return new HttpProjectConfigManager(httpClient, url);
            }

            if (sdkKey == null) {
                throw new NullPointerException("sdkKey cannot be null");
            }

            url = String.format(format, sdkKey);

            return new HttpProjectConfigManager(httpClient, url);

        }
    }

    /**
     * Handler for the event request that returns nothing (i.e., Void)
     */
    private static final class ProjectConfigResponseHandler implements ResponseHandler<ProjectConfig> {

        @Override
        @CheckForNull
        public ProjectConfig handleResponse(HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                // read the response, so we can close the connection
                HttpEntity entity = response.getEntity();
                String datafile = EntityUtils.toString(entity, "UTF-8");

                try {
                    return new DatafileProjectConfig.Builder().withDatafile(datafile).build();
                } catch (ConfigParseException e) {
                    logger.warn("Unable to parse datafile", e);
                }

                return null;
            } else {
                // TODO handle unmodifed response.
                throw new ClientProtocolException("unexpected response from event endpoint, status: " + status);
            }
        }
    }
}
