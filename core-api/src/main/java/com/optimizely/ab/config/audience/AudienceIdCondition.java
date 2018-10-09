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

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

public class AudienceIdCondition implements Condition {
    private Audience audience;
    private String audienceId;

    public AudienceIdCondition(Audience audience) {
        this.audience = audience;
    }

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
    public Boolean evaluate(Map<String, ?> attributes) {
        if (audience == null) {
            // throw audience not found exception?
            return null;
        }
        return audience.getConditions().evaluate(attributes);
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
}
