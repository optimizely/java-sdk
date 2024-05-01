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

import io.javalin.Javalin;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.junit.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.optimizely.ab.OptimizelyHttpClient.builder;
import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

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
        assertEquals(builder.validateAfterInactivity, 5000);
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
    public void testRetries() throws IOException {
        // Javalin intercepts before proxy, so host and port should be set correct here
        String host = "http://localhost";
        int port = 8000;
        Javalin app = Javalin.create().start(port);
        int maxFailures = 2;

        AtomicInteger callTimes = new AtomicInteger();
        app.get("/", ctx -> {
            callTimes.addAndGet(1);
            int count = callTimes.get();
            if (count < maxFailures) {
                throw new NoHttpResponseException("TESTING CONNECTION FAILURE");
            } else {
                ctx.status(200).result("Success");
            }
        });

        OptimizelyHttpClient optimizelyHttpClient = spy(OptimizelyHttpClient.builder().build());
        optimizelyHttpClient.execute(new HttpGet(host + ":" + String.valueOf(port)));
        assertEquals(3, callTimes.get());
    }

    @Test
    public void testRetriesWithCustom() throws IOException {
        // Javalin intercepts before proxy, so host and port should be set correct here
        String host = "http://localhost";
        int port = 8000;
        Javalin app = Javalin.create().start(port);
        int maxFailures = 2;

        AtomicInteger callTimes = new AtomicInteger();
        app.get("/", ctx -> {
            callTimes.addAndGet(1);
            int count = callTimes.get();
            if (count < maxFailures) {
//                throw new NoHttpResponseException("TESTING CONNECTION FAILURE");
                throw new IOException("TESTING CONNECTION FAILURE");

//                ctx.status(500).result("TESTING Server Error");
            } else {
                ctx.status(200).result("Success");
            }
        });

        HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(3, true);
        OptimizelyHttpClient optimizelyHttpClient = spy(OptimizelyHttpClient.builder().withRetryHandler(retryHandler).build());

        optimizelyHttpClient.execute(new HttpGet(host + ":" + String.valueOf(port)));
        assertEquals(3, callTimes.get());
    }

//
//    @Test
//    public void testRetriesWithCustom() throws IOException {
//        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
//        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
//
//        HttpRequestRetryHandler mockRetryHandler = spy(new DefaultHttpRequestRetryHandler(3, true));
//        when(mockRetryHandler.retryRequest(any(), any(), any())).thenReturn(true);
//
//        OptimizelyHttpClient optimizelyHttpClient = OptimizelyHttpClient.builder().withRetryHandler(mockRetryHandler).build();
//        try {
//            optimizelyHttpClient.execute(new HttpGet("https://example.com"));
//        } catch(Exception e) {
//            assert(e instanceof IOException);
//        }
//        verify(mockRetryHandler, times(3)).retryRequest(any(), any(), any());
// }

}
