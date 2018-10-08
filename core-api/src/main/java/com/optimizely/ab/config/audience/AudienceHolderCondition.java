package com.optimizely.ab.config.audience;

import javax.annotation.Nullable;
import java.util.Map;

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
}
