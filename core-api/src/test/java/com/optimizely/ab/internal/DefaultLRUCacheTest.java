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
        Cache<String> cache = new DefaultLRUCache<>();
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

        String[] itemKeys = cache.linkedHashMap.keySet().toArray(new String[0]);
        assertEquals("user1", itemKeys[0]);
        assertEquals("user2", itemKeys[1]);
        assertEquals("user3", itemKeys[2]);

        assertEquals(Arrays.asList("segment1", "segment2"), cache.lookup("user1"));

        itemKeys = cache.linkedHashMap.keySet().toArray(new String[0]);
        // Lookup should move user1 to bottom of the list and push up others.
        assertEquals("user2", itemKeys[0]);
        assertEquals("user3", itemKeys[1]);
        assertEquals("user1", itemKeys[2]);

        assertEquals(Arrays.asList("segment3", "segment4"), cache.lookup("user2"));

        itemKeys = cache.linkedHashMap.keySet().toArray(new String[0]);
        // Lookup should move user2 to bottom of the list and push up others.
        assertEquals("user3", itemKeys[0]);
        assertEquals("user1", itemKeys[1]);
        assertEquals("user2", itemKeys[2]);

        assertEquals(Arrays.asList("segment5", "segment6"), cache.lookup("user3"));

        itemKeys = cache.linkedHashMap.keySet().toArray(new String[0]);
        // Lookup should move user3 to bottom of the list and push up others.
        assertEquals("user1", itemKeys[0]);
        assertEquals("user2", itemKeys[1]);
        assertEquals("user3", itemKeys[2]);
    }

    @Test
    public void saveShouldReorderList() {
        DefaultLRUCache<List<String>> cache = new DefaultLRUCache<>();

        cache.save("user1", Arrays.asList("segment1", "segment2"));
        cache.save("user2", Arrays.asList("segment3", "segment4"));
        cache.save("user3", Arrays.asList("segment5", "segment6"));

        String[] itemKeys = cache.linkedHashMap.keySet().toArray(new String[0]);
        assertEquals("user1", itemKeys[0]);
        assertEquals("user2", itemKeys[1]);
        assertEquals("user3", itemKeys[2]);

        cache.save("user1", Arrays.asList("segment1", "segment2"));

        itemKeys = cache.linkedHashMap.keySet().toArray(new String[0]);
        // save should move user1 to bottom of the list and push up others.
        assertEquals("user2", itemKeys[0]);
        assertEquals("user3", itemKeys[1]);
        assertEquals("user1", itemKeys[2]);

        cache.save("user2", Arrays.asList("segment3", "segment4"));

        itemKeys = cache.linkedHashMap.keySet().toArray(new String[0]);
        // save should move user2 to bottom of the list and push up others.
        assertEquals("user3", itemKeys[0]);
        assertEquals("user1", itemKeys[1]);
        assertEquals("user2", itemKeys[2]);

        cache.save("user3", Arrays.asList("segment5", "segment6"));

        itemKeys = cache.linkedHashMap.keySet().toArray(new String[0]);
        // save should move user3 to bottom of the list and push up others.
        assertEquals("user1", itemKeys[0]);
        assertEquals("user2", itemKeys[1]);
        assertEquals("user3", itemKeys[2]);
    }

    @Test
    public void whenCacheIsDisabled() {
        DefaultLRUCache<List<String>> cache = new DefaultLRUCache<>(0,Cache.DEFAULT_TIMEOUT_SECONDS);

        cache.save("user1", Arrays.asList("segment1", "segment2"));
        cache.save("user2", Arrays.asList("segment3", "segment4"));
        cache.save("user3", Arrays.asList("segment5", "segment6"));

        assertNull(cache.lookup("user1"));
        assertNull(cache.lookup("user2"));
        assertNull(cache.lookup("user3"));
    }

    @Test
    public void whenItemsExpire() throws InterruptedException {
        DefaultLRUCache<List<String>> cache = new DefaultLRUCache<>(Cache.DEFAULT_MAX_SIZE, 1);
        cache.save("user1", Arrays.asList("segment1", "segment2"));
        assertEquals(Arrays.asList("segment1", "segment2"), cache.lookup("user1"));
        assertEquals(1, cache.linkedHashMap.size());
        Thread.sleep(1000);
        assertNull(cache.lookup("user1"));
        assertEquals(0, cache.linkedHashMap.size());
    }

    @Test
    public void whenCacheReachesMaxSize() {
        DefaultLRUCache<List<String>> cache = new DefaultLRUCache<>(2, Cache.DEFAULT_TIMEOUT_SECONDS);

        cache.save("user1", Arrays.asList("segment1", "segment2"));
        cache.save("user2", Arrays.asList("segment3", "segment4"));
        cache.save("user3", Arrays.asList("segment5", "segment6"));

        assertEquals(2, cache.linkedHashMap.size());

        assertEquals(Arrays.asList("segment5", "segment6"), cache.lookup("user3"));
        assertEquals(Arrays.asList("segment3", "segment4"), cache.lookup("user2"));
        assertNull(cache.lookup("user1"));
    }

    @Test
    public void whenCacheIsReset() {
        DefaultLRUCache<List<String>> cache = new DefaultLRUCache<>();
        cache.save("user1", Arrays.asList("segment1", "segment2"));
        cache.save("user2", Arrays.asList("segment3", "segment4"));
        cache.save("user3", Arrays.asList("segment5", "segment6"));

        assertEquals(Arrays.asList("segment1", "segment2"), cache.lookup("user1"));
        assertEquals(Arrays.asList("segment3", "segment4"), cache.lookup("user2"));
        assertEquals(Arrays.asList("segment5", "segment6"), cache.lookup("user3"));

        assertEquals(3, cache.linkedHashMap.size());

        cache.reset();

        assertNull(cache.lookup("user1"));
        assertNull(cache.lookup("user2"));
        assertNull(cache.lookup("user3"));

        assertEquals(0, cache.linkedHashMap.size());
    }
}
