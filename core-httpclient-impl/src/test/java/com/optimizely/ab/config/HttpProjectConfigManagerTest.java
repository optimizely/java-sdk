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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.optimizely.ab.OptimizelyHttpClient;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static com.optimizely.ab.config.HttpProjectConfigManager.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HttpProjectConfigManagerTest {

    class MyResponse extends BasicHttpResponse implements CloseableHttpResponse {

        public MyResponse(ProtocolVersion protocolVersion, Integer status, String body) {
            super(protocolVersion, status, body);
        }

        @Override
        public void close() throws IOException {

        }
    }
    @Mock
    private OptimizelyHttpClient mockHttpClient;

    private String datafileString;
    private HttpProjectConfigManager projectConfigManager;

    @Before
    public void setUp() throws Exception {
        datafileString = Resources.toString(Resources.getResource("valid-project-config-v4.json"), Charsets.UTF_8);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity(datafileString));

        when(mockHttpClient.execute(any(HttpGet.class)))
            .thenReturn(httpResponse);

        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .build();
    }

    @After
    public void tearDown() {
        if (projectConfigManager == null) {
            return;
        }

        projectConfigManager.close();

        System.clearProperty("optimizely." + HttpProjectConfigManager.CONFIG_BLOCKING_UNIT);
        System.clearProperty("optimizely." + HttpProjectConfigManager.CONFIG_BLOCKING_DURATION);
        System.clearProperty("optimizely." + HttpProjectConfigManager.CONFIG_POLLING_UNIT);
        System.clearProperty("optimizely." + HttpProjectConfigManager.CONFIG_POLLING_DURATION);
    }

    @Test
    public void testHttpGetBySdkKey() throws Exception {
        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .build();

        URI actual = projectConfigManager.getUri();
        assertEquals(new URI("https://cdn.optimizely.com/datafiles/sdk-key.json"), actual);
    }

    @Test
    public void testHttpGetByCustomFormat() throws Exception {
        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .withFormat("https://custom.optimizely.com/%s.json")
            .build();

        URI actual = projectConfigManager.getUri();
        assertEquals(new URI("https://custom.optimizely.com/sdk-key.json"), actual);
    }

    @Test
    public void testHttpGetByCustomUrl() throws Exception {
        String expected = "https://custom.optimizely.com/custom-location.json";

        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withUrl(expected)
            .build();

        URI actual = projectConfigManager.getUri();
        assertEquals(new URI(expected), actual);
    }

    @Test
    public void testHttpGetBySdkKeyForAuthDatafile() throws Exception {
        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .withDatafileAccessToken("auth-token")
            .build();

        URI actual = projectConfigManager.getUri();
        assertEquals(new URI("https://config.optimizely.com/datafiles/auth/sdk-key.json"), actual);
    }

    @Test
    public void testHttpGetByCustomUrlForAuthDatafile() throws Exception {
        String expected = "https://custom.optimizely.com/custom-location.json";

        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withUrl(expected)
            .withSdkKey("sdk-key")
            .withDatafileAccessToken("auth-token")
            .build();

        URI actual = projectConfigManager.getUri();
        assertEquals(new URI(expected), actual);
    }

    @Test
    public void testCreateHttpRequest() throws Exception {
        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .build();

        HttpGet request = projectConfigManager.createHttpRequest();
        assertEquals(request.getURI().toString(), "https://cdn.optimizely.com/datafiles/sdk-key.json");
        assertEquals(request.getHeaders("Authorization").length, 0);
    }

    @Test
    public void testCreateHttpRequestForAuthDatafile() throws Exception {
        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .withDatafileAccessToken("auth-token")
            .build();

        HttpGet request = projectConfigManager.createHttpRequest();
        assertEquals(request.getURI().toString(), "https://config.optimizely.com/datafiles/auth/sdk-key.json");
        assertEquals(request.getHeaders("Authorization")[0].getValue(), "Bearer auth-token");
    }

    @Test
    public void testPoll() throws Exception {
        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .build();

        assertEquals("1480511547", projectConfigManager.getConfig().getRevision());
    }

    @Test
    public void testBuildDefer() throws Exception {
        // always returns null so PollingProjectConfigManager will never resolve.
        mockHttpClient = mock(OptimizelyHttpClient.class);

        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .build(true);
    }

    @Test
    public void testInvalidPollingInterval() {
        Builder builder = builder();
        long expectedPeriod = builder.period;
        TimeUnit expectedTimeUnit = builder.timeUnit;

        builder.withPollingInterval(null, SECONDS);
        assertEquals(expectedPeriod, builder.period);
        assertEquals(expectedTimeUnit, builder.timeUnit);

        builder.withPollingInterval(-1L, SECONDS);
        assertEquals(expectedPeriod, builder.period);
        assertEquals(expectedTimeUnit, builder.timeUnit);

        builder.withPollingInterval(10L, null);
        assertEquals(expectedPeriod, builder.period);
        assertEquals(expectedTimeUnit, builder.timeUnit);

        builder.withPollingInterval(10L, SECONDS);
        assertEquals(10, builder.period);
        assertEquals(SECONDS, builder.timeUnit);
    }

    @Test
    public void testInvalidBlockingTimeout() {
        Builder builder = builder();
        long expectedPeriod = builder.blockingTimeoutPeriod;
        TimeUnit expectedTimeUnit = builder.blockingTimeoutUnit;

        builder.withBlockingTimeout(null, SECONDS);
        assertEquals(expectedPeriod, builder.blockingTimeoutPeriod);
        assertEquals(expectedTimeUnit, builder.blockingTimeoutUnit);

        builder.withBlockingTimeout(-1L, SECONDS);
        assertEquals(expectedPeriod, builder.blockingTimeoutPeriod);
        assertEquals(expectedTimeUnit, builder.blockingTimeoutUnit);

        builder.withBlockingTimeout(10L, null);
        assertEquals(expectedPeriod, builder.blockingTimeoutPeriod);
        assertEquals(expectedTimeUnit, builder.blockingTimeoutUnit);

        builder.withBlockingTimeout(10L, SECONDS);
        assertEquals(10, builder.blockingTimeoutPeriod);
        assertEquals(SECONDS, builder.blockingTimeoutUnit);
    }

    @Test
    @Ignore
    public void testGetDatafileHttpResponse2XX() throws Exception {
        String modifiedStamp = "Wed, 24 Apr 2019 07:07:07 GMT";
        CloseableHttpResponse getResponse = new MyResponse(new ProtocolVersion("TEST", 0, 0), 200, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));
        getResponse.setHeader(HttpHeaders.LAST_MODIFIED, modifiedStamp);

        String datafile = projectConfigManager.getDatafileFromResponse(getResponse);
        assertNotNull(datafile);

        assertEquals("4", parseProjectConfig(datafile).getVersion());
        // Confirm last modified time is set
        assertEquals(modifiedStamp, projectConfigManager.getLastModified());
    }

    @Test(expected = ClientProtocolException.class)
    public void testGetDatafileHttpResponse3XX() throws Exception {
        CloseableHttpResponse getResponse = new MyResponse(new ProtocolVersion("TEST", 0, 0), 300, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        projectConfigManager.getDatafileFromResponse(getResponse);
    }

    @Test
    public void testGetDatafileHttpResponse304() throws Exception {
        CloseableHttpResponse getResponse = new MyResponse(new ProtocolVersion("TEST", 0, 0), 304, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        String datafile = projectConfigManager.getDatafileFromResponse(getResponse);
        assertNull(datafile);
    }

    @Test(expected = ClientProtocolException.class)
    public void testGetDatafileHttpResponse4XX() throws Exception {
        CloseableHttpResponse getResponse = new MyResponse(new ProtocolVersion("TEST", 0, 0), 400, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        projectConfigManager.getDatafileFromResponse(getResponse);
    }

    @Test(expected = ClientProtocolException.class)
    public void testGetDatafileHttpResponse5XX() throws Exception {
        CloseableHttpResponse getResponse = new MyResponse(new ProtocolVersion("TEST", 0, 0), 500, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        projectConfigManager.getDatafileFromResponse(getResponse);
    }

    @Test
    public void testInvalidPayload() throws Exception {
        reset(mockHttpClient);
        CloseableHttpResponse invalidPayloadResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        when(statusLine.getStatusCode()).thenReturn(200);
        when(invalidPayloadResponse.getStatusLine()).thenReturn(statusLine);
        when(invalidPayloadResponse.getEntity()).thenReturn(new StringEntity("I am an invalid response!"));

        when(mockHttpClient.execute(any(HttpGet.class)))
            .thenReturn(invalidPayloadResponse);

        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .withBlockingTimeout(10L, TimeUnit.MILLISECONDS)
            .build();

        assertNull(projectConfigManager.getConfig());
    }

    @Test
    public void testInvalidPollingIntervalFromSystemProperties() throws Exception {
        System.setProperty("optimizely." + HttpProjectConfigManager.CONFIG_POLLING_DURATION, "-1");
        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .build();
    }

    @Test
    public void testInvalidBlockingIntervalFromSystemProperties() throws Exception {
        reset(mockHttpClient);
        CloseableHttpResponse invalidPayloadResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        when(statusLine.getStatusCode()).thenReturn(200);
        when(invalidPayloadResponse.getStatusLine()).thenReturn(statusLine);
        when(invalidPayloadResponse.getEntity()).thenReturn(new StringEntity("I am an invalid response!"));

        when(mockHttpClient.execute(any(HttpGet.class)))
            .thenReturn(invalidPayloadResponse);

        System.setProperty("optimizely." + HttpProjectConfigManager.CONFIG_BLOCKING_DURATION, "-1");
        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .build();
    }

    @Test
    @Ignore
    public void testBasicFetch() throws Exception {
        projectConfigManager = builder()
            .withSdkKey("7vPf3v7zye3fY4PcbejeCz")
            .build();

        ProjectConfig actual = projectConfigManager.getConfig();
        assertNotNull(actual);
        assertNotNull(actual.getVersion());
    }

    @Test
    @Ignore
    public void testBasicFetchTwice() throws Exception {
        projectConfigManager = builder()
            .withSdkKey("7vPf3v7zye3fY4PcbejeCz")
            .build();

        ProjectConfig actual = projectConfigManager.getConfig();
        assertNotNull(actual);
        assertNotNull(actual.getVersion());

        // Assert ProjectConfig when refetched as datafile has not changed
        ProjectConfig latestConfig = projectConfigManager.getConfig();
        assertEquals(actual, latestConfig);
    }
}
