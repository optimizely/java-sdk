package com.optimizely.ab.decision;

import com.optimizely.ab.config.ProjectConfig;

import java.util.Map;

public interface DecisionService<T, V> {
    V getDecision(T entity, String userId, Map<String, ?> attributes, ProjectConfig projectConfig);
}
