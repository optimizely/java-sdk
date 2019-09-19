package com.optimizely.ab.decision;

import com.optimizely.ab.config.ProjectConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CompositeDecisionService<T, V> implements DecisionService<T, V> {

    private static final Logger logger = LoggerFactory.getLogger(CompositeDecisionService.class);

    private final List<DecisionService<T, V>> decisionServices;

    private CompositeDecisionService(List<DecisionService<T, V>> decisionServices) {
        this.decisionServices = Collections.unmodifiableList(decisionServices);
    }

    @Override
    public V getDecision(T entity, String userId, Map<String, ?> attributes, ProjectConfig projectConfig) {
        for (DecisionService<T, V> decisionService: decisionServices) {
            V decision = decisionService.getDecision(entity, userId, attributes, projectConfig);
            if (decision != null) {
                logger.info("decision made");
                return decision;
            }

            logger.debug("trying next decision");
        }

        logger.info("decision NOT made");
        return null;
    }

    public static <T, V> Builder<T, V> builder() {
        return new Builder<>();
    }

    public static class Builder<T, V> {
        private List<DecisionService<T, V>> decisionServices = new ArrayList<>();

        private Builder() {}

        public Builder addDecisionService(DecisionService<T, V> decisionService) {
            decisionServices.add(decisionService);
            return this;
        }

        public CompositeDecisionService<T, V> build() {
            return new CompositeDecisionService<>(decisionServices);
        }
    }
}
