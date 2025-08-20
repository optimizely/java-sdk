package com.optimizely.ab.cmab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.optimizely.ab.OptimizelyHttpClient;

public class DefaultCmabClient implements CmabClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCmabClient.class);
    // Update constants to match JS error messages format
    private static final String CMAB_FETCH_FAILED = "CMAB decision fetch failed with status: %d";
    private static final String INVALID_CMAB_FETCH_RESPONSE = "Invalid CMAB fetch response";
    private static final Pattern VARIATION_ID_PATTERN = Pattern.compile("\"variation_id\"\\s*:\\s*\"?([^\"\\s,}]+)\"?");
    private static final String CMAB_PREDICTION_ENDPOINT = "https://prediction.cmab.optimizely.com/predict/%s";

    private final OptimizelyHttpClient httpClient;
    private final RetryConfig retryConfig;
    private final ScheduledExecutorService executorService;

    // Main constructor: HTTP client + retry config
    public DefaultCmabClient(OptimizelyHttpClient httpClient, CmabClientConfig config) {
        this.httpClient = httpClient != null ? httpClient : OptimizelyHttpClient.builder().build();
        this.retryConfig = config != null ? config.getRetryConfig() : null;
        this.executorService = Executors.newScheduledThreadPool(2);
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

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public CompletableFuture<String> fetchDecision(String ruleId, String userId, Map<String, Object> attributes, String cmabUuid) {
        // Implementation will use this.httpClient and this.retryConfig
        String url = String.format(CMAB_PREDICTION_ENDPOINT, ruleId);
        String requestBody = buildRequestJson(userId, ruleId, attributes, cmabUuid);

        // Use retry logic if configured, otherwise single request
        if (retryConfig != null && retryConfig.getMaxRetries() > 0) {
            return doFetchWithRetry(url, requestBody, userId, ruleId, retryConfig.getMaxRetries());
        } else {
            return doFetch(url, requestBody, userId, ruleId);
        }
    }

    private CompletableFuture<String> doFetch(String url, String requestBody, String userId, String ruleId) {
        return CompletableFuture.supplyAsync(() -> {
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");

            try {
                request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
            } catch (Exception e) {
                logger.error(String.format(CMAB_FETCH_FAILED, e));
                throw new CompletionException(new RuntimeException(String.format(CMAB_FETCH_FAILED, e)));
            }

            CloseableHttpResponse response = null;
            try {
                logger.debug("Making CMAB prediction request to: {} for user: {}", url, userId);
                response = httpClient.execute(request);

                int statusCode = response.getStatusLine().getStatusCode();

                if (!isSuccessStatusCode(statusCode)) {
                    logger.error("CMAB fetch failed (Response code: {}) for user: {}", statusCode, userId);
                    // Status code error (like JS: new OptimizelyError(CMAB_FETCH_FAILED, response.statusCode))
                    throw new CompletionException(new RuntimeException(String.format(CMAB_FETCH_FAILED, statusCode)));
                }

                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                logger.debug("CMAB response received for user: {}", userId);

                
                if (!validateResponse(responseBody)) {
                    logger.error(INVALID_CMAB_FETCH_RESPONSE);
                    throw new CompletionException(new RuntimeException(INVALID_CMAB_FETCH_RESPONSE));
                }

                String variationId = parseVariationId(responseBody);
                logger.info("CMAB returned variation '{}' for rule '{}' and user '{}'", variationId, ruleId, userId);

                return variationId;

            } catch (IOException e) {
                logger.error(String.format(CMAB_FETCH_FAILED, e));
                throw new CompletionException(new RuntimeException(String.format(CMAB_FETCH_FAILED, e)));
            } finally {
                closeHttpResponse(response);
            }
        });
    }

    private CompletableFuture<String> doFetchWithRetry(String url, String requestBody, String userId, String ruleId, int retriesLeft) {
        return doFetch(url, requestBody, userId, ruleId)
            .handle((result, throwable) -> {
                if (retriesLeft > 0 && shouldRetry(throwable)) {
                    logger.warn("CMAB fetch failed, retrying... ({} retries left) for user: {}", retriesLeft, userId);

                    // Calculate delay using RetryConfig
                    int attemptNumber = retryConfig.getMaxRetries() - retriesLeft;
                    long delay = retryConfig.calculateDelay(attemptNumber);

                    CompletableFuture<String> future = new CompletableFuture<>();

                    // Schedule retry after delay
                    executorService.schedule(() -> {
                        doFetchWithRetry(url, requestBody, userId, ruleId, retriesLeft - 1)
                            .whenComplete((retryResult, retryEx) -> {
                                if (retryEx != null) {
                                    future.completeExceptionally(retryEx);
                                } else {
                                    future.complete(retryResult);
                                }
                            });
                    }, delay, TimeUnit.MILLISECONDS);

                    return future;
                } else if (throwable != null) {
                    logger.error("CMAB fetch failed after all retries for user: {}", userId, throwable);
                    CompletableFuture<String> failedFuture = new CompletableFuture<>();
                    failedFuture.completeExceptionally(throwable);
                    return failedFuture;
                } else {
                    // Success case
                    CompletableFuture<String> successFuture = new CompletableFuture<>();
                    successFuture.complete(result);
                    return successFuture;
                }
            })
            .thenCompose(future -> future); // Flatten the nested CompletableFuture
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

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof CompletionException) {
            Throwable cause = throwable.getCause();
            if (cause instanceof IOException) {
                return true; // Network errors - always retry
            }
            if (cause instanceof RuntimeException) {
                String message = cause.getMessage();
                // Retry on 5xx server errors (look for "status: 5" in formatted message)
                if (message != null && message.matches(".*status: 5\\d\\d")) {
                    return true;
                }
            }
        }
        return false;
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
        throw new RuntimeException("Could not parse variation_id from CMAB response");
    }

    private static void closeHttpResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }
}
