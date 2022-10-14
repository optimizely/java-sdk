package com.optimizely.ab.odp;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class ODPManagerTest {
    private static final String API_RESPONSE = "{\"data\":{\"customer\":{\"audiences\":{\"edges\":[{\"node\":{\"name\":\"segment1\",\"state\":\"qualified\"}},{\"node\":{\"name\":\"segment2\",\"state\":\"qualified\"}}]}}}}";

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
        ODPConfig config = new ODPConfig("test-key", "test-host");
        ODPManager odpManager = new ODPManager(config, mockSegmentManager, mockEventManager);
        verify(mockEventManager, times(1)).start();
    }

    @Test
    public void shouldStopEventManagerWhenCloseIsCalled() {
        ODPConfig config = new ODPConfig("test-key", "test-host");
        ODPManager odpManager = new ODPManager(config, mockSegmentManager, mockEventManager);

        // Stop is not called in the default flow.
        verify(mockEventManager, times(0)).stop();

        odpManager.close();
        // stop should be called when odpManager is closed.
        verify(mockEventManager, times(1)).stop();
    }

    @Test
    public void shouldUseNewSettingsInEventManagerWhenODPConfigIsUpdated() throws InterruptedException {
        Mockito.when(mockApiManager.sendEvents(any(), any(), any())).thenReturn(200);
        ODPConfig config = new ODPConfig("test-key", "test-host", Arrays.asList("segment1", "segment2"));
        ODPManager odpManager = new ODPManager(config, mockApiManager);

        odpManager.getEventManager().identifyUser("vuid", "fsuid");
        Thread.sleep(2000);
        verify(mockApiManager, times(1))
            .sendEvents(eq("test-key"), eq("test-host/v3/events"), any());

        odpManager.updateSettings("test-host-updated", "test-key-updated", Arrays.asList("segment1"));
        odpManager.getEventManager().identifyUser("vuid", "fsuid");
        Thread.sleep(1200);
        verify(mockApiManager, times(1))
            .sendEvents(eq("test-key-updated"), eq("test-host-updated/v3/events"), any());
    }

    @Test
    public void shouldUseNewSettingsInSegmentManagerWhenODPConfigIsUpdated() {
        Mockito.when(mockApiManager.fetchQualifiedSegments(anyString(), anyString(), anyString(), anyString(), anyList()))
            .thenReturn(API_RESPONSE);
        ODPConfig config = new ODPConfig("test-key", "test-host", Arrays.asList("segment1", "segment2"));
        ODPManager odpManager = new ODPManager(config, mockApiManager);

        odpManager.getSegmentManager().getQualifiedSegments("test-id");
        verify(mockApiManager, times(1))
            .fetchQualifiedSegments(eq("test-key"), eq("test-host/v3/graphql"), any(), any(), any());

        odpManager.updateSettings("test-host-updated", "test-key-updated", Arrays.asList("segment1"));
        odpManager.getSegmentManager().getQualifiedSegments("test-id");
        verify(mockApiManager, times(1))
            .fetchQualifiedSegments(eq("test-key-updated"), eq("test-host-updated/v3/graphql"), any(), any(), any());
    }

    @Test
    public void shouldGetEventManager() {
        ODPConfig config = new ODPConfig("test-key", "test-host");
        ODPManager odpManager = new ODPManager(config, mockSegmentManager, mockEventManager);
        assertNotNull(odpManager.getEventManager());

        odpManager = new ODPManager(config, mockApiManager);
        assertNotNull(odpManager.getEventManager());
    }

    @Test
    public void shouldGetSegmentManager() {
        ODPConfig config = new ODPConfig("test-key", "test-host");
        ODPManager odpManager = new ODPManager(config, mockSegmentManager, mockEventManager);
        assertNotNull(odpManager.getSegmentManager());

        odpManager = new ODPManager(config, mockApiManager);
        assertNotNull(odpManager.getSegmentManager());
    }
}