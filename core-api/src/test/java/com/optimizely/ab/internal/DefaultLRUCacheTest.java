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
package com.optimizely.ab.internal;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class DefaultLRUCacheTest {

    @Test
    public void createSaveAndLookupOneItem() {
        LRUCache<String> cache = new DefaultLRUCache<>();
        assertNull(cache.lookup("key1"));
        cache.save("key1", "value1");
        assertEquals("value1", cache.lookup("key1"));
    }

    @Test
    public void saveAndLookupMultipleItems() {
        DefaultLRUCache<List<String>> cache = new DefaultLRUCache<>();

        cache.save("user1", Arrays.asList("segment1", "segment2"));
        cache.save("user2", Arrays.asList("segment3", "segment4"));
        cache.save("user3", Arrays.asList("segment5", "segment6"));

        assertEquals("user3", cache.linkedList.get(0).key);
        assertEquals("user2", cache.linkedList.get(1).key);
        assertEquals("user1", cache.linkedList.get(2).key);

        assertEquals(Arrays.asList("segment1", "segment2"), cache.lookup("user1"));

        // Lookup should move user1 to top of the list and push down others.
        assertEquals("user1", cache.linkedList.get(0).key);
        assertEquals("user3", cache.linkedList.get(1).key);
        assertEquals("user2", cache.linkedList.get(2).key);

        assertEquals(Arrays.asList("segment3", "segment4"), cache.lookup("user2"));

        // Lookup should move user2 to top of the list and push down others.
        assertEquals("user2", cache.linkedList.get(0).key);
        assertEquals("user1", cache.linkedList.get(1).key);
        assertEquals("user3", cache.linkedList.get(2).key);

        assertEquals(Arrays.asList("segment5", "segment6"), cache.lookup("user3"));

        // Lookup should move user3 to top of the list and push down others.
        assertEquals("user3", cache.linkedList.get(0).key);
        assertEquals("user2", cache.linkedList.get(1).key);
        assertEquals("user1", cache.linkedList.get(2).key);
    }

    @Test
    public void whenCacheIsDisabled() {
        DefaultLRUCache<List<String>> cache = new DefaultLRUCache<>();
        cache.setMaxSize(0);
        cache.save("user1", Arrays.asList("segment1", "segment2"));
        cache.save("user2", Arrays.asList("segment3", "segment4"));
        cache.save("user3", Arrays.asList("segment5", "segment6"));

        assertNull(cache.lookup("user1"));
        assertNull(cache.lookup("user2"));
        assertNull(cache.lookup("user3"));
    }

    @Test
    public void whenItemsExpire() throws InterruptedException {
        DefaultLRUCache<List<String>> cache = new DefaultLRUCache<>();
        cache.setTimeout(1L);
        cache.save("user1", Arrays.asList("segment1", "segment2"));
        assertEquals(Arrays.asList("segment1", "segment2"), cache.lookup("user1"));
        assertEquals(1, cache.linkedList.size());
        assertEquals(1, cache.hashMap.size());
        Thread.sleep(1000);
        assertNull(cache.lookup("user1"));
        assertEquals(0, cache.linkedList.size());
        assertEquals(0, cache.hashMap.size());
    }

    @Test
    public void whenCacheReachesMaxSize() {
        DefaultLRUCache<List<String>> cache = new DefaultLRUCache<>();
        cache.setMaxSize(2);

        cache.save("user1", Arrays.asList("segment1", "segment2"));
        cache.save("user2", Arrays.asList("segment3", "segment4"));
        cache.save("user3", Arrays.asList("segment5", "segment6"));

        assertEquals(2, cache.linkedList.size());
        assertEquals(2, cache.hashMap.size());

        assertEquals(Arrays.asList("segment5", "segment6"), cache.lookup("user3"));
        assertEquals(Arrays.asList("segment3", "segment4"), cache.lookup("user2"));
        assertNull(cache.lookup("user1"));
    }

    @Test
    public void whenMaxSizeIsReducedInBetween() {
        DefaultLRUCache<List<String>> cache = new DefaultLRUCache<>();
        cache.save("user1", Arrays.asList("segment1", "segment2"));
        cache.save("user2", Arrays.asList("segment3", "segment4"));
        cache.save("user3", Arrays.asList("segment5", "segment6"));

        assertEquals(Arrays.asList("segment1", "segment2"), cache.lookup("user1"));
        assertEquals(Arrays.asList("segment3", "segment4"), cache.lookup("user2"));
        assertEquals(Arrays.asList("segment5", "segment6"), cache.lookup("user3"));

        assertEquals(3, cache.linkedList.size());
        assertEquals(3, cache.hashMap.size());

        cache.setMaxSize(1);

        assertEquals(Arrays.asList("segment5", "segment6"), cache.lookup("user3"));
        assertNull(cache.lookup("user1"));
        assertNull(cache.lookup("user2"));

        assertEquals(1, cache.linkedList.size());
        assertEquals(1, cache.hashMap.size());
    }
}
