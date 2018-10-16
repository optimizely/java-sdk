/**
 *
 *    Copyright 2018, Optimizely and contributors
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
 package com.optimizely.ab.config.audience;

import com.optimizely.ab.config.ProjectConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * The AudienceIdCondition is a holder for the audience id in
 * {@link com.optimizely.ab.config.Experiment#audienceConditions auienceConditions}.  The AudienceId is later
 * resolved to Audience before evaluation.  If you do not resolve the AudienceIdCondition before evaluating, the
 * condition will fail.  AudienceIdConditions are resolved when the ProjectConfig is passed into evaluate.
 */
public class AudienceIdCondition implements Condition {
    private Audience audience;
    private String audienceId;

    private static Logger logger = LoggerFactory.getLogger("AudienceIdCondition");

    /**
     * This is for running legacy {@link com.optimizely.ab.config.Experiment#audienceIds}.  During json parsing the audienceId is the only thing available.
     * @param audience The audience to be evaluated.
     */
    public AudienceIdCondition(Audience audience) {
        this.audience = audience;
        this.audienceId = audience.getId();
    }

    /**
     * Constructor used in json parsing to store the audienceId parsed from Experiment.audienceConditions.
     * @param audienceId
     */
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

    @Nullable
    @Override
    public Boolean evaluate(ProjectConfig config, Map<String, ?> attributes) {
        if (config != null) {
            audience = config.getAudienceIdMapping().get(audienceId);
        }
        if (audience == null) {
            logger.error(String.format("Audience not set for audienceConditions %s", audienceId));
            return null;
        }
        return audience.getConditions().evaluate(config, attributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudienceIdCondition condition = (AudienceIdCondition) o;
        return ((audience == null) ? (null == condition.audience) :
                (audience.getId().equals(condition.audience!=null?condition.audience.getId():null))) &&
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
}