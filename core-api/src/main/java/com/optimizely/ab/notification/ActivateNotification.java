package com.optimizely.ab.notification;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import sun.rmi.runtime.Log;

import java.util.Map;


public abstract class ActivateNotification implements Notification {

    @Override
    public void notify(Object... args) {
        assert(args[0] instanceof Experiment);
        Experiment experiment = (Experiment) args[0];
        assert(args[1] instanceof String);
        String userId = (String) args[1];
        assert(args[2] instanceof java.util.Map);
        Map<String, String> attributes = (Map<String, String>) args[2];
        assert(args[3] instanceof Variation);
        Variation variation = (Variation) args[3];
        assert(args[4] instanceof LogEvent);
        LogEvent logEvent = (LogEvent) args[4];

        onActivate(experiment, userId, attributes, variation, logEvent);
    }

    // Notice the argument list for decision
    abstract void onActivate(@javax.annotation.Nonnull Experiment experiment,
                             @javax.annotation.Nonnull String userId,
                             @javax.annotation.Nonnull Map<String, String> attributes,
                             @javax.annotation.Nonnull Variation variation,
                             @javax.annotation.Nonnull LogEvent event) ;

}

