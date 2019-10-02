package com.optimizely.ab.decision.feature;

import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.decision.audience.FullStackAudienceEvaluator;
import com.optimizely.ab.decision.bucketer.MurmurhashBucketer;
import com.optimizely.ab.decision.entities.DecisionStatus;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.entities.FeatureDecision;
import com.optimizely.ab.decision.entities.Reason;
import com.optimizely.ab.decision.experiment.CompositeExperimentService;
import com.optimizely.ab.decision.experiment.ExperimentDecisionService;
import com.optimizely.ab.decision.experiment.service.ExperimentBucketerService;
import com.optimizely.ab.decision.experiment.service.ForcedVariationService;
import com.optimizely.ab.decision.experiment.service.UserProfileDecisionService;
import com.optimizely.ab.decision.experiment.service.WhitelistingService;
import com.optimizely.ab.decision.feature.service.FeatureExperimentService;
import com.optimizely.ab.decision.feature.service.FeatureRolloutService;
import com.optimizely.ab.event.internal.UserContext;
import com.optimizely.ab.internal.ExperimentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CompositeFeatureService implements FeatureDecisionService {
    private static final Logger logger = LoggerFactory.getLogger(CompositeExperimentService.class);

    private final UserProfileService userProfileService;
    private transient ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping;

    /**
     * Initialize composite experiment service for decision service
     *
     * @param userProfileService     UserProfileService implementation for storing user info.
     * @param forcedVariationMapping Forced Variation for user if exists
     */
    public CompositeFeatureService(@Nullable UserProfileService userProfileService,
                                      @Nullable ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping) {
        this.userProfileService = userProfileService;
        this.forcedVariationMapping = forcedVariationMapping;
    }

    @Override
    public FeatureDecision getDecision(@Nonnull FeatureFlag featureFlag, @Nonnull UserContext userContext) {
        FeatureDecision featureDecision;
        // loop through different experiment decision services until we get a decision
        for (FeatureDecisionService featureDecisionService : getFeatureServices()) {
            featureDecision = featureDecisionService.getDecision(featureFlag, userContext);
            if (featureDecision != null)
                return featureDecision;
        }

        return new FeatureDecision(null,
            null,
            FeatureDecision.DecisionSource.FEATURE_TEST);

    }

    /**
     * Get Experiment Services
     *
     * @return List of {@link FeatureDecisionService}
     */
    private List<FeatureDecisionService> getFeatureServices() {
        return Arrays.asList(
            new FeatureExperimentService(userProfileService, forcedVariationMapping),
            new FeatureRolloutService()
        );
    }
}
