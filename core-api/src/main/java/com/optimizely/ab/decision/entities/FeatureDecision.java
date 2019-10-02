package com.optimizely.ab.decision.entities;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;

import javax.annotation.Nullable;

public class FeatureDecision {
    /**
     * The {@link Experiment} the Feature is associated with.
     */
    @Nullable
    public Experiment experiment;

    /**
     * The {@link Variation} the user was bucketed into.
     */
    @Nullable
    public Variation variation;

    /**
     * The source of the {@link Variation}.
     */
    @Nullable
    public DecisionSource decisionSource;

    public enum DecisionSource {
        FEATURE_TEST("feature-test"),
        ROLLOUT("rollout");

        private final String key;

        DecisionSource(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    /**
     * Initialize a FeatureDecision object.
     *
     * @param experiment     The {@link Experiment} the Feature is associated with.
     * @param variation      The {@link Variation} the user was bucketed into.
     * @param decisionSource The source of the variation.
     */
    public FeatureDecision(@Nullable Experiment experiment,
                           @Nullable Variation variation,
                           @Nullable DecisionSource decisionSource) {
        this.experiment = experiment;
        this.variation = variation;
        this.decisionSource = decisionSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeatureDecision that = (FeatureDecision) o;

        if (variation != null ? !variation.equals(that.variation) : that.variation != null)
            return false;
        return decisionSource == that.decisionSource;
    }

    @Override
    public int hashCode() {
        int result = variation != null ? variation.hashCode() : 0;
        result = 31 * result + (decisionSource != null ? decisionSource.hashCode() : 0);
        return result;
    }
}

