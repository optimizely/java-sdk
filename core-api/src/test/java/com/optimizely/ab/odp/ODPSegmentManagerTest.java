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
import com.optimizely.ab.internal.Cache;
import com.optimizely.ab.internal.LogbackVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class ODPSegmentManagerTest {

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Mock
    Cache<List<String>> mockCache;

    @Mock
    ODPApiManager mockApiManager;

    private static final String API_RESPONSE = "{\"data\":{\"customer\":{\"audiences\":{\"edges\":[{\"node\":{\"name\":\"segment1\",\"state\":\"qualified\"}},{\"node\":{\"name\":\"segment2\",\"state\":\"qualified\"}}]}}}}";

    @Before
    public void setup() {
        mockCache = mock(Cache.class);
        mockApiManager = mock(ODPApiManager.class);
    }

    @Test
    public void cacheHit() {
        Mockito.when(mockCache.lookup(any())).thenReturn(Arrays.asList("segment1-cached", "segment2-cached"));

        ODPConfig odpConfig = new ODPConfig("testKey", "testHost", new HashSet<>(Arrays.asList("segment1", "segment2")));
        ODPSegmentManager segmentManager = new ODPSegmentManager(mockApiManager, mockCache);
        segmentManager.updateSettings(odpConfig);
        List<String> segments = segmentManager.getQualifiedSegments(ODPUserKey.FS_USER_ID, "testId");

        // Cache lookup called with correct key
        verify(mockCache, times(1)).lookup("fs_user_id-$-testId");

        // Cache hit! No api call was made to the server.
        verify(mockApiManager, times(0)).fetchQualifiedSegments(any(), any(), any(), any(), any());
        verify(mockCache, times(0)).save(any(), any());
        verify(mockCache, times(0)).reset();

        logbackVerifier.expectMessage(Level.DEBUG, "ODP Cache Hit. Returning segments from Cache.");

        assertEquals(Arrays.asList("segment1-cached", "segment2-cached"), segments);
    }

    @Test
    public void cacheMiss() {
        Mockito.when(mockCache.lookup(any())).thenReturn(null);
        Mockito.when(mockApiManager.fetchQualifiedSegments(anyString(), anyString(), anyString(), anyString(), anySet()))
            .thenReturn(API_RESPONSE);

        ODPConfig odpConfig = new ODPConfig("testKey", "testHost", new HashSet<>(Arrays.asList("segment1", "segment2")));
        ODPSegmentManager segmentManager = new ODPSegmentManager(mockApiManager, mockCache);
        segmentManager.updateSettings(odpConfig);
        List<String> segments = segmentManager.getQualifiedSegments(ODPUserKey.VUID, "testId");

        // Cache lookup called with correct key
        verify(mockCache, times(1)).lookup("vuid-$-testId");

        // Cache miss! Make api call and save to cache
        verify(mockApiManager, times(1))
            .fetchQualifiedSegments(odpConfig.getApiKey(), odpConfig.getApiHost() + "/v3/graphql", "vuid", "testId", new HashSet<>(Arrays.asList("segment1", "segment2")));
        verify(mockCache, times(1)).save("vuid-$-testId", Arrays.asList("segment1", "segment2"));
        verify(mockCache, times(0)).reset();

        logbackVerifier.expectMessage(Level.DEBUG, "ODP Cache Miss. Making a call to ODP Server.");

        assertEquals(Arrays.asList("segment1", "segment2"), segments);
    }

    @Test
    public void ignoreCache() {
        Mockito.when(mockCache.lookup(any())).thenReturn(Arrays.asList("segment1-cached", "segment2-cached"));
        Mockito.when(mockApiManager.fetchQualifiedSegments(anyString(), anyString(), anyString(), anyString(), anySet()))
            .thenReturn(API_RESPONSE);

        ODPConfig odpConfig = new ODPConfig("testKey", "testHost", new HashSet<>(Arrays.asList("segment1", "segment2")));
        ODPSegmentManager segmentManager = new ODPSegmentManager(mockApiManager, mockCache);
        segmentManager.updateSettings(odpConfig);
        List<String> segments = segmentManager.getQualifiedSegments(ODPUserKey.FS_USER_ID, "testId", Collections.singletonList(ODPSegmentOption.IGNORE_CACHE));

        // Cache Ignored! lookup should not be called
        verify(mockCache, times(0)).lookup(any());

        // Cache Ignored! Make API Call but do NOT save because of cacheIgnore
        verify(mockApiManager, times(1))
            .fetchQualifiedSegments(odpConfig.getApiKey(), odpConfig.getApiHost() + "/v3/graphql", "fs_user_id", "testId", new HashSet<>(Arrays.asList("segment1", "segment2")));
        verify(mockCache, times(0)).save(any(), any());
        verify(mockCache, times(0)).reset();

        assertEquals(Arrays.asList("segment1", "segment2"), segments);
    }

    @Test
    public void resetCache() {
        Mockito.when(mockCache.lookup(any())).thenReturn(Arrays.asList("segment1-cached", "segment2-cached"));
        Mockito.when(mockApiManager.fetchQualifiedSegments(anyString(), anyString(), anyString(), anyString(), anySet()))
            .thenReturn(API_RESPONSE);

        ODPConfig odpConfig = new ODPConfig("testKey", "testHost", new HashSet<>(Arrays.asList("segment1", "segment2")));
        ODPSegmentManager segmentManager = new ODPSegmentManager(mockApiManager, mockCache);
        segmentManager.updateSettings(odpConfig);
        List<String> segments = segmentManager.getQualifiedSegments(ODPUserKey.FS_USER_ID, "testId", Collections.singletonList(ODPSegmentOption.RESET_CACHE));

        // Call reset
        verify(mockCache, times(1)).reset();

        // Cache Reset! lookup should not be called because cache would be empty.
        verify(mockCache, times(0)).lookup(any());

        // Cache reset but not Ignored! Make API Call and save to cache
        verify(mockApiManager, times(1))
            .fetchQualifiedSegments(odpConfig.getApiKey(), odpConfig.getApiHost() + "/v3/graphql", "fs_user_id", "testId", new HashSet<>(Arrays.asList("segment1", "segment2")));
        verify(mockCache, times(1)).save("fs_user_id-$-testId", Arrays.asList("segment1", "segment2"));

        assertEquals(Arrays.asList("segment1", "segment2"), segments);
    }

    @Test
    public void resetAndIgnoreCache() {
        Mockito.when(mockCache.lookup(any())).thenReturn(Arrays.asList("segment1-cached", "segment2-cached"));
        Mockito.when(mockApiManager.fetchQualifiedSegments(anyString(), anyString(), anyString(), anyString(), anySet()))
            .thenReturn(API_RESPONSE);

        ODPConfig odpConfig = new ODPConfig("testKey", "testHost", new HashSet<>(Arrays.asList("segment1", "segment2")));
        ODPSegmentManager segmentManager = new ODPSegmentManager(mockApiManager, mockCache);
        segmentManager.updateSettings(odpConfig);
        List<String> segments = segmentManager
            .getQualifiedSegments(ODPUserKey.FS_USER_ID, "testId", Arrays.asList(ODPSegmentOption.RESET_CACHE, ODPSegmentOption.IGNORE_CACHE));

        // Call reset
        verify(mockCache, times(1)).reset();

        verify(mockCache, times(0)).lookup(any());

        // Cache is also Ignored! Make API Call but do not save
        verify(mockApiManager, times(1))
            .fetchQualifiedSegments(odpConfig.getApiKey(), odpConfig.getApiHost() + "/v3/graphql", "fs_user_id", "testId", new HashSet<>(Arrays.asList("segment1", "segment2")));
        verify(mockCache, times(0)).save(any(), any());

        assertEquals(Arrays.asList("segment1", "segment2"), segments);
    }

    @Test
    public void odpConfigNotReady() {
        Mockito.when(mockCache.lookup(any())).thenReturn(Arrays.asList("segment1-cached", "segment2-cached"));

        ODPConfig odpConfig = new ODPConfig(null, null, new HashSet<>(Arrays.asList("segment1", "segment2")));
        ODPSegmentManager segmentManager = new ODPSegmentManager(mockApiManager, mockCache);
        segmentManager.updateSettings(odpConfig);
        List<String> segments = segmentManager.getQualifiedSegments(ODPUserKey.FS_USER_ID, "testId");

        // No further methods should be called.
        verify(mockCache, times(0)).lookup("fs_user_id-$-testId");
        verify(mockApiManager, times(0)).fetchQualifiedSegments(any(), any(), any(), any(), any());
        verify(mockCache, times(0)).save(any(), any());
        verify(mockCache, times(0)).reset();

        logbackVerifier.expectMessage(Level.ERROR, "Audience segments fetch failed (ODP is not enabled)");

        assertEquals(Collections.emptyList(), segments);
    }

    @Test
    public void noSegmentsInProject() {
        Mockito.when(mockCache.lookup(any())).thenReturn(Arrays.asList("segment1-cached", "segment2-cached"));

        ODPConfig odpConfig = new ODPConfig("testKey", "testHost", null);
        ODPSegmentManager segmentManager = new ODPSegmentManager(mockApiManager, mockCache);
        segmentManager.updateSettings(odpConfig);
        List<String> segments = segmentManager.getQualifiedSegments(ODPUserKey.FS_USER_ID, "testId");

        // No further methods should be called.
        verify(mockCache, times(0)).lookup("fs_user_id-$-testId");
        verify(mockApiManager, times(0)).fetchQualifiedSegments(any(), any(), any(), any(), any());
        verify(mockCache, times(0)).save(any(), any());
        verify(mockCache, times(0)).reset();

        logbackVerifier.expectMessage(Level.DEBUG, "No Segments are used in the project, Not Fetching segments. Returning empty list");

        assertEquals(Collections.emptyList(), segments);
    }
}
