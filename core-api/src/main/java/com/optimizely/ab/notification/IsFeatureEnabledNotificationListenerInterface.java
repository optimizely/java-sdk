package com.optimizely.ab.notification;

import javax.annotation.Nonnull;
import java.util.Map;

public interface IsFeatureEnabledNotificationListenerInterface {
    /**
     * onIsFeatureEnabled is called when a IsFeatureEnabled event is triggered
     *
     * @param featureKey   - The unique key of the feature.
     * @param userId     - user id passed into track.
     * @param attributes - filtered attributes list after passed into track
     * @param featureInfo  - map consisting of 3 keys: enabled, source and event. Enabled denotes whether or not feature was enabled for the user, source denoting whether feature was accessed through EXPERIMENT or ROLLOUT and the event object if an event was sent to the Optimizely backend as a result of the feature access
     */
    void onIsFeatureEnabled(@Nonnull String featureKey,
                                 @Nonnull String userId,
                                 @Nonnull Map<String, ?> attributes,
                                 @Nonnull Map<String, ?> featureInfo);
}
