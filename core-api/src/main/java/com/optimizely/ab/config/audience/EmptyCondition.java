package com.optimizely.ab.config.audience;

import com.optimizely.ab.config.ProjectConfig;

import javax.annotation.Nullable;
import java.util.Map;

public class EmptyCondition<T> implements Condition<T> {
    @Nullable
    @Override
    public Boolean evaluate(ProjectConfig config, Map<String, ?> attributes) {
        return true;
    }

}
