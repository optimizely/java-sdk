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

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.internal.Cache;
import com.optimizely.ab.internal.DefaultLRUCache;
import com.optimizely.ab.odp.parser.ResponseJsonParser;
import com.optimizely.ab.odp.parser.ResponseJsonParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class ODPSegmentManager {

    private static final Logger logger = LoggerFactory.getLogger(ODPSegmentManager.class);

    private static final String SEGMENT_URL_PATH = "/v3/graphql";
    @VisibleForTesting
    public final ODPApiManager apiManager;

    private volatile ODPConfig odpConfig;

    private final Cache<List<String>> segmentsCache;

    public ODPSegmentManager(ODPApiManager apiManager) {
        this(apiManager, Cache.DEFAULT_MAX_SIZE, Cache.DEFAULT_TIMEOUT_SECONDS);
    }

    public ODPSegmentManager(ODPApiManager apiManager, Cache<List<String>> cache) {
        this.apiManager = apiManager;
        this.segmentsCache = cache;
    }

    public ODPSegmentManager(ODPApiManager apiManager, Integer cacheSize, Integer cacheTimeoutSeconds) {
        this.apiManager = apiManager;
        this.segmentsCache = new DefaultLRUCache<>(cacheSize, cacheTimeoutSeconds);
    }

    public List<String> getQualifiedSegments(String userId) {
        return getQualifiedSegments(userId, Collections.emptyList());
    }
    public List<String> getQualifiedSegments(String userId, List<ODPSegmentOption> options) {
        if (ODPManager.isVuid(userId)) {
            return getQualifiedSegments(ODPUserKey.VUID, userId, options);
        } else {
            return getQualifiedSegments(ODPUserKey.FS_USER_ID, userId, options);
        }
    }

    public List<String> getQualifiedSegments(ODPUserKey userKey, String userValue) {
        return getQualifiedSegments(userKey, userValue, Collections.emptyList());
    }

    public List<String> getQualifiedSegments(ODPUserKey userKey, String userValue, List<ODPSegmentOption> options) {
        if (odpConfig == null || !odpConfig.isReady()) {
            logger.error("Audience segments fetch failed (ODP is not enabled)");
            return null;
        }

        if (!odpConfig.hasSegments()) {
            logger.debug("No Segments are used in the project, Not Fetching segments. Returning empty list");
            return Collections.emptyList();
        }

        List<String> qualifiedSegments;
        String cacheKey = getCacheKey(userKey.getKeyString(), userValue);

        if (options.contains(ODPSegmentOption.RESET_CACHE)) {
            segmentsCache.reset();
        } else if (!options.contains(ODPSegmentOption.IGNORE_CACHE)) {
            qualifiedSegments = segmentsCache.lookup(cacheKey);
            if (qualifiedSegments != null) {
                logger.debug("ODP Cache Hit. Returning segments from Cache.");
                return qualifiedSegments;
            }
        }

        logger.debug("ODP Cache Miss. Making a call to ODP Server.");

        qualifiedSegments = apiManager.fetchQualifiedSegments(odpConfig.getApiKey(), odpConfig.getApiHost() + SEGMENT_URL_PATH, userKey.getKeyString(), userValue, odpConfig.getAllSegments());
        if (qualifiedSegments != null && !options.contains(ODPSegmentOption.IGNORE_CACHE)) {
            segmentsCache.save(cacheKey, qualifiedSegments);
        }

        return qualifiedSegments;
    }

    public void getQualifiedSegments(ODPUserKey userKey, String userValue, ODPSegmentFetchCallback callback, List<ODPSegmentOption> options) {
        AsyncSegmentFetcher segmentFetcher = new AsyncSegmentFetcher(userKey, userValue, options, callback);
        segmentFetcher.start();
    }

    public void getQualifiedSegments(ODPUserKey userKey, String userValue, ODPSegmentFetchCallback callback) {
        getQualifiedSegments(userKey, userValue, callback, Collections.emptyList());
    }

    public void getQualifiedSegments(String userId, ODPSegmentFetchCallback callback, List<ODPSegmentOption> options) {
        if (ODPManager.isVuid(userId)) {
            getQualifiedSegments(ODPUserKey.VUID, userId, callback, options);
        } else {
            getQualifiedSegments(ODPUserKey.FS_USER_ID, userId, callback, options);
        }
    }

    public void getQualifiedSegments(String userId, ODPSegmentFetchCallback callback) {
        getQualifiedSegments(userId, callback, Collections.emptyList());
    }

    private String getCacheKey(String userKey, String userValue) {
        return userKey + "-$-" + userValue;
    }

    public void updateSettings(ODPConfig odpConfig) {
        this.odpConfig = odpConfig;
    }

    public void resetCache() {
        segmentsCache.reset();
    }

    @FunctionalInterface
    public interface ODPSegmentFetchCallback {
        void onCompleted(List<String> segments);
    }

    private class AsyncSegmentFetcher extends Thread {

        private final ODPUserKey userKey;
        private final String userValue;
        private final List<ODPSegmentOption> segmentOptions;
        private final ODPSegmentFetchCallback callback;

        public AsyncSegmentFetcher(ODPUserKey userKey, String userValue, List<ODPSegmentOption> segmentOptions, ODPSegmentFetchCallback callback) {
            this.userKey = userKey;
            this.userValue = userValue;
            this.segmentOptions = segmentOptions;
            this.callback = callback;
        }

        @Override
        public void run() {
            List<String> segments = getQualifiedSegments(userKey, userValue, segmentOptions);
            callback.onCompleted(segments);
        }
    }
}
