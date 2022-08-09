package com.optimizely.ab.config.audience;

import java.util.List;

public abstract class LeafCondition<T> implements Condition<T> {

    @Override
    public List<Condition> getConditions() {
        return null;
    }
}
