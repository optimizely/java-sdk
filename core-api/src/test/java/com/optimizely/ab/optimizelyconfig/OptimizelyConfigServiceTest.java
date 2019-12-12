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
package com.optimizely.ab.optimizelyconfig;

import com.optimizely.ab.config.*;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.*;
import static org.junit.Assert.*;

public class OptimizelyConfigServiceTest {

    private OptimizelyConfig optimizelyConfig;
    private ProjectConfig projectConfig;

    @Before
    public void initialize()throws Exception {
        projectConfig = new DatafileProjectConfig.Builder().withDatafile(validConfigJsonV4()).build();
        optimizelyConfig = new OptimizelyConfigService(projectConfig).getOptimizelyConfig();
    }

    @Test
    public void shouldReturnAllExperimentsExceptRollouts() throws Exception {
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = optimizelyConfig.getExperimentsMap();
        assertEquals(optimizelyExperimentMap.size(), 13);

        List<Experiment> experiments = getAllExperimentsFromDatafile();
        experiments.forEach(experiment -> {
            OptimizelyExperiment optimizelyExperiment = optimizelyExperimentMap.get(experiment.getKey());
            assertEquals(optimizelyExperiment.getId(), experiment.getId());
            assertEquals(optimizelyExperiment.getKey(), experiment.getKey());

            Map<String, OptimizelyVariation> optimizelyVariationMap = optimizelyExperimentMap.get(experiment.getKey()).getVariationsMap();
            experiment.getVariations().forEach(variation -> {
            OptimizelyVariation optimizelyVariation = optimizelyVariationMap.get(variation.getKey());
                assertEquals(optimizelyVariation.getId(), variation.getId());
                assertEquals(optimizelyVariation.getKey(), variation.getKey());
            });
        });
    }

    @Test
    public void shouldReturnAllFeatureFlag() throws Exception {
        Map<String, OptimizelyFeature> optimizelyFeatureMap = optimizelyConfig.getFeaturesMap();
        assertEquals(optimizelyFeatureMap.size(), 7);

        projectConfig.getFeatureFlags().forEach(featureFlag -> {
            OptimizelyFeature optimizelyFeature = optimizelyFeatureMap.get(featureFlag.getKey());
            assertEquals(optimizelyFeature.getId(), featureFlag.getId());
            assertEquals(optimizelyFeature.getKey(), featureFlag.getKey());

            featureFlag.getExperimentIds().forEach(experimentId -> {
               String experimentKey =  projectConfig.getExperimentIdMapping().get(experimentId).getKey();
               assertNotNull(optimizelyFeatureMap.get(featureFlag.getKey()).getExperimentsMap().get(experimentKey));
            });

            Map<String, OptimizelyVariable> optimizelyVariableMap = optimizelyFeatureMap.get(featureFlag.getKey()).getVariablesMap();
            featureFlag.getVariables().forEach(variable -> {
                OptimizelyVariable optimizelyVariable = optimizelyVariableMap.get(variable.getKey());
                assertEquals(optimizelyVariable.getId(), variable.getId());
                assertEquals(optimizelyVariable.getKey(), variable.getKey());
                assertEquals(optimizelyVariable.getType(), variable.getType().getVariableType().toLowerCase());
                assertEquals(optimizelyVariable.getValue(), variable.getDefaultValue());
            });
        });
    }

    @Test
    public void shouldCorrectlyMergeAllFeatureVariables() throws Exception {
        List<FeatureFlag> featureFlags = projectConfig.getFeatureFlags();
        Map<String, Experiment> datafileExperimentsMap = new HashMap<>();
        getAllExperimentsFromDatafile().forEach(experiment ->
            datafileExperimentsMap.put(experiment.getKey(), experiment)
        );
        featureFlags.forEach(featureFlag -> {
            List<String> experimentIds = featureFlag.getExperimentIds();
            experimentIds.forEach(experimentId -> {
                String experimentKey = projectConfig.getExperimentIdMapping().get(experimentId).getKey();
                OptimizelyExperiment experiment = optimizelyConfig.getExperimentsMap().get(experimentKey);
                List<Variation> variations = datafileExperimentsMap.get(experimentKey).getVariations();
                Map<String, OptimizelyVariation> variationsMap = experiment.getVariationsMap();
                variations.forEach(variation ->
                    featureFlag.getVariables().forEach(variable -> {
                        OptimizelyVariable optimizelyVariable = variationsMap.get(variation.getKey()).getVariablesMap().get(variable.getKey());
                        assertEquals(variable.getId(), optimizelyVariable.getId());
                        assertEquals(variable.getKey(), optimizelyVariable.getKey());
                        assertEquals(variable.getType().getVariableType().toLowerCase(), optimizelyVariable.getType());
                        if (!variation.getFeatureEnabled()) {
                            assertEquals(variable.getDefaultValue(), optimizelyVariable.getValue());
                        }
                    })
                );
            });
        });
    }

    @Test
    public void shouldReturnCorrectRevision() throws Exception {
        String revision = optimizelyConfig.getRevision();
        assertEquals(revision, projectConfig.getRevision());
    }

    private List<Experiment> getAllExperimentsFromDatafile() {
        List<Experiment> experiments = new ArrayList<>();
        projectConfig.getGroups().forEach(group ->
            experiments.addAll(group.getExperiments())
        );
        experiments.addAll(projectConfig.getExperiments());
        return experiments;
    }
}
