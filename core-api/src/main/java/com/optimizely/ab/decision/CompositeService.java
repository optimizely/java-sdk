/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/
package com.optimizely.ab.decision;

import com.optimizely.ab.bucketing.FeatureDecision;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.decision.experiment.ExperimentService;
import com.optimizely.ab.decision.experiment.services.ExperimentBucketerService;
import com.optimizely.ab.decision.experiment.services.ForcedVariationService;
import com.optimizely.ab.decision.experiment.services.UserProfileService;
import com.optimizely.ab.decision.experiment.services.WhitelistingService;
import com.optimizely.ab.decision.feature.FeatureService;
import com.optimizely.ab.decision.feature.services.FeatureRolloutService;
import com.optimizely.ab.event.internal.UserContext;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * CompositeService provides out of the box decision making for features and experiments
 */
public class CompositeService<T, V> implements Service<T, V> {

    private final List<ExperimentService> experimentServices;
    private final List<FeatureService> featureServices;

    /**
     * Initialize a decision service for the Optimizely client.
     *
     * @param experimentServices {@link ExperimentService} provided from Optimizely Client
     * @param featureServices    {@link FeatureService} provided from Optimizely Client
     */
    private CompositeService(List<ExperimentService> experimentServices,
                             List<FeatureService> featureServices) {
        this.experimentServices = Collections.unmodifiableList(experimentServices);
        this.featureServices = Collections.unmodifiableList(featureServices);
    }

    /**
     * Returns Decision for provided experiment or feature
     *
     * @param decisionContext {@link Experiment} or {@link FeatureFlag}
     * @param userContext     userContext It have user id, attributes and a reference to the current {@link ProjectConfig}
     * @return {@link Variation} or {@link FeatureDecision}
     */
    @Override
    public V getDecision(@Nonnull T decisionContext,
                         @Nonnull UserContext userContext) {
        return null;
    }

    public static <T, V> Builder<T, V> builder() {
        return new Builder<>();
    }

    /**
     * {@link CompositeService} instance builder.
     */
    public static class Builder<T, V> {
        private List<ExperimentService> experimentServices;
        private List<FeatureService> featureServices;

        /**
         * Set Experiment Services
         *
         * @param experimentServices {@link ExperimentService} provided from Optimizely Client
         * @return {@link Builder}
         */
        public Builder withExperimentServices(List<ExperimentService> experimentServices) {
            this.experimentServices = experimentServices;
            return this;
        }

        /**
         * Set Feature Services
         *
         * @param featureServices {@link FeatureService} provided from Optimizely Client
         * @return {@link Builder}
         */
        public Builder withFeatureServices(List<FeatureService> featureServices) {
            this.featureServices = featureServices;
            return this;
        }

        /**
         * Create actual object of {@link CompositeService}
         *
         * @return {@link CompositeService}
         */
        public CompositeService<T, V> build() {

            if (experimentServices == null) {
                experimentServices = Arrays.asList(
                    new ForcedVariationService(),
                    new WhitelistingService(),
                    new UserProfileService(),
                    new ExperimentBucketerService());
            }

            if (featureServices == null) {
                featureServices = Arrays.asList(new FeatureRolloutService());
            }
            return new CompositeService<>(experimentServices, featureServices);
        }
    }
}
