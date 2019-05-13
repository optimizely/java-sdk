/**
 *
 *    Copyright 2019, Optimizely and contributors
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
package com.optimizely.ab.notification;

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;

import java.util.Map;

/**
 * ActivateNotification supplies notification for AB activatation.
 *
 * @deprecated in favor of {@link DecisionNotification} which provides notifications for Experiment, Feature
 * and Rollout decisions.
 */
@Deprecated
public final class ActivateNotification implements Notification {

    private final Experiment experiment;
    private final String userId;
    private final Map<String, ?> attributes;
    private final Variation variation;
    private final LogEvent event;

    @VisibleForTesting
    ActivateNotification() {
        this(null, null, null, null, null);
    }

    /**
     * @param experiment - The experiment object being activated.
     * @param userId     - The userId passed into activate.
     * @param attributes - The filtered attribute list passed into activate
     * @param variation  - The variation that was returned from activate.
     * @param event      - The impression event that was triggered.
     */
    public ActivateNotification(Experiment experiment, String userId, Map<String, ?> attributes, Variation variation, LogEvent event) {
        this.experiment = experiment;
        this.userId = userId;
        this.attributes = attributes;
        this.variation = variation;
        this.event = event;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public Variation getVariation() {
        return variation;
    }

    public LogEvent getEvent() {
        return event;
    }


}
