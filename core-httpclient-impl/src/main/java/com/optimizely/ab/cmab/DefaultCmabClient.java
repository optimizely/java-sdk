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

public class DefaultCmabClient implements CmabClient {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultCmabClient.class);
    private static final int DEFAULT_TIMEOUT_MS = 10000;
    // Update constants to match JS error messages format
    private static final String CMAB_FETCH_FAILED = "CMAB decision fetch failed with status: %s";
    private static final String INVALID_CMAB_FETCH_RESPONSE = "Invalid CMAB fetch response";
    private static final Pattern VARIATION_ID_PATTERN = Pattern.compile("\"variation_id\"\\s*:\\s*\"?([^\"\\s,}]+)\"?");
    private static final String CMAB_PREDICTION_ENDPOINT = "https://prediction.cmab.optimizely.com/predict/%s";

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

    // Default constructor (no retry, default HTTP client)
    public DefaultCmabClient() {
        this(null, CmabClientConfig.withNoRetry());
    }

    // Extract HTTP client creation logic
    private OptimizelyHttpClient createDefaultHttpClient() {
        int timeoutMs = (retryConfig != null) ? retryConfig.getMaxTimeoutMs() : DEFAULT_TIMEOUT_MS;
        return OptimizelyHttpClient.builder().setTimeoutMillis(timeoutMs).build();
    }

    @Override
    public String fetchDecision(String ruleId, String userId, Map<String, Object> attributes, String cmabUuid) {
        // Implementation will use this.httpClient and this.retryConfig
        String url = String.format(CMAB_PREDICTION_ENDPOINT, ruleId);
        String requestBody = buildRequestJson(userId, ruleId, attributes, cmabUuid);

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
            String errorMessage = String.format(CMAB_FETCH_FAILED, e.getMessage());
            logger.error(errorMessage);
            throw new CmabFetchException(errorMessage);
        }
        request.setHeader("content-type", "application/json");
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);
            
            if (!isSuccessStatusCode(response.getStatusLine().getStatusCode())) {
                StatusLine statusLine = response.getStatusLine();
                String errorMessage = String.format(CMAB_FETCH_FAILED, statusLine.getReasonPhrase());
                logger.error(errorMessage);
                throw new CmabFetchException(errorMessage);
            }

            String responseBody;
            try {
                responseBody = EntityUtils.toString(response.getEntity());

                if (!validateResponse(responseBody)) {
                    logger.error(INVALID_CMAB_FETCH_RESPONSE);
                    throw new CmabInvalidResponseException(INVALID_CMAB_FETCH_RESPONSE);
                }
                return parseVariationId(responseBody);
            } catch (IOException | ParseException e) {
                logger.error(CMAB_FETCH_FAILED);
                throw new CmabInvalidResponseException(INVALID_CMAB_FETCH_RESPONSE);
            }
            
        } catch (IOException e) {
            String errorMessage = String.format(CMAB_FETCH_FAILED, e.getMessage());
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
                    String errorMessage = String.format(CMAB_FETCH_FAILED, "Request interrupted during retry");
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
        String errorMessage = String.format(CMAB_FETCH_FAILED, "Exhausted all retries for CMAB request");
        logger.error(errorMessage);
        throw new CmabFetchException(errorMessage, lastException);
    }
    
    private String buildRequestJson(String userId, String ruleId, Map<String, Object> attributes, String cmabUuid) {
        StringBuilder json = new StringBuilder();
        json.append("{\"instances\":[{");
        json.append("\"visitorId\":\"").append(escapeJson(userId)).append("\",");
        json.append("\"experimentId\":\"").append(escapeJson(ruleId)).append("\",");
        json.append("\"cmabUUID\":\"").append(escapeJson(cmabUuid)).append("\",");
        json.append("\"attributes\":[");

        boolean first = true;
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("{\"id\":\"").append(escapeJson(entry.getKey())).append("\",");
            json.append("\"value\":").append(formatJsonValue(entry.getValue())).append(",");
            json.append("\"type\":\"custom_attribute\"}");
            first = false;
        }

        json.append("]}]}");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String formatJsonValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    // Helper methods
    private boolean isSuccessStatusCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private boolean validateResponse(String responseBody) {
        try {
            return responseBody.contains("predictions") && 
                responseBody.contains("variation_id") &&
                parseVariationIdForValidation(responseBody) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shouldRetry(Exception exception) {
        return (exception instanceof CmabFetchException) || 
            (exception instanceof CmabInvalidResponseException);
    }

    private String parseVariationIdForValidation(String jsonResponse) {
        Matcher matcher = VARIATION_ID_PATTERN.matcher(jsonResponse);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String parseVariationId(String jsonResponse) {
        // Simple regex to extract variation_id from predictions[0].variation_id
        Pattern pattern = Pattern.compile("\"predictions\"\\s*:\\s*\\[\\s*\\{[^}]*\"variation_id\"\\s*:\\s*\"?([^\"\\s,}]+)\"?");
        Matcher matcher = pattern.matcher(jsonResponse);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new CmabInvalidResponseException(INVALID_CMAB_FETCH_RESPONSE);
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
