/**
 *    Copyright 2019, Optimizely Inc. and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab;

import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.ProjectConfigManager;
import com.optimizely.ab.decision.experiment.ExperimentService;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.EventProcessor;
import org.junit.rules.ExternalResource;

/**
 * Factory class for building and maintaining an Optimizely instance. The methods mirror the
 * {@link Optimizely.Builder} methods so test can can use either class interchangeably.
 *
 * The main motivation of this class is to ensure that the built Optimizely resource get's
 * explicitly closed at the end of each test run.
 */
public class OptimizelyRule extends ExternalResource {

    private Optimizely.Builder builder;
    private Optimizely optimizely;

    public OptimizelyRule withEventProcessor(EventProcessor eventProcessor) {
        builder.withEventProcessor(eventProcessor);
        return this;
    }

    public OptimizelyRule withDecisionService(DecisionService decisionService) {
        builder.withDecisionService(decisionService);
        return this;
    }

    public OptimizelyRule withDatafile(String datafile) {
        builder.withDatafile(datafile);
        return this;
    }

    public OptimizelyRule withErrorHandler(ErrorHandler errorHandler) {
        builder.withErrorHandler(errorHandler);
        return this;
    }

    public OptimizelyRule withExperimentService(ExperimentService experimentService) {
        builder.withExperimentService(experimentService);
        return this;
    }

    public OptimizelyRule withEventHandler(EventHandler eventHandler) {
        builder.withEventHandler(eventHandler);
        return this;
    }

    public OptimizelyRule withConfig(ProjectConfig projectConfig) {
        builder.withConfig(projectConfig);
        return this;
    }

    public OptimizelyRule withConfigManager(ProjectConfigManager projectConfigManager) {
        builder.withConfigManager(projectConfigManager);
        return this;
    }

    public Optimizely build() {
        optimizely = builder.build();
        return optimizely;
    }

    public void before() {
        builder = Optimizely.builder();
    }

    public void after() {
        if (optimizely == null) {
            // Build so we can shut everything down.
            build();
        }

        // Blocks and waits for graceful shutdown.
        optimizely.close();
        optimizely = null;
    }

}
