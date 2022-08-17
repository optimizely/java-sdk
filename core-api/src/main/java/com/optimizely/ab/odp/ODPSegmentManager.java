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

    private final ODPApiManager apiManager;

    private final ODPConfig odpConfig;

    private final Cache<List<String>> segmentsCache;

    public ODPSegmentManager(ODPConfig odpConfig, ODPApiManager apiManager) {
        this(odpConfig, apiManager, Cache.DEFAULT_MAX_SIZE, Cache.DEFAULT_TIMEOUT_SECONDS);
    }

    public ODPSegmentManager(ODPConfig odpConfig, ODPApiManager apiManager, Cache<List<String>> cache) {
        this.apiManager = apiManager;
        this.odpConfig = odpConfig;
        this.segmentsCache = cache;
    }

    public ODPSegmentManager(ODPConfig odpConfig, ODPApiManager apiManager, Integer cacheSize, Integer cacheTimeoutSeconds) {
        this.apiManager = apiManager;
        this.odpConfig = odpConfig;
        this.segmentsCache = new DefaultLRUCache<>(cacheSize, cacheTimeoutSeconds);
    }

    public List<String> getQualifiedSegments(String fsUserId) {
        return getQualifiedSegments(ODPUserKey.FS_USER_ID, fsUserId, Collections.emptyList());
    }
    public List<String> getQualifiedSegments(String fsUserId, List<ODPSegmentOption> options) {
        return getQualifiedSegments(ODPUserKey.FS_USER_ID, fsUserId, options);
    }

    public List<String> getQualifiedSegments(ODPUserKey userKey, String userValue) {
        return getQualifiedSegments(userKey, userValue, Collections.emptyList());
    }

    public List<String> getQualifiedSegments(ODPUserKey userKey, String userValue, List<ODPSegmentOption> options) {
        if (!odpConfig.isReady()) {
            logger.error("ODP is not enabled.");
            return Collections.emptyList();
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

        ResponseJsonParser parser = ResponseJsonParserFactory.getParser();
        String qualifiedSegmentsResponse = apiManager.fetchQualifiedSegments(odpConfig.getApiKey(), odpConfig.getApiHost() + SEGMENT_URL_PATH, userKey.getKeyString(), userValue, odpConfig.getAllSegments());
        qualifiedSegments = parser.parseQualifiedSegments(qualifiedSegmentsResponse);

        if (qualifiedSegments != null && !options.contains(ODPSegmentOption.IGNORE_CACHE)) {
            segmentsCache.save(cacheKey, qualifiedSegments);
        }

        return qualifiedSegments;
    }

    private String getCacheKey(String userKey, String userValue) {
        return userKey + "-$-" + userValue;
    }
}
