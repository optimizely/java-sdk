/**
 *
 *    Copyright 2018-2022, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
 package com.optimizely.ab.config.audience;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.internal.InvalidAudienceCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;

/**
 * The AudienceIdCondition is a holder for the audience id in
 * {@link com.optimizely.ab.config.Experiment#audienceConditions auienceConditions}.
 * If the audienceId is not resolved at evaluation time, the
 * condition will fail.  AudienceIdConditions are resolved when the ProjectConfig is passed into evaluate.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AudienceIdCondition<T> implements Condition<T> {
    private Audience audience;
    final private String audienceId;

    final private static Logger logger = LoggerFactory.getLogger(AudienceIdCondition.class);

    /**
     * Constructor used in json parsing to store the audienceId parsed from Experiment.audienceConditions.
     *
     * @param audienceId The audience id
     */
    @JsonCreator
    public AudienceIdCondition(String audienceId) {
        this.audienceId = audienceId;
    }

    public Audience getAudience() {
        return audience;
    }

    public void setAudience(Audience audience) {
        this.audience = audience;
    }

    public String getAudienceId() {
        return audienceId;
    }

    @Override
    public String getOperandOrId() {
        return audienceId;
    }

    @Nullable
    @Override
    public Boolean evaluate(ProjectConfig config, OptimizelyUserContext user) {
        if (config != null) {
            audience = config.getAudienceIdMapping().get(audienceId);
        }
        if (audience == null) {
            logger.error("Audience {} could not be found.", audienceId);
            return null;
        }
        logger.debug("Starting to evaluate audience \"{}\" with conditions: {}.", audience.getId(), audience.getConditions());
        Boolean result = audience.getConditions().evaluate(config, user);
        logger.debug("Audience \"{}\" evaluated to {}.", audience.getId(), result);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudienceIdCondition condition = (AudienceIdCondition) o;
        return ((audience == null) ? (null == condition.audience) :
            (audience.getId().equals(condition.audience != null ? condition.audience.getId() : null))) &&
            ((audienceId == null) ? (null == condition.audienceId) :
                (audienceId.equals(condition.audienceId)));
    }

    @Override
    public int hashCode() {

        return Objects.hash(audience, audienceId);
    }

    @Override
    public String toString() {
        return audienceId;
    }

    @Override
    public String toJson() { return null; }
}
