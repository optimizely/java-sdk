/**
 *
 *    Copyright 2019-2020, Optimizely
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

import org.apache.http.HttpException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.junit.*;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ConnectionOptions;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.optimizely.ab.OptimizelyHttpClient.builder;
import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockserver.model.HttpForward.forward;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.*;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

public class OptimizelyHttpClientTest {
    @Before
    public void setUp() {
        System.setProperty("https.proxyHost", "localhost");
        // default port (80) returns 404 instead of HttpHostConnectException
        System.setProperty("https.proxyPort", "12345");
    }

    @After
    public void tearDown() {
        System.clearProperty("https.proxyHost");
    }

    @Test
    public void testDefaultConfiguration() {
        OptimizelyHttpClient.Builder builder = builder();
        assertEquals(builder.validateAfterInactivity, 1000);
        assertEquals(builder.maxTotalConnections, 200);
        assertEquals(builder.maxPerRoute, 20);
        assertNull(builder.customRetryHandler);

        OptimizelyHttpClient optimizelyHttpClient = builder.build();
        assertTrue(optimizelyHttpClient.getHttpClient() instanceof CloseableHttpClient);
    }

    @Test
    public void testNonDefaultConfiguration() {
        OptimizelyHttpClient optimizelyHttpClient = builder()
            .withValidateAfterInactivity(1)
            .withMaxPerRoute(2)
            .withMaxTotalConnections(3)
            .withEvictIdleConnections(5, MINUTES)
            .build();

        assertTrue(optimizelyHttpClient.getHttpClient() instanceof CloseableHttpClient);
    }

    @Test
    public void testEvictTime() {
        OptimizelyHttpClient.Builder builder = builder();
        long expectedPeriod = builder.evictConnectionIdleTimePeriod;
        TimeUnit expectedTimeUnit = builder.evictConnectionIdleTimeUnit;

        assertEquals(expectedPeriod, 0L);
        assertEquals(expectedTimeUnit, MILLISECONDS);

        builder.withEvictIdleConnections(10L, SECONDS);
        assertEquals(10, builder.evictConnectionIdleTimePeriod);
        assertEquals(SECONDS, builder.evictConnectionIdleTimeUnit);
    }

    @Test(expected = HttpHostConnectException.class)
    public void testProxySettings() throws IOException {
        OptimizelyHttpClient optimizelyHttpClient = builder().build();

        // If this request succeeds then the proxy config was not picked up.
        HttpGet get = new HttpGet("https://www.optimizely.com");
        optimizelyHttpClient.execute(get);
    }

    @Test
    public void testExecute() throws IOException {
        HttpUriRequest httpUriRequest = RequestBuilder.get().build();
        ResponseHandler<Boolean> responseHandler = response -> false;

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        when(mockHttpClient.execute(httpUriRequest, responseHandler)).thenReturn(true);

        OptimizelyHttpClient optimizelyHttpClient = new OptimizelyHttpClient(mockHttpClient);
        assertTrue(optimizelyHttpClient.execute(httpUriRequest, responseHandler));
    }

    @Test
    public void testRetriesWithCustomRetryHandler() throws IOException {

        // [NOTE] Request retries are all handled inside HttpClient. Not easy for unit test.
        //  - "DefaultHttpRetryHandler" in HttpClient retries only with special types of Exceptions
        //    like "NoHttpResponseException", etc.
        //    Other exceptions (SocketTimeout, ProtocolException, etc.) all ignored.
        //  - Not easy to force the specific exception type in the low-level.
        //  - This test just validates custom retry handler injected ok by validating the number of retries.

        class CustomRetryHandler implements HttpRequestRetryHandler {
            private final int maxRetries;

            public CustomRetryHandler(int maxRetries) {
                this.maxRetries = maxRetries;
            }

            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                // override to retry for any type of exceptions
                return executionCount < maxRetries;
            }
        }

        int port = 9999;
        ClientAndServer mockServer;
        int retryCount;

        // default httpclient (retries enabled by default, but no retry for timeout connection)

        mockServer = ClientAndServer.startClientAndServer(port);
        mockServer
            .when(request().withMethod("GET").withPath("/"))
            .error(HttpError.error());

        OptimizelyHttpClient clientDefault = OptimizelyHttpClient.builder()
            .setTimeoutMillis(100)
            .build();

        try {
            clientDefault.execute(new HttpGet("http://localhost:" + port));
            fail();
        } catch (Exception e) {
            retryCount = mockServer.retrieveRecordedRequests(request()).length;
            assertEquals(1, retryCount);
        }
        mockServer.stop();

        // httpclient with custom retry handler (5 times retries for any request)

        mockServer = ClientAndServer.startClientAndServer(port);
        mockServer
            .when(request().withMethod("GET").withPath("/"))
            .error(HttpError.error());

        OptimizelyHttpClient clientWithRetries = OptimizelyHttpClient.builder()
            .withRetryHandler(new CustomRetryHandler(5))
            .setTimeoutMillis(100)
            .build();

        try {
            clientWithRetries.execute(new HttpGet("http://localhost:" + port));
            fail();
        } catch (Exception e) {
            retryCount = mockServer.retrieveRecordedRequests(request()).length;
            assertEquals(5, retryCount);
        }
        mockServer.stop();
    }
}
