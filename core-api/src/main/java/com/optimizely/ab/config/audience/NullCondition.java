package com.optimizely.ab.config.audience;

import com.optimizely.ab.config.ProjectConfig;

import javax.annotation.Nullable;
import java.util.Map;

public class NullCondition implements Condition {
    @Nullable
    @Override
    public Boolean evaluate(ProjectConfig config, Map<String, ?> attributes) {
        return null;
    }
}
