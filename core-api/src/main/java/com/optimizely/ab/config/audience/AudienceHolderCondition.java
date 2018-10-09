package com.optimizely.ab.config.audience;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

public class AudienceHolderCondition implements Condition {
    private Audience audience;
    private String audienceId;

    public AudienceHolderCondition(Audience audience) {
        this.audience = audience;
    }

    public AudienceHolderCondition(String audienceId) {
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
        AudienceHolderCondition condition = (AudienceHolderCondition) o;
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
