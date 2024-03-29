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

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.optimizely.ab.OptimizelyHttpClient.builder;
import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        OptimizelyHttpClient optimizelyHttpClient = builder().build();
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
}
