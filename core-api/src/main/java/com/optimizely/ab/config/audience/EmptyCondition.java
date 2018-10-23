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

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EmptyCondition) return true;
        return false;
    }

}
