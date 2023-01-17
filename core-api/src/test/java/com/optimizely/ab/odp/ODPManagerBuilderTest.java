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

import com.optimizely.ab.internal.Cache;
import org.junit.Test;

import java.util.*;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class ODPManagerBuilderTest {

    @Test
    public void withApiManager() {
        ODPApiManager mockApiManager = mock(ODPApiManager.class);
        ODPManager odpManager = ODPManager.builder().withApiManager(mockApiManager).build();
        odpManager.updateSettings("test-host", "test-key", new HashSet<>(Arrays.asList("Segment-1", "Segment-2")));
        odpManager.getSegmentManager().getQualifiedSegments("test-user");
        verify(mockApiManager).fetchQualifiedSegments(any(), any(), any(), any(), any());
    }

    @Test
    public void withSegmentManager() {
        ODPSegmentManager mockSegmentManager = mock(ODPSegmentManager.class);
        ODPEventManager mockEventManager = mock(ODPEventManager.class);
        ODPManager odpManager = ODPManager.builder()
            .withSegmentManager(mockSegmentManager)
            .withEventManager(mockEventManager)
            .build();
        assertSame(mockSegmentManager, odpManager.getSegmentManager());
    }

    @Test
    public void withEventManager() {
        ODPSegmentManager mockSegmentManager = mock(ODPSegmentManager.class);
        ODPEventManager mockEventManager = mock(ODPEventManager.class);
        ODPManager odpManager = ODPManager.builder()
            .withSegmentManager(mockSegmentManager)
            .withEventManager(mockEventManager)
            .build();
        assertSame(mockEventManager, odpManager.getEventManager());
    }

    @Test
    public void withSegmentCache() {
        Cache<List<String>> mockCache = mock(Cache.class);
        ODPApiManager mockApiManager = mock(ODPApiManager.class);
        ODPManager odpManager = ODPManager.builder()
            .withApiManager(mockApiManager)
            .withSegmentCache(mockCache)
            .build();

        odpManager.updateSettings("test-host", "test-key", new HashSet<>(Arrays.asList("Segment-1", "Segment-2")));
        odpManager.getSegmentManager().getQualifiedSegments("test-user");
        verify(mockCache).lookup("fs_user_id-$-test-user");
    }

    @Test
    public void withUserCommonDataAndCommonIdentifiers() {
        Map<String, Object> data = new HashMap<>();
        data.put("k1", "v1");
        Map<String, String> identifiers = new HashMap<>();
        identifiers.put("k2", "v2");

        ODPEventManager mockEventManager = mock(ODPEventManager.class);
        ODPSegmentManager mockSegmentManager = mock(ODPSegmentManager.class);
        ODPManager.builder()
            .withUserCommonData(data)
            .withUserCommonIdentifiers(identifiers)
            .withEventManager(mockEventManager)
            .withSegmentManager(mockSegmentManager)
            .build();

        verify(mockEventManager).setUserCommonData(eq(data));
        verify(mockEventManager).setUserCommonIdentifiers(eq(identifiers));
    }

}
