/**
 *
 *    Copyright 2022, Optimizely
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
import com.optimizely.ab.internal.LogbackVerifier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ODPEventManagerTest {

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Mock
    ODPApiManager mockApiManager;

    @Captor
    ArgumentCaptor<String> payloadCaptor;

    @Before
    public void setup() {
        mockApiManager = mock(ODPApiManager.class);
    }

    @Test
    public void logAndDiscardEventWhenEventManagerIsNotRunning() {
        ODPConfig odpConfig = new ODPConfig("key", "host", null);
        ODPEventManager eventManager = new ODPEventManager(odpConfig, mockApiManager);
        ODPEvent event = new ODPEvent("test-type", "test-action", Collections.emptyMap(), Collections.emptyMap());
        eventManager.sendEvent(event);
        logbackVerifier.expectMessage(Level.WARN, "Failed to Process ODP Event. ODPEventManager is not running");
    }

    @Test
    public void logAndDiscardEventWhenODPConfigNotReady() {
        ODPConfig odpConfig = new ODPConfig(null, null, null);
        ODPEventManager eventManager = new ODPEventManager(odpConfig, mockApiManager);
        eventManager.start();
        ODPEvent event = new ODPEvent("test-type", "test-action", Collections.emptyMap(), Collections.emptyMap());
        eventManager.sendEvent(event);
        logbackVerifier.expectMessage(Level.DEBUG, "Unable to Process ODP Event. ODPConfig is not ready.");
    }

    @Test
    public void dispatchEventsInCorrectNumberOfBatches() throws InterruptedException {
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(202);
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(odpConfig, mockApiManager);
        eventManager.start();
        for (int i = 0; i < 25; i++) {
            eventManager.sendEvent(getEvent(i));
        }
        Thread.sleep(1500);
        Mockito.verify(mockApiManager, times(3)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), any());
    }

    @Test
    public void dispatchEventsWithCorrectPayload() throws InterruptedException {
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(202);
        int batchSize = 2;
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(odpConfig, mockApiManager, batchSize, null, null);
        eventManager.start();
        for (int i = 0; i < 6; i++) {
            eventManager.sendEvent(getEvent(i));
        }
        Thread.sleep(500);
        Mockito.verify(mockApiManager, times(3)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), payloadCaptor.capture());
        List<String> payloads = payloadCaptor.getAllValues();

        for (int i = 0; i < payloads.size(); i++) {
            JSONArray events = new JSONArray(payloads.get(i));
            assertEquals(batchSize, events.length());
            for (int j = 0; j < events.length(); j++) {
                int id = (batchSize * i) + j;
                JSONObject event = events.getJSONObject(j);
                assertEquals("test-type-" + id , event.getString("type"));
                assertEquals("test-action-" + id , event.getString("action"));
                assertEquals("value1-" + id, event.getJSONObject("identifiers").getString("identifier1"));
                assertEquals("value2-" + id, event.getJSONObject("identifiers").getString("identifier2"));
                assertEquals("data-value1-" + id, event.getJSONObject("data").getString("data1"));
                assertEquals(id, event.getJSONObject("data").getInt("data2"));
                assertEquals("sdk", event.getJSONObject("data").getString("data_source_type"));
            }
        }
    }

    @Test
    public void dispatchEventsWithCorrectFlushInterval() throws InterruptedException {
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(202);
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(odpConfig, mockApiManager);
        eventManager.start();
        for (int i = 0; i < 25; i++) {
            eventManager.sendEvent(getEvent(i));
        }
        Thread.sleep(500);
        Mockito.verify(mockApiManager, times(2)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), any());

        // Last batch is incomplete so it needs almost a second to flush.
        Thread.sleep(1500);
        Mockito.verify(mockApiManager, times(3)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), any());
    }

    @Test
    public void retryFailedEvents() throws InterruptedException {
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(500);
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(odpConfig, mockApiManager);
        eventManager.start();
        for (int i = 0; i < 25; i++) {
            eventManager.sendEvent(getEvent(i));
        }
        Thread.sleep(500);

        // Should be called thrice for each batch
        Mockito.verify(mockApiManager, times(6)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), any());

        // Last batch is incomplete so it needs almost a second to flush.
        Thread.sleep(1500);
        Mockito.verify(mockApiManager, times(9)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), any());
    }

    @Test
    public void shouldFlushAllScheduledEventsBeforeStopping() throws InterruptedException {
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(202);
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(odpConfig, mockApiManager);
        eventManager.start();
        for (int i = 0; i < 25; i++) {
            eventManager.sendEvent(getEvent(i));
        }
        eventManager.stop();
        Thread.sleep(1500);
        Mockito.verify(mockApiManager, times(3)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), any());
        logbackVerifier.expectMessage(Level.DEBUG, "Exiting ODP Event Dispatcher Thread.");
    }

    @Test
    public void prepareCorrectPayloadForIdentifyUser() throws InterruptedException {
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(202);
        int batchSize = 2;
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(odpConfig, mockApiManager, batchSize, null, null);
        eventManager.start();
        for (int i = 0; i < 2; i++) {
            eventManager.identifyUser("the-vuid-" + i, "the-fs-user-id-" + i);
        }

        Thread.sleep(1500);
        Mockito.verify(mockApiManager, times(1)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        JSONArray events = new JSONArray(payload);
        assertEquals(batchSize, events.length());
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            assertEquals("fullstack", event.getString("type"));
            assertEquals("client_initialized", event.getString("action"));
            assertEquals("the-vuid-" + i, event.getJSONObject("identifiers").getString("vuid"));
            assertEquals("the-fs-user-id-" + i, event.getJSONObject("identifiers").getString("fs_user_id"));
            assertEquals("sdk", event.getJSONObject("data").getString("data_source_type"));
        }
    }

    @Test
    public void applyUpdatedODPConfigWhenAvailable() throws InterruptedException {
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(202);
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(odpConfig, mockApiManager);
        eventManager.start();
        for (int i = 0; i < 25; i++) {
            eventManager.sendEvent(getEvent(i));
        }
        Thread.sleep(500);
        Mockito.verify(mockApiManager, times(2)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), any());
        eventManager.updateSettings(new ODPConfig("new-key", "http://www.new-odp-host.com"));

        // Should immediately Flush current batch with old ODP config when settings are changed
        Thread.sleep(100);
        Mockito.verify(mockApiManager, times(3)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), any());

        // New events should use new config
        for (int i = 0; i < 10; i++) {
            eventManager.sendEvent(getEvent(i));
        }
        Thread.sleep(100);
        Mockito.verify(mockApiManager, times(1)).sendEvents(eq("new-key"), eq("http://www.new-odp-host.com/v3/events"), any());
    }

    @Test
    public void validateEventData() {
        ODPEvent event = new ODPEvent("type", "action", null, null);
        Map<String, Object> data = new HashMap<>();

        data.put("String", "string Value");
        data.put("Integer", 100);
        data.put("Float", 33.89);
        data.put("Boolean", true);
        data.put("null", null);
        event.setData(data);
        assertTrue(event.isDataValid());

        data.put("RandomObject", new Object());
        assertFalse(event.isDataValid());
    }

    private ODPEvent getEvent(int id) {
        Map<String, String> identifiers = new HashMap<>();
        identifiers.put("identifier1", "value1-" + id);
        identifiers.put("identifier2", "value2-" + id);

        Map<String, Object> data = new HashMap<>();
        data.put("data1", "data-value1-" + id);
        data.put("data2", id);

        return new ODPEvent("test-type-" + id , "test-action-" + id, identifiers, data);
    }
}
