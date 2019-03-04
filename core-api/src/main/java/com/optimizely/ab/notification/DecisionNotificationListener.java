package com.optimizely.ab.notification;

import javax.annotation.Nonnull;
import java.util.Map;

public abstract class DecisionNotificationListener implements NotificationListener, DecisionNotificationListenerInterface {

    /**
     * Base notify called with var args.  This method parses the parameters and calls the abstract method.
     *
     * @param args - variable argument list based on the type of notification.
     */
    @Override
    public final void notify(Object... args) {
        assert (args[0] instanceof String);
        String type = (String) args[0];
        assert (args[1] instanceof String);
        String userId = (String) args[1];
        Map<String, ?> attributes = null;
        if (args[2] != null) {
            assert (args[2] instanceof java.util.Map);
            attributes = (Map<String, ?>) args[2];
        }
        Map<String, ?> decisionInfo = null;
        if (args[3] != null) {
            assert (args[3] instanceof java.util.Map);
            attributes = (Map<String, ?>) args[3];
        }
        onDecision(type, userId, attributes, decisionInfo);
    }

    @Override
    public abstract void onDecision(@Nonnull String type,
                                    @Nonnull String userId,
                                    @Nonnull Map<String, ?> attributes,
                                    @Nonnull Map<String, ?> decisionInfo);
}
