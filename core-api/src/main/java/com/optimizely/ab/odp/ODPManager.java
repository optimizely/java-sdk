package com.optimizely.ab.odp;

import javax.annotation.Nonnull;
import java.util.List;

public class ODPManager {
    private volatile ODPConfig odpConfig;
    private final ODPSegmentManager segmentManager;
    private final ODPEventManager eventManager;

    public ODPManager(@Nonnull ODPConfig odpConfig, @Nonnull ODPApiManager apiManager) {
        this(odpConfig, new ODPSegmentManager(odpConfig, apiManager), new ODPEventManager(odpConfig, apiManager));
    }

    public ODPManager(@Nonnull ODPConfig odpConfig, @Nonnull ODPSegmentManager segmentManager, @Nonnull ODPEventManager eventManager) {
        this.odpConfig = odpConfig;
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

    public Boolean updateSettings(String apiHost, String apiKey, List<String> allSegments) {
        ODPConfig newConfig = new ODPConfig(apiKey, apiHost, allSegments);
        if (!odpConfig.equals(newConfig)) {
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
}
