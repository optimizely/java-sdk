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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class ODPManagerTest {
    private static final List<String> API_RESPONSE = Arrays.asList(new String[]{"segment1", "segment2"});

    @Mock
    ODPApiManager mockApiManager;

    @Mock
    ODPEventManager mockEventManager;

    @Mock
    ODPSegmentManager mockSegmentManager;

    @Before
    public void setup() {
        mockApiManager = mock(ODPApiManager.class);
        mockEventManager = mock(ODPEventManager.class);
        mockSegmentManager = mock(ODPSegmentManager.class);
    }

    @Test
    public void shouldStartEventManagerWhenODPManagerIsInitialized() {
        ODPManager.builder().withSegmentManager(mockSegmentManager).withEventManager(mockEventManager).build();

        verify(mockEventManager, times(1)).start();
    }

    @Test
    public void shouldStopEventManagerWhenCloseIsCalled() {
        ODPManager odpManager = ODPManager.builder().withSegmentManager(mockSegmentManager).withEventManager(mockEventManager).build();
        odpManager.updateSettings("test-key", "test-host", Collections.emptySet());

        // Stop is not called in the default flow.
        verify(mockEventManager, times(0)).stop();

        odpManager.close();
        // stop should be called when odpManager is closed.
        verify(mockEventManager, times(1)).stop();
    }

    @Test
    public void shouldUseNewSettingsInEventManagerWhenODPConfigIsUpdated() throws InterruptedException {
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(200);
        ODPManager odpManager = ODPManager.builder().withApiManager(mockApiManager).build();
        odpManager.updateSettings("test-host", "test-key", new HashSet<>(Arrays.asList("segment1", "segment2")));

        odpManager.getEventManager().identifyUser("vuid", "fsuid");
        Thread.sleep(2000);
        verify(mockApiManager, times(1))
            .sendEvents(eq("test-key"), eq("test-host/v3/events"), any());

        odpManager.updateSettings("test-host-updated", "test-key-updated", new HashSet<>(Arrays.asList("segment1")));
        odpManager.getEventManager().identifyUser("vuid", "fsuid");
        Thread.sleep(1200);
        verify(mockApiManager, times(1))
            .sendEvents(eq("test-key-updated"), eq("test-host-updated/v3/events"), any());
    }

    @Test
    public void shouldUseNewSettingsInSegmentManagerWhenODPConfigIsUpdated() {
        Mockito.when(mockApiManager.fetchQualifiedSegments(anyString(), anyString(), anyString(), anyString(), anySet()))
            .thenReturn(API_RESPONSE);
        ODPManager odpManager = ODPManager.builder().withApiManager(mockApiManager).build();
        odpManager.updateSettings("test-host", "test-key", new HashSet<>(Arrays.asList("segment1", "segment2")));

        odpManager.getSegmentManager().getQualifiedSegments("test-id");
        verify(mockApiManager, times(1))
            .fetchQualifiedSegments(eq("test-key"), eq("test-host/v3/graphql"), any(), any(), any());

        odpManager.updateSettings("test-host-updated", "test-key-updated", new HashSet<>(Arrays.asList("segment1")));
        odpManager.getSegmentManager().getQualifiedSegments("test-id");
        verify(mockApiManager, times(1))
            .fetchQualifiedSegments(eq("test-key-updated"), eq("test-host-updated/v3/graphql"), any(), any(), any());
    }

    @Test
    public void shouldGetEventManager() {
        ODPManager odpManager = ODPManager.builder().withSegmentManager(mockSegmentManager).withEventManager(mockEventManager).build();
        assertNotNull(odpManager.getEventManager());

        odpManager = ODPManager.builder().withApiManager(mockApiManager).build();
        assertNotNull(odpManager.getEventManager());
    }

    @Test
    public void shouldGetSegmentManager() {
        ODPManager odpManager = ODPManager.builder().withSegmentManager(mockSegmentManager).withEventManager(mockEventManager).build();
        assertNotNull(odpManager.getSegmentManager());

        odpManager = ODPManager.builder().withApiManager(mockApiManager).build();
        assertNotNull(odpManager.getSegmentManager());
    }

    @Test
    public void isVuid() {
        assertTrue(ODPManager.isVuid("vuid_123"));
        assertFalse(ODPManager.isVuid("vuid123"));
        assertFalse(ODPManager.isVuid("any_123"));
        assertFalse(ODPManager.isVuid(""));
    }
}
