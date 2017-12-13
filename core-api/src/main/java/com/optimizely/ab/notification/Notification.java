package com.optimizely.ab.notification;

import javax.annotation.Nonnull;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import java.util.Map;

public interface Notification {

    public void notify(Object ...args);
}
