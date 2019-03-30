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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;

import static com.optimizely.ab.config.HttpProjectConfigManager.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpProjectConfigManagerTest {

    @Mock
    OptimizelyHttpClient mockHttpClient;

    private String datafileString;

    @Before
    public void setUp() throws Exception {
        datafileString = Resources.toString(Resources.getResource("valid-project-config-v4.json"), Charsets.UTF_8);
    }

    @Test
    public void testHttpGetBySdkKey() throws Exception {
        HttpProjectConfigManager projectConfigManager = builder()
            .withSdkKey("sdk-key")
            .build();

        HttpGet actual = projectConfigManager.getHttpGet();

        assertEquals("GET", actual.getMethod());
        assertEquals(new URI("https://cdn.optimizely.com/datafiles/sdk-key.json"), actual.getURI());
    }

    @Test
    public void testHttpGetByCustomFormat() throws Exception {
        HttpProjectConfigManager projectConfigManager = builder()
            .withSdkKey("sdk-key")
            .withFormat("https://custom.optimizely.com/%s.json")
            .build();

        HttpGet actual = projectConfigManager.getHttpGet();

        assertEquals("GET", actual.getMethod());
        assertEquals(new URI("https://custom.optimizely.com/sdk-key.json"), actual.getURI());
    }

    @Test
    public void testHttpGetByCustomUrl() throws Exception {
        String expected = "https://custom.optimizely.com/custom-location.json";

        HttpProjectConfigManager projectConfigManager = builder()
            .withUrl(expected)
            .build();

        HttpGet actual = projectConfigManager.getHttpGet();

        assertEquals("GET", actual.getMethod());
        assertEquals(new URI(expected), actual.getURI());
    }

    @Test
    public void testGetConfig() throws Exception {
        HttpProjectConfigManager projectConfigManager = builder()
            .withOptimizelyHttpClient(mockHttpClient)
            .withSdkKey("sdk-key")
            .build();

        ProjectConfig projectConfig = new DatafileProjectConfig.Builder().withDatafile(datafileString).build();
        when(mockHttpClient.execute(eq(projectConfigManager.getHttpGet()), any(ResponseHandler.class)))
            .thenReturn(projectConfig);

        assertEquals(projectConfig, projectConfigManager.getConfig());
    }

    @Test
    public void testProjectConfigResponseHandler2XX() throws Exception {
        HttpProjectConfigManager projectConfigManager = builder()
            .withSdkKey("sdk-key")
            .build();

        ResponseHandler<ProjectConfig> handler = projectConfigManager.getResponseHandler();

        HttpResponse getResponse = new BasicHttpResponse(new ProtocolVersion("TEST", 0, 0), 200, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        ProjectConfig projectConfig = handler.handleResponse(getResponse);
        assertNotNull(projectConfig);
        assertEquals("4", projectConfig.getVersion());
    }

    @Test(expected = ClientProtocolException.class)
    public void testProjectConfigResponseHandler3XX() throws Exception {
        HttpProjectConfigManager projectConfigManager = builder()
            .withSdkKey("sdk-key")
            .build();

        ResponseHandler<ProjectConfig> handler = projectConfigManager.getResponseHandler();

        HttpResponse getResponse = new BasicHttpResponse(new ProtocolVersion("TEST", 0, 0), 300, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        handler.handleResponse(getResponse);
    }

    @Test(expected = ClientProtocolException.class)
    public void testProjectConfigResponseHandler4XX() throws Exception {
        HttpProjectConfigManager projectConfigManager = builder()
            .withSdkKey("sdk-key")
            .build();

        ResponseHandler<ProjectConfig> handler = projectConfigManager.getResponseHandler();

        HttpResponse getResponse = new BasicHttpResponse(new ProtocolVersion("TEST", 0, 0), 400, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        handler.handleResponse(getResponse);
    }

    @Test(expected = ClientProtocolException.class)
    public void testProjectConfigResponseHandler5XX() throws Exception {
        HttpProjectConfigManager projectConfigManager = builder()
            .withSdkKey("sdk-key")
            .build();

        ResponseHandler<ProjectConfig> handler = projectConfigManager.getResponseHandler();

        HttpResponse getResponse = new BasicHttpResponse(new ProtocolVersion("TEST", 0, 0), 500, "TEST");
        getResponse.setEntity(new StringEntity(datafileString));

        handler.handleResponse(getResponse);
    }

    @Test
    @Ignore
    public void testBasicFetch() throws Exception {
        ProjectConfigManager projectConfigManager = builder()
            .withSdkKey("7vPf3v7zye3fY4PcbejeCz")
            .build();

        ProjectConfig actual = projectConfigManager.getConfig();
        assertNotNull(actual);
        assertEquals("4", actual.getVersion());
    }
}
