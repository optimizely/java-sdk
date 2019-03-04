package com.optimizely.ab.notification;

import javax.annotation.Nonnull;
import java.util.Map;

public interface DecisionNotificationListenerInterface {

    /**
     * onDecision called when an activate was triggered
     *
     * @param type         - The notification type.
     * @param userId       - The userId passed into activate.
     * @param attributes   - The filtered attribute list passed into activate
     * @param decisionInfo - The decision Information containing all parameters passed in api.
     */
    void onDecision(@Nonnull String type,
                    @Nonnull String userId,
                    @Nonnull Map<String, ?> attributes,
                    @Nonnull Map<String, ?> decisionInfo);
}
