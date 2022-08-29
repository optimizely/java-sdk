/**
 *    Copyright 2022, Optimizely Inc. and contributors
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
package com.optimizely.ab.odp;

import ch.qos.logback.classic.Level;
import com.optimizely.ab.OptimizelyHttpClient;
import com.optimizely.ab.internal.LogbackVerifier;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DefaultODPApiManagerTest {
    private static final String validResponse = "{\"data\":{\"customer\":{\"audiences\":{\"edges\":[{\"node\":{\"name\":\"has_email\",\"state\":\"qualified\"}},{\"node\":{\"name\":\"has_email_opted_in\",\"state\":\"qualified\"}}]}}}}";

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    OptimizelyHttpClient mockHttpClient;

    @Before
    public void setUp() throws Exception {
        setupHttpClient(200);
    }

    private void setupHttpClient(int statusCode) throws Exception {
        mockHttpClient = mock(OptimizelyHttpClient.class);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity(validResponse));

        when(mockHttpClient.execute(any(HttpPost.class)))
            .thenReturn(httpResponse);
    }

    @Test
    public void generateCorrectSegmentsStringWhenListHasOneItem() {
        DefaultODPApiManager apiManager = new DefaultODPApiManager();
        String expected = "\\\"only_segment\\\"";
        String actual = apiManager.getSegmentsStringForRequest(Arrays.asList("only_segment"));
        assertEquals(expected, actual);
    }

    @Test
    public void generateCorrectSegmentsStringWhenListHasMultipleItems() {
        DefaultODPApiManager apiManager = new DefaultODPApiManager();
        String expected = "\\\"segment_1\\\", \\\"segment_2\\\", \\\"segment_3\\\"";
        String actual = apiManager.getSegmentsStringForRequest(Arrays.asList("segment_1", "segment_2", "segment_3"));
        assertEquals(expected, actual);
    }

    @Test
    public void generateEmptyStringWhenGivenListIsEmpty() {
        DefaultODPApiManager apiManager = new DefaultODPApiManager();
        String actual = apiManager.getSegmentsStringForRequest(new ArrayList<>());
        assertEquals("", actual);
    }

    @Test
    public void generateCorrectRequestBody() throws Exception {
        ODPApiManager apiManager = new DefaultODPApiManager(mockHttpClient);
        apiManager.fetchQualifiedSegments("key", "endPoint", "fs_user_id", "test_user", Arrays.asList("segment_1", "segment_2"));
        verify(mockHttpClient, times(1)).execute(any(HttpPost.class));

        String expectedResponse = "{\"query\": \"query {customer(fs_user_id: \\\"test_user\\\") {audiences(subset: [\\\"segment_1\\\", \\\"segment_2\\\"]) {edges {node {name state}}}}}\"}";
        ArgumentCaptor<HttpPost> request = ArgumentCaptor.forClass(HttpPost.class);
        verify(mockHttpClient).execute(request.capture());
        assertEquals(expectedResponse, EntityUtils.toString(request.getValue().getEntity()));
    }

    @Test
    public void returnResponseStringWhenStatusIs200() throws Exception {
        ODPApiManager apiManager = new DefaultODPApiManager(mockHttpClient);
        String responseString = apiManager.fetchQualifiedSegments("key", "endPoint", "fs_user_id", "test_user", Arrays.asList("segment_1", "segment_2"));
        verify(mockHttpClient, times(1)).execute(any(HttpPost.class));
        assertEquals(validResponse, responseString);
    }

    @Test
    public void returnNullWhenStatusIsNot200AndLogError() throws Exception {
        setupHttpClient(500);
        ODPApiManager apiManager = new DefaultODPApiManager(mockHttpClient);
        String responseString = apiManager.fetchQualifiedSegments("key", "endPoint", "fs_user_id", "test_user", Arrays.asList("segment_1", "segment_2"));
        verify(mockHttpClient, times(1)).execute(any(HttpPost.class));
        logbackVerifier.expectMessage(Level.ERROR, "Unexpected response from ODP server, Response code: 500, null");
        assertNull(responseString);
    }

    @Test
    public void eventDispatchSuccess() {
        ODPApiManager apiManager = new DefaultODPApiManager(mockHttpClient);
        apiManager.sendEvents("testKey", "testEndpoint", "[]");
        logbackVerifier.expectMessage(Level.DEBUG, "ODP Event Dispatched successfully");
    }

    @Test
    public void eventDispatchFailStatus() throws Exception {
        setupHttpClient(400);
        ODPApiManager apiManager = new DefaultODPApiManager(mockHttpClient);
        apiManager.sendEvents("testKey", "testEndpoint", "[]]");
        logbackVerifier.expectMessage(Level.ERROR, "ODP event send failed (Response code: 400, null)");
    }
}
