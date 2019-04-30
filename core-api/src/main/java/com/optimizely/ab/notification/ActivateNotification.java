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

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * ActivateNotification is deprecated in favor of {@link DecisionNotification}
 */
@Deprecated
public class ActivateNotification implements Notification {

    // Experiment being activated.
    private final Experiment experiment;
    // UserId that triggered the event.
    private final String userId;
    // User attributes associated with the user. Included in the event to enable
    // results segmentation and advanced targeting.
    private final Map<String, ?> attributes;
    // Variation being activated.
    private final Variation variation;
    // LogEvent to be sent to Optimizely log endpoint.
    private final LogEvent event;

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
