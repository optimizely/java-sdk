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
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
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

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static com.optimizely.ab.config.HttpProjectConfigManager.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpProjectConfigManagerTest {

    @Mock
    private OptimizelyHttpClient mockHttpClient;

    private String datafileString;
    private HttpProjectConfigManager projectConfigManager;

    @Before
    public void setUp() throws Exception {
        datafileString = Resources.toString(Resources.getResource("valid-project-config-v4.json"), Charsets.UTF_8);
        when(mockHttpClient.execute(any(HttpGet.class), any(ResponseHandler.class)))
            .thenReturn(datafileString);
    }

    @After
    public void tearDown() {
        if (projectConfigManager == null) {
            return;
        }

        projectConfigManager.close();
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
    @Ignore
    public void testProjectConfigResponseHandler2XX() throws Exception {
        ResponseHandler<String> handler = new ProjectConfigResponseHandler();

        HttpResponse getResponse = new BasicHttpResponse(new ProtocolVersion("TEST", 0, 0), 200, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        String datafile = handler.handleResponse(getResponse);
        assertNotNull(datafile);

        assertEquals("4", parseProjectConfig(datafile).getVersion());
    }

    @Test(expected = ClientProtocolException.class)
    public void testProjectConfigResponseHandler3XX() throws Exception {
        ResponseHandler<String> handler = new ProjectConfigResponseHandler();

        HttpResponse getResponse = new BasicHttpResponse(new ProtocolVersion("TEST", 0, 0), 300, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        handler.handleResponse(getResponse);
    }

    @Test(expected = ClientProtocolException.class)
    public void testProjectConfigResponseHandler4XX() throws Exception {
        ResponseHandler<String> handler = new ProjectConfigResponseHandler();

        HttpResponse getResponse = new BasicHttpResponse(new ProtocolVersion("TEST", 0, 0), 400, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        handler.handleResponse(getResponse);
    }

    @Test(expected = ClientProtocolException.class)
    public void testProjectConfigResponseHandler5XX() throws Exception {
        ResponseHandler<String> handler = new ProjectConfigResponseHandler();

        HttpResponse getResponse = new BasicHttpResponse(new ProtocolVersion("TEST", 0, 0), 500, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        handler.handleResponse(getResponse);
    }

    @Test
    public void testInvalidPayload() throws Exception {
        reset(mockHttpClient);
        when(mockHttpClient.execute(any(HttpGet.class), any(ResponseHandler.class)))
            .thenReturn("I am an invalid response!");

        projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .withBlockingTimeout(10, TimeUnit.MILLISECONDS)
            .build();

        assertNull(projectConfigManager.getConfig());
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
}
