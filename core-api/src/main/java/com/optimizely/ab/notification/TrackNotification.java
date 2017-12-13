package com.optimizely.ab.notification;

import java.util.Map;
import javax.annotation.Nonnull;

import com.optimizely.ab.event.LogEvent;

public abstract class TrackNotification implements Notification {

    @Override
    public final void notify(Object... args) {
        assert(args[0] instanceof String);
        String eventKey = (String) args[0];
        assert(args[1] instanceof String);
        String userId = (String) args[1];
        assert(args[2] instanceof java.util.Map);
        Map<String, String> attributes = (Map<String, String>) args[2];
        assert(args[3] instanceof java.util.Map);
        Map<String, ?> eventTags = (Map<String, ?>) args[3];
        assert(args[4] instanceof LogEvent);
        LogEvent logEvent = (LogEvent) args[4];

        onTrack(eventKey, userId,attributes,eventTags, logEvent);
    }

    // Notice the argument list for decision
    public abstract void onTrack(@Nonnull String eventKey,
                          @Nonnull String userId,
                          @Nonnull Map<String, String> attributes,
                          @Nonnull Map<String, ?>  eventTags,
                          @Nonnull LogEvent event) ;
}
