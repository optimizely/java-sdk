package com.optimizely.ab.decision.feature;


import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.decision.entities.FeatureDecision;
import com.optimizely.ab.event.internal.UserContext;

import javax.annotation.Nonnull;

public interface FeatureDecisionService {
    FeatureDecision getDecision(@Nonnull FeatureFlag featureFlag,
                                @Nonnull UserContext userContext);
}
