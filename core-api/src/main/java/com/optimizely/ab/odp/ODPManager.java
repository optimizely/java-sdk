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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public class ODPManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ODPManager.class);

    private volatile ODPConfig odpConfig;
    private final ODPSegmentManager segmentManager;
    private final ODPEventManager eventManager;

    private ODPManager(@Nonnull ODPApiManager apiManager) {
        this(new ODPSegmentManager(apiManager), new ODPEventManager(apiManager));
    }

    private ODPManager(@Nonnull ODPSegmentManager segmentManager, @Nonnull ODPEventManager eventManager) {
        this.segmentManager = segmentManager;
        this.eventManager = eventManager;
        this.eventManager.start();
    }

    public ODPSegmentManager getSegmentManager() {
        return segmentManager;
    }

    public ODPEventManager getEventManager() {
        return eventManager;
    }

    public Boolean updateSettings(String apiHost, String apiKey, Set<String> allSegments) {
        ODPConfig newConfig = new ODPConfig(apiKey, apiHost, allSegments);
        if (odpConfig == null || !odpConfig.equals(newConfig)) {
            logger.debug("Updating ODP Config");
            odpConfig = newConfig;
            eventManager.updateSettings(odpConfig);
            segmentManager.resetCache();
            segmentManager.updateSettings(odpConfig);
            return true;
        }
        return false;
    }

    public void close() {
        eventManager.stop();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ODPSegmentManager segmentManager;
        private ODPEventManager eventManager;
        private ODPApiManager apiManager;
        private Integer cacheSize;
        private Integer cacheTimeoutSeconds;
        private Integer batchSize;
        private Integer flushIntervalMillis;
        private Cache<List<String>> cacheImpl;

        public Builder withApiManager(ODPApiManager apiManager) {
            this.apiManager = apiManager;
            return this;
        }

        public Builder withSegmentManager(ODPSegmentManager segmentManager) {
            this.segmentManager = segmentManager;
            return this;
        }

        public Builder withEventManager(ODPEventManager eventManager) {
            this.eventManager = eventManager;
            return this;
        }

        public Builder withSegmentCacheSize(Integer cacheSize) {
            this.cacheSize = cacheSize;
            return this;
        }

        public Builder withSegmentCacheTimeout(Integer cacheTimeoutSeconds) {
            this.cacheTimeoutSeconds = cacheTimeoutSeconds;
            return this;
        }

        public Builder withSegmentCache(Cache<List<String>> cacheImpl) {
            this.cacheImpl = cacheImpl;
            return this;
        }

        public Builder withEventBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder withEventFlushInterval(Integer flushIntervalMillis) {
            this.flushIntervalMillis = flushIntervalMillis;
            return this;
        }

        public ODPManager build() {
            if ((segmentManager == null || eventManager == null) && apiManager == null) {
                logger.warn("ApiManager instance is needed when using default EventManager or SegmentManager");
                return null;
            }

            if (segmentManager == null) {
                if (cacheImpl != null) {
                    segmentManager = new ODPSegmentManager(apiManager, cacheImpl);
                } else if (cacheSize != null || cacheTimeoutSeconds != null) {
                    // Converting null to -1 so that DefaultCache uses the default values;
                    if (cacheSize == null) {
                        cacheSize = -1;
                    }
                    if (cacheTimeoutSeconds == null) {
                        cacheTimeoutSeconds = -1;
                    }
                    segmentManager = new ODPSegmentManager(apiManager, cacheSize, cacheTimeoutSeconds);
                } else {
                    segmentManager = new ODPSegmentManager(apiManager);
                }
            }

            if (eventManager == null) {
                eventManager = new ODPEventManager(apiManager, batchSize, null, flushIntervalMillis);
            }

            return new ODPManager(segmentManager, eventManager);
        }
    }
}
