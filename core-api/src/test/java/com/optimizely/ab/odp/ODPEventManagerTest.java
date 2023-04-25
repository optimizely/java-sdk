/**
 *
 *    Copyright 2022-2023, Optimizely
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
import com.optimizely.ab.event.internal.BuildVersionInfo;
import com.optimizely.ab.event.internal.ClientEngineInfo;
import com.optimizely.ab.event.internal.payload.EventBatch;
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

import java.util.*;

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
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.updateSettings(odpConfig);
        ODPEvent event = new ODPEvent("test-type", "test-action", Collections.singletonMap("any-key", "any-value"), Collections.emptyMap());
        eventManager.sendEvent(event);
        logbackVerifier.expectMessage(Level.WARN, "Failed to Process ODP Event. ODPEventManager is not running");
    }

    @Test
    public void logAndDiscardEventWhenODPConfigNotReady() {
        ODPConfig odpConfig = new ODPConfig(null, null, null);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.updateSettings(odpConfig);
        eventManager.start();
        ODPEvent event = new ODPEvent("test-type", "test-action", Collections.singletonMap("any-key", "any-value"), Collections.emptyMap());
        eventManager.sendEvent(event);
        logbackVerifier.expectMessage(Level.DEBUG, "Unable to Process ODP Event. ODPConfig is not ready.");
    }

    @Test
    public void dispatchEventsInCorrectNumberOfBatches() throws InterruptedException {
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(202);
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.updateSettings(odpConfig);
        eventManager.start();
        for (int i = 0; i < 25; i++) {
            eventManager.sendEvent(getEvent(i));
        }
        Thread.sleep(1500);
        Mockito.verify(mockApiManager, times(3)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), any());
    }

    @Test
    public void logAndDiscardEventWhenIdentifiersEmpty()  throws InterruptedException {
        int flushInterval = 0;
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager, null, flushInterval);
        eventManager.updateSettings(odpConfig);
        eventManager.start();

        ODPEvent event = new ODPEvent("test-type", "test-action", Collections.emptyMap(), Collections.emptyMap());
        eventManager.sendEvent(event);
        Thread.sleep(500);
        Mockito.verify(mockApiManager, never()).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), any());
        logbackVerifier.expectMessage(Level.ERROR, "ODP event send failed (event identifiers must have at least one key-value pair)");
    }

    @Test
    public void dispatchEventsWithCorrectPayload() throws InterruptedException {
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(202);
        int flushInterval = 0;
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager, null, flushInterval);
        eventManager.updateSettings(odpConfig);
        eventManager.start();
        for (int i = 0; i < 6; i++) {
            eventManager.sendEvent(getEvent(i));
        }
        Thread.sleep(500);
        Mockito.verify(mockApiManager, times(6)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), payloadCaptor.capture());
        List<String> payloads = payloadCaptor.getAllValues();

        for (int i = 0; i < payloads.size(); i++) {
            JSONArray events = new JSONArray(payloads.get(i));
            assertEquals(1, events.length());
            for (int j = 0; j < events.length(); j++) {
                int id = (1 * i) + j;
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
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.updateSettings(odpConfig);
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
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.updateSettings(odpConfig);
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
        int flushInterval = 20000;
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(202);
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager, null, flushInterval);
        eventManager.updateSettings(odpConfig);
        eventManager.start();
        for (int i = 0; i < 8; i++) {
            eventManager.sendEvent(getEvent(i));
        }
        eventManager.stop();
        Thread.sleep(1500);
        Mockito.verify(mockApiManager, times(1)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), any());
        logbackVerifier.expectMessage(Level.DEBUG, "Exiting ODP Event Dispatcher Thread.");
    }

    @Test
    public void prepareCorrectPayloadForIdentifyUser() throws InterruptedException {
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(202);
        int flushInterval = 0;
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager, null, flushInterval);
        eventManager.updateSettings(odpConfig);
        eventManager.start();
        for (int i = 0; i < 2; i++) {
            eventManager.identifyUser("the-vuid-" + i, "the-fs-user-id-" + i);
        }

        Thread.sleep(1500);
        Mockito.verify(mockApiManager, times(2)).sendEvents(eq("key"), eq("http://www.odp-host.com/v3/events"), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        JSONArray events = new JSONArray(payload);
        assertEquals(1, events.length());
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            assertEquals("fullstack", event.getString("type"));
            assertEquals("identified", event.getString("action"));
            assertEquals("the-vuid-" + (i + 1), event.getJSONObject("identifiers").getString("vuid"));
            assertEquals("the-fs-user-id-" + (i + 1), event.getJSONObject("identifiers").getString("fs_user_id"));
            assertEquals("sdk", event.getJSONObject("data").getString("data_source_type"));
        }
    }

    @Test
    public void identifyUserWithVuidAndUserId() throws InterruptedException {
        ODPEventManager eventManager = spy(new ODPEventManager(mockApiManager));
        ArgumentCaptor<ODPEvent> captor = ArgumentCaptor.forClass(ODPEvent.class);

        eventManager.identifyUser("vuid_123", "test-user");
        verify(eventManager, times(1)).sendEvent(captor.capture());

        ODPEvent event = captor.getValue();
        Map<String, String> identifiers = event.getIdentifiers();
        assertEquals(identifiers.size(), 2);
        assertEquals(identifiers.get("vuid"), "vuid_123");
        assertEquals(identifiers.get("fs_user_id"), "test-user");
    }

    @Test
    public void identifyUserWithVuidOnly() throws InterruptedException {
        ODPEventManager eventManager = spy(new ODPEventManager(mockApiManager));
        ArgumentCaptor<ODPEvent> captor = ArgumentCaptor.forClass(ODPEvent.class);

        eventManager.identifyUser("vuid_123", null);
        verify(eventManager, times(1)).sendEvent(captor.capture());

        ODPEvent event = captor.getValue();
        Map<String, String> identifiers = event.getIdentifiers();
        assertEquals(identifiers.size(), 1);
        assertEquals(identifiers.get("vuid"), "vuid_123");
    }

    @Test
    public void identifyUserWithUserIdOnly() throws InterruptedException {
        ODPEventManager eventManager = spy(new ODPEventManager(mockApiManager));
        ArgumentCaptor<ODPEvent> captor = ArgumentCaptor.forClass(ODPEvent.class);

        eventManager.identifyUser(null, "test-user");
        verify(eventManager, times(1)).sendEvent(captor.capture());

        ODPEvent event = captor.getValue();
        Map<String, String> identifiers = event.getIdentifiers();
        assertEquals(identifiers.size(), 1);
        assertEquals(identifiers.get("fs_user_id"), "test-user");
    }

    @Test
    public void identifyUserWithVuidAsUserId() throws InterruptedException {
        ODPEventManager eventManager = spy(new ODPEventManager(mockApiManager));
        ArgumentCaptor<ODPEvent> captor = ArgumentCaptor.forClass(ODPEvent.class);

        eventManager.identifyUser(null, "vuid_123");
        verify(eventManager, times(1)).sendEvent(captor.capture());

        ODPEvent event = captor.getValue();
        Map<String, String> identifiers = event.getIdentifiers();
        assertEquals(identifiers.size(), 1);
        // SDK will convert userId to vuid when userId has a valid vuid format.
        assertEquals(identifiers.get("vuid"), "vuid_123");
    }

    @Test
    public void applyUpdatedODPConfigWhenAvailable() throws InterruptedException {
        Mockito.reset(mockApiManager);
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(202);
        ODPConfig odpConfig = new ODPConfig("key", "http://www.odp-host.com", null);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.updateSettings(odpConfig);
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

    @Test
    public void validateEventCommonData() {
        Map<String, Object> sourceData = new HashMap<>();
        sourceData.put("k1", "v1");

        Mockito.reset(mockApiManager);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        Map<String, Object> merged = eventManager.augmentCommonData(sourceData);

        assertEquals(merged.get("k1"), "v1");
        assertTrue(merged.get("idempotence_id").toString().length() > 16);
        assertEquals(merged.get("data_source_type"), "sdk");
        assertEquals(merged.get("data_source"), "java-sdk");
        assertTrue(merged.get("data_source_version").toString().length() > 0);
        assertEquals(merged.size(), 5);

        // when clientInfo is overridden (android-sdk):

        ClientEngineInfo.setClientEngine(EventBatch.ClientEngine.ANDROID_SDK);
        BuildVersionInfo.setClientVersion("1.2.3");
        merged = eventManager.augmentCommonData(sourceData);

        assertEquals(merged.get("k1"), "v1");
        assertTrue(merged.get("idempotence_id").toString().length() > 16);
        assertEquals(merged.get("data_source_type"), "sdk");
        assertEquals(merged.get("data_source"), "android-sdk");
        assertEquals(merged.get("data_source_version"), "1.2.3");
        assertEquals(merged.size(), 5);

        // restore the default values for other tests
        ClientEngineInfo.setClientEngine(ClientEngineInfo.DEFAULT);
        BuildVersionInfo.setClientVersion(BuildVersionInfo.VERSION);
    }

    @Test
    public void validateAugmentCommonData() {
        Map<String, Object> sourceData = new HashMap<>();
        sourceData.put("k1", "source-1");
        sourceData.put("k2", "source-2");
        Map<String, Object> userCommonData = new HashMap<>();
        userCommonData.put("k3", "common-1");
        userCommonData.put("k4", "common-2");

        Mockito.reset(mockApiManager);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.setUserCommonData(userCommonData);

        Map<String, Object> merged = eventManager.augmentCommonData(sourceData);

        // event-sourceData
        assertEquals(merged.get("k1"), "source-1");
        assertEquals(merged.get("k2"), "source-2");
        // userCommonData
        assertEquals(merged.get("k3"), "common-1");
        assertEquals(merged.get("k4"), "common-2");
        // sdk-generated common data
        assertNotNull(merged.get("idempotence_id"));
        assertEquals(merged.get("data_source_type"), "sdk");
        assertNotNull(merged.get("data_source"));
        assertNotNull(merged.get("data_source_version"));

        assertEquals(merged.size(), 8);
    }

    @Test
    public void validateAugmentCommonData_keyConflicts1() {
        Map<String, Object> sourceData = new HashMap<>();
        sourceData.put("k1", "source-1");
        sourceData.put("k2", "source-2");
        Map<String, Object> userCommonData = new HashMap<>();
        userCommonData.put("k1", "common-1");
        userCommonData.put("k2", "common-2");

        Mockito.reset(mockApiManager);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.setUserCommonData(userCommonData);

        Map<String, Object> merged = eventManager.augmentCommonData(sourceData);

        // event-sourceData overrides userCommonData
        assertEquals(merged.get("k1"), "source-1");
        assertEquals(merged.get("k2"), "source-2");
        // sdk-generated common data
        assertNotNull(merged.get("idempotence_id"));
        assertEquals(merged.get("data_source_type"), "sdk");
        assertNotNull(merged.get("data_source"));
        assertNotNull(merged.get("data_source_version"));

        assertEquals(merged.size(), 6);
    }

    @Test
    public void validateAugmentCommonData_keyConflicts2() {
        Map<String, Object> sourceData = new HashMap<>();
        sourceData.put("data_source_type", "source-1");
        Map<String, Object> userCommonData = new HashMap<>();
        userCommonData.put("data_source_type", "common-1");

        Mockito.reset(mockApiManager);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.setUserCommonData(userCommonData);

        Map<String, Object> merged = eventManager.augmentCommonData(sourceData);

        // event-sourceData overrides userCommonData and sdk-generated common data
        assertEquals(merged.get("data_source_type"), "source-1");
        // sdk-generated common data
        assertNotNull(merged.get("idempotence_id"));
        assertNotNull(merged.get("data_source"));
        assertNotNull(merged.get("data_source_version"));

        assertEquals(merged.size(), 4);
    }

    @Test
    public void validateAugmentCommonData_keyConflicts3() {
        Map<String, Object> sourceData = new HashMap<>();
        sourceData.put("k1", "source-1");
        Map<String, Object> userCommonData = new HashMap<>();
        userCommonData.put("data_source_type", "common-1");

        Mockito.reset(mockApiManager);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.setUserCommonData(userCommonData);

        Map<String, Object> merged = eventManager.augmentCommonData(sourceData);

        // userCommonData overrides sdk-generated common data
        assertEquals(merged.get("data_source_type"), "common-1");
        assertEquals(merged.get("k1"), "source-1");
        // sdk-generated common data
        assertNotNull(merged.get("idempotence_id"));
        assertNotNull(merged.get("data_source"));
        assertNotNull(merged.get("data_source_version"));

        assertEquals(merged.size(), 5);
    }

    @Test
    public void validateAugmentCommonIdentifiers() {
        Map<String, String> sourceIdentifiers = new HashMap<>();
        sourceIdentifiers.put("k1", "source-1");
        sourceIdentifiers.put("k2", "source-2");
        Map<String, String> userCommonIdentifiers = new HashMap<>();
        userCommonIdentifiers.put("k3", "common-1");
        userCommonIdentifiers.put("k4", "common-2");

        Mockito.reset(mockApiManager);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.setUserCommonIdentifiers(userCommonIdentifiers);

        Map<String, String> merged = eventManager.augmentCommonIdentifiers(sourceIdentifiers);

        // event-sourceIdentifiers
        assertEquals(merged.get("k1"), "source-1");
        assertEquals(merged.get("k2"), "source-2");
        // userCommonIdentifiers
        assertEquals(merged.get("k3"), "common-1");
        assertEquals(merged.get("k4"), "common-2");

        assertEquals(merged.size(), 4);
    }

    @Test
    public void validateAugmentCommonIdentifiers_keyConflicts() {
        Map<String, String> sourceIdentifiers = new HashMap<>();
        sourceIdentifiers.put("k1", "source-1");
        sourceIdentifiers.put("k2", "source-2");
        Map<String, String> userCommonIdentifiers = new HashMap<>();
        userCommonIdentifiers.put("k1", "common-1");
        userCommonIdentifiers.put("k2", "common-2");

        Mockito.reset(mockApiManager);
        ODPEventManager eventManager = new ODPEventManager(mockApiManager);
        eventManager.setUserCommonIdentifiers(userCommonIdentifiers);

        Map<String, String> merged = eventManager.augmentCommonIdentifiers(sourceIdentifiers);

        // event-sourceIdentifiers overrides userCommonIdentifiers
        assertEquals(merged.get("k1"), "source-1");
        assertEquals(merged.get("k2"), "source-2");

        assertEquals(merged.size(), 2);
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
