/**
 * Copyright 2025, Optimizely
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.cmab;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.optimizely.ab.OptimizelyHttpClient;
import com.optimizely.ab.cmab.client.CmabClient;
import com.optimizely.ab.cmab.client.CmabClientConfig;
import com.optimizely.ab.cmab.client.CmabFetchException;
import com.optimizely.ab.cmab.client.CmabInvalidResponseException;
import com.optimizely.ab.cmab.client.RetryConfig;
import com.optimizely.ab.cmab.client.CmabClientHelper;

public class DefaultCmabClient implements CmabClient {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultCmabClient.class);
    private static final int DEFAULT_TIMEOUT_MS = 10000;

    private final OptimizelyHttpClient httpClient;
    private final RetryConfig retryConfig;

    // Primary constructor - all others delegate to this
    public DefaultCmabClient(OptimizelyHttpClient httpClient, CmabClientConfig config) {
        this.retryConfig = config != null ? config.getRetryConfig() : null;
        this.httpClient = httpClient != null ? httpClient : createDefaultHttpClient();
    }

    // Constructor with HTTP client only (no retry)
    public DefaultCmabClient(OptimizelyHttpClient httpClient) {
        this(httpClient, CmabClientConfig.withNoRetry());
    }

    // Constructor with just retry config (uses default HTTP client)
    public DefaultCmabClient(CmabClientConfig config) {
        this(null, config);
    }

    // Default constructor (default HTTP client, default retry config)
    public DefaultCmabClient() {
        this(null, CmabClientConfig.withDefaultRetry());
    }

    // Extract HTTP client creation logic
    private OptimizelyHttpClient createDefaultHttpClient() {
        int timeoutMs = (retryConfig != null) ? retryConfig.getMaxTimeoutMs() : DEFAULT_TIMEOUT_MS;
        return OptimizelyHttpClient.builder().setTimeoutMillis(timeoutMs).build();
    }

    @Override
    public String fetchDecision(String ruleId, String userId, Map<String, Object> attributes, String cmabUuid) {
        // Implementation will use this.httpClient and this.retryConfig
        String url = String.format(CmabClientHelper.CMAB_PREDICTION_ENDPOINT, ruleId);
        String requestBody = CmabClientHelper.buildRequestJson(userId, ruleId, attributes, cmabUuid);

        // Use retry logic if configured, otherwise single request
        if (retryConfig != null && retryConfig.getMaxRetries() > 0) {
            return doFetchWithRetry(url, requestBody, retryConfig.getMaxRetries());
        } else {
            return doFetch(url, requestBody);
        }
    }

    private String doFetch(String url, String requestBody) {
        HttpPost request = new HttpPost(url);
        try {
            request.setEntity(new StringEntity(requestBody));
        } catch (UnsupportedEncodingException e) {
            String errorMessage = String.format(CmabClientHelper.CMAB_FETCH_FAILED, e.getMessage());
            logger.error(errorMessage);
            throw new CmabFetchException(errorMessage);
        }
        request.setHeader("content-type", "application/json");
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);
            
            if (!CmabClientHelper.isSuccessStatusCode(response.getStatusLine().getStatusCode())) {
                StatusLine statusLine = response.getStatusLine();
                String errorMessage = String.format(CmabClientHelper.CMAB_FETCH_FAILED, statusLine.getReasonPhrase());
                logger.error(errorMessage);
                throw new CmabFetchException(errorMessage);
            }

            String responseBody;
            try {
                responseBody = EntityUtils.toString(response.getEntity());

                if (!CmabClientHelper.validateResponse(responseBody)) {
                    logger.error(CmabClientHelper.INVALID_CMAB_FETCH_RESPONSE);
                    throw new CmabInvalidResponseException(CmabClientHelper.INVALID_CMAB_FETCH_RESPONSE);
                }
                return CmabClientHelper.parseVariationId(responseBody);
            } catch (IOException | ParseException e) {
                logger.error(CmabClientHelper.CMAB_FETCH_FAILED);
                throw new CmabInvalidResponseException(CmabClientHelper.INVALID_CMAB_FETCH_RESPONSE);
            }
            
        } catch (IOException e) {
            String errorMessage = String.format(CmabClientHelper.CMAB_FETCH_FAILED, e.getMessage());
            logger.error(errorMessage);
            throw new CmabFetchException(errorMessage);
        } finally {
            closeHttpResponse(response);
        }
    }

    private String doFetchWithRetry(String url, String requestBody, int maxRetries) {
        double backoff = retryConfig.getBackoffBaseMs();
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return doFetch(url, requestBody);
            } catch (CmabFetchException | CmabInvalidResponseException e) {
                lastException = e;
                
                // If this is the last attempt, don't wait - just break and throw
                if (attempt >= maxRetries) {
                    break;
                }
                
                // Log retry attempt
                logger.info("Retrying CMAB request (attempt: {}) after {} ms...", 
                        attempt + 1, (int) backoff);
                
                try {
                    Thread.sleep((long) backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    String errorMessage = String.format(CmabClientHelper.CMAB_FETCH_FAILED, "Request interrupted during retry");
                    logger.error(errorMessage);
                    throw new CmabFetchException(errorMessage, ie);
                }
                
                // Calculate next backoff using exponential backoff with multiplier
                backoff = Math.min(
                    backoff * Math.pow(retryConfig.getBackoffMultiplier(), attempt + 1),
                    retryConfig.getMaxTimeoutMs()
                );
            }
        }
        
        // If we get here, all retries were exhausted
        String errorMessage = String.format(CmabClientHelper.CMAB_FETCH_FAILED, "Exhausted all retries for CMAB request");
        logger.error(errorMessage);
        throw new CmabFetchException(errorMessage, lastException);
    }

    private static void closeHttpResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                logger.warn(e.getLocalizedMessage());
            }
        }
    }
}
