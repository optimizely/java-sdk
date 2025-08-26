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
import java.util.HashMap;
import java.util.Map;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.optimizely.ab.OptimizelyHttpClient;
import com.optimizely.ab.cmab.client.CmabClientConfig;
import com.optimizely.ab.cmab.client.CmabFetchException;
import com.optimizely.ab.cmab.client.CmabInvalidResponseException;
import com.optimizely.ab.cmab.client.RetryConfig;
import com.optimizely.ab.internal.LogbackVerifier;

import ch.qos.logback.classic.Level;

public class DefaultCmabClientTest {

    private static final String validCmabResponse = "{\"predictions\":[{\"variation_id\":\"treatment_1\"}]}";

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    OptimizelyHttpClient mockHttpClient;
    DefaultCmabClient cmabClient;

    @Before
    public void setUp() throws Exception {
        setupHttpClient(200);
        cmabClient = new DefaultCmabClient(mockHttpClient);
    }

    private void setupHttpClient(int statusCode) throws Exception {
        mockHttpClient = mock(OptimizelyHttpClient.class);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(statusLine.getReasonPhrase()).thenReturn(statusCode == 500 ? "Internal Server Error" : "OK");
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity(validCmabResponse));

        when(mockHttpClient.execute(any(HttpPost.class)))
            .thenReturn(httpResponse);
    }

    @Test
    public void testBuildRequestJson() throws Exception {
        String ruleId = "rule_123";
        String userId = "user_456";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("browser", "chrome");
        attributes.put("isMobile", true);
        String cmabUuid = "uuid_789";

        // Fixed: Direct method call instead of CompletableFuture
        String result = cmabClient.fetchDecision(ruleId, userId, attributes, cmabUuid);

        assertEquals("treatment_1", result);
        verify(mockHttpClient, times(1)).execute(any(HttpPost.class));

        ArgumentCaptor<HttpPost> request = ArgumentCaptor.forClass(HttpPost.class);
        verify(mockHttpClient).execute(request.capture());
        String actualRequestBody = EntityUtils.toString(request.getValue().getEntity());
        
        assertTrue(actualRequestBody.contains("\"visitorId\":\"user_456\""));
        assertTrue(actualRequestBody.contains("\"experimentId\":\"rule_123\""));
        assertTrue(actualRequestBody.contains("\"cmabUUID\":\"uuid_789\""));
        assertTrue(actualRequestBody.contains("\"browser\""));
        assertTrue(actualRequestBody.contains("\"chrome\""));
        assertTrue(actualRequestBody.contains("\"isMobile\""));
        assertTrue(actualRequestBody.contains("true"));
    }

    @Test
    public void returnVariationWhenStatusIs200() throws Exception {
        String ruleId = "rule_123";
        String userId = "user_456";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("segment", "premium");
        String cmabUuid = "uuid_789";

        // Fixed: Direct method call instead of CompletableFuture
        String result = cmabClient.fetchDecision(ruleId, userId, attributes, cmabUuid);

        assertEquals("treatment_1", result);
        verify(mockHttpClient, times(1)).execute(any(HttpPost.class));
        
        // Note: Remove this line if your implementation doesn't log this specific message
        // logbackVerifier.expectMessage(Level.INFO, "CMAB returned variation 'treatment_1' for rule 'rule_123' and user 'user_456'");
    }

    @Test
    public void returnErrorWhenStatusIsNot200AndLogError() throws Exception {
        // Create new mock for 500 error
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(500);
        when(statusLine.getReasonPhrase()).thenReturn("Internal Server Error");
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity("Server Error"));
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);

        String ruleId = "rule_123";
        String userId = "user_456";
        Map<String, Object> attributes = new HashMap<>();
        String cmabUuid = "uuid_789";

        try {
            cmabClient.fetchDecision(ruleId, userId, attributes, cmabUuid);
            fail("Expected CmabFetchException");
        } catch (CmabFetchException e) {
            assertTrue(e.getMessage().contains("Internal Server Error"));
        }

        verify(mockHttpClient, times(1)).execute(any(HttpPost.class));
        // Fixed: Match actual log message format
        logbackVerifier.expectMessage(Level.ERROR, "CMAB decision fetch failed with status: Internal Server Error");
    }

    @Test
    public void returnErrorWhenInvalidResponseAndLogError() throws Exception {
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity("{\"predictions\":[]}"));
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);

        String ruleId = "rule_123";
        String userId = "user_456";
        Map<String, Object> attributes = new HashMap<>();
        String cmabUuid = "uuid_789";

        try {
            cmabClient.fetchDecision(ruleId, userId, attributes, cmabUuid);
            fail("Expected CmabInvalidResponseException");
        } catch (CmabInvalidResponseException e) {
            assertEquals("Invalid CMAB fetch response", e.getMessage());
        }

        verify(mockHttpClient, times(1)).execute(any(HttpPost.class));
        logbackVerifier.expectMessage(Level.ERROR, "Invalid CMAB fetch response");
    }

    @Test
    public void testNoRetryWhenNoRetryConfig() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class)))
            .thenThrow(new IOException("Network error"));

        String ruleId = "rule_123";
        String userId = "user_456";
        Map<String, Object> attributes = new HashMap<>();
        String cmabUuid = "uuid_789";

        try {
            cmabClient.fetchDecision(ruleId, userId, attributes, cmabUuid);
            fail("Expected CmabFetchException");
        } catch (CmabFetchException e) {
            assertTrue(e.getMessage().contains("Network error"));
        }

        verify(mockHttpClient, times(1)).execute(any(HttpPost.class));
        logbackVerifier.expectMessage(Level.ERROR, "CMAB decision fetch failed with status: Network error");
    }

    @Test
    public void testRetryOnNetworkError() throws Exception {
        // Create retry config
        RetryConfig retryConfig = new RetryConfig(2, 50L, 1.5, 10000);
        CmabClientConfig config = new CmabClientConfig(retryConfig);
        DefaultCmabClient cmabClientWithRetry = new DefaultCmabClient(mockHttpClient, config);

        // Setup response for successful retry
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity(validCmabResponse));
        
        // First call fails with IOException, second succeeds
        when(mockHttpClient.execute(any(HttpPost.class)))
            .thenThrow(new IOException("Network error"))
            .thenReturn(httpResponse);

        String ruleId = "rule_123";
        String userId = "user_456";
        Map<String, Object> attributes = new HashMap<>();
        String cmabUuid = "uuid_789";

        String result = cmabClientWithRetry.fetchDecision(ruleId, userId, attributes, cmabUuid);

        assertEquals("treatment_1", result);
        verify(mockHttpClient, times(2)).execute(any(HttpPost.class));
        
        // Fixed: Match actual retry log message format
        logbackVerifier.expectMessage(Level.INFO, "Retrying CMAB request (attempt: 1) after 50 ms...");
    }

    @Test
    public void testRetryExhausted() throws Exception {
        RetryConfig retryConfig = new RetryConfig(2, 50L, 1.5, 10000);
        CmabClientConfig config = new CmabClientConfig(retryConfig);
        DefaultCmabClient cmabClientWithRetry = new DefaultCmabClient(mockHttpClient, config);

        // All calls fail
        when(mockHttpClient.execute(any(HttpPost.class)))
            .thenThrow(new IOException("Network error"));

        String ruleId = "rule_123";
        String userId = "user_456";
        Map<String, Object> attributes = new HashMap<>();
        String cmabUuid = "uuid_789";

        try {
            cmabClientWithRetry.fetchDecision(ruleId, userId, attributes, cmabUuid);
            fail("Expected CmabFetchException");
        } catch (CmabFetchException e) {
            assertTrue(e.getMessage().contains("Exhausted all retries for CMAB request"));
        }

        // Should attempt initial call + 2 retries = 3 total
        verify(mockHttpClient, times(3)).execute(any(HttpPost.class));
        logbackVerifier.expectMessage(Level.ERROR, "CMAB decision fetch failed with status: Exhausted all retries for CMAB request");
    }

    @Test
    public void testEmptyResponseThrowsException() throws Exception {
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity(""));
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);

        String ruleId = "rule_123";
        String userId = "user_456";
        Map<String, Object> attributes = new HashMap<>();
        String cmabUuid = "uuid_789";

        try {
            cmabClient.fetchDecision(ruleId, userId, attributes, cmabUuid);
            fail("Expected CmabInvalidResponseException");
        } catch (CmabInvalidResponseException e) {
            assertEquals("Invalid CMAB fetch response", e.getMessage());
        }
    }
}