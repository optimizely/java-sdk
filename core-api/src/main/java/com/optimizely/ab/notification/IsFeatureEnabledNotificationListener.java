package com.optimizely.ab.notification;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public abstract class IsFeatureEnabledNotificationListener implements NotificationListener, IsFeatureEnabledNotificationListenerInterface {

    @Override
    public void notify(Object... args) {
        assert (args[0] instanceof String);
        String featureKey = (String) args[0];
        assert (args[1] instanceof String);
        String userId = (String) args[1];
        Map<String, ?> attributes = null;
        if (args[2] != null) {
            assert (args[2] instanceof java.util.Map);
            attributes = (Map<String, ?>) args[2];
        }
        Map<String, ?> featureInfo = null;
        if (args[3] != null) {
            assert (args[3] instanceof java.util.Map);
            featureInfo = (HashMap<String, ?>) args[3];
        }

        onIsFeatureEnabled(featureKey, userId, attributes, featureInfo);
    }

    @Override
    public abstract void onIsFeatureEnabled(@Nonnull String featureKey,
                                            @Nonnull String userId,
                                            @Nonnull Map<String, ?> attributes,
                                            @Nonnull Map<String, ?> featureInfo);

}
