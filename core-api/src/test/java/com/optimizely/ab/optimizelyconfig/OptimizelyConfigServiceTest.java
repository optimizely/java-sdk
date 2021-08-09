/****************************************************************************
 * Copyright 2020-2021, Optimizely, Inc. and contributors                        *
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
import com.optimizely.ab.config.audience.Audience;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class OptimizelyConfigServiceTest {

    private ProjectConfig projectConfig;
    private OptimizelyConfigService optimizelyConfigService;
    private OptimizelyConfig expectedConfig;

    @Before
    public void initialize() {
        projectConfig = generateOptimizelyConfig();
        optimizelyConfigService = new OptimizelyConfigService(projectConfig);
        expectedConfig = getExpectedConfig();
    }

    @Test
    public void testGetExperimentsMap() {
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = optimizelyConfigService.getExperimentsMap();
        assertEquals(optimizelyExperimentMap.size(), 2);
        assertEquals(expectedConfig.getExperimentsMap(), optimizelyExperimentMap);
    }

    @Test
    public void testRevision() {
        String revision = optimizelyConfigService.getConfig().getRevision();
        assertEquals(expectedConfig.getRevision(), revision);
    }

    @Test
    public void testSdkKey() {
        String sdkKey = optimizelyConfigService.getConfig().getSdkKey();
        assertEquals(expectedConfig.getSdkKey(), sdkKey);
    }

    @Test
    public void testEnvironmentKey() {
        String environmentKey = optimizelyConfigService.getConfig().getEnvironmentKey();
        assertEquals(expectedConfig.getEnvironmentKey(), environmentKey);
    }

    @Test
    public void testGetFeaturesMap() {
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = optimizelyConfigService.getExperimentsMap();
        Map<String, OptimizelyFeature> optimizelyFeatureMap = optimizelyConfigService.getFeaturesMap(optimizelyExperimentMap);
        assertEquals(2, optimizelyFeatureMap.size());
        assertEquals(expectedConfig.getFeaturesMap(), optimizelyFeatureMap);
    }

    @Test
    public void testGetFeatureVariablesMap() {
        FeatureFlag featureFlag = projectConfig.getFeatureFlags().get(1);
        Map<String, OptimizelyVariable> optimizelyVariableMap =
            optimizelyConfigService.getFeatureVariablesMap(featureFlag.getVariables());
        Map<String, OptimizelyVariable> expectedVariablesMap =
            expectedConfig.getFeaturesMap().get("multi_variate_feature").getVariablesMap();
        assertEquals(expectedVariablesMap.size(), optimizelyVariableMap.size());
        assertEquals(expectedVariablesMap, optimizelyVariableMap);
    }

    @Test
    public void testGetExperimentsMapForFeature() {
        List<String> experimentIds = projectConfig.getFeatureFlags().get(1).getExperimentIds();
        Map<String, OptimizelyExperiment> optimizelyFeatureExperimentMap =
            optimizelyConfigService.getExperimentsMapForFeature(experimentIds, optimizelyConfigService.getExperimentsMap());
        assertEquals(expectedConfig.getFeaturesMap().get("multi_variate_feature").getExperimentsMap().size(), optimizelyFeatureExperimentMap.size());
    }

    @Test
    public void testGetFeatureVariableUsageInstanceMap() {
        List<FeatureVariableUsageInstance> featureVariableUsageInstances =
            projectConfig.getExperiments().get(1).getVariations().get(1).getFeatureVariableUsageInstances();
        Map<String, OptimizelyVariable> optimizelyVariableMap =
            optimizelyConfigService.getFeatureVariableUsageInstanceMap(featureVariableUsageInstances);
        Map<String, OptimizelyVariable> expectedOptimizelyVariableMap = new HashMap<String, OptimizelyVariable>() {{
            put(
                "675244127",
                new OptimizelyVariable(
                    "675244127",
                    null,
                    null,
                    "F"
                )
            );
            put(
                "4052219963",
                new OptimizelyVariable(
                    "4052219963",
                    null,
                    null,
                    "eorge"
                )
            );
        }};
        assertEquals(expectedOptimizelyVariableMap.size(), optimizelyVariableMap.size());
        assertEquals(expectedOptimizelyVariableMap, optimizelyVariableMap);
    }

    @Test
    public void testGetVariationsMap() {
        Map<String, OptimizelyVariation> optimizelyVariationMap =
            optimizelyConfigService.getVariationsMap(projectConfig.getExperiments().get(1).getVariations(), "3262035800", null);
        assertEquals(expectedConfig.getExperimentsMap().get("multivariate_experiment").getVariationsMap().size(), optimizelyVariationMap.size());
        assertEquals(expectedConfig.getExperimentsMap().get("multivariate_experiment").getVariationsMap(), optimizelyVariationMap);
    }

    @Test
    public void testGetExperimentFeatureKey() {
        String featureKey = optimizelyConfigService.getExperimentFeatureKey("3262035800");
        assertEquals("multi_variate_feature", featureKey);
    }

    @Test
    public void testGenerateFeatureKeyToVariablesMap() {
        Map<String, List<FeatureVariable>> featureKeyToVariableMap = optimizelyConfigService.generateFeatureKeyToVariablesMap();
        FeatureVariable featureVariable = featureKeyToVariableMap.get("multi_variate_feature").get(0);
        OptimizelyVariable expectedOptimizelyVariable = expectedConfig.getFeaturesMap().get("multi_variate_feature").getVariablesMap().get("first_letter");
        assertEquals(expectedOptimizelyVariable.getId(), featureVariable.getId());
        assertEquals(expectedOptimizelyVariable.getValue(), featureVariable.getDefaultValue());
        assertEquals(expectedOptimizelyVariable.getKey(), featureVariable.getKey());
        assertEquals(expectedOptimizelyVariable.getType(), featureVariable.getType());
    }

    @Test
    public void testGetMergedVariablesMap() {
        Variation variation = projectConfig.getExperiments().get(1).getVariations().get(1);
        Map<String, OptimizelyVariable> optimizelyVariableMap = optimizelyConfigService.getMergedVariablesMap(variation, "3262035800", null);
        Map<String, OptimizelyVariable> expectedOptimizelyVariableMap =
            expectedConfig.getExperimentsMap().get("multivariate_experiment").getVariationsMap().get("Feorge").getVariablesMap();
        assertEquals(expectedOptimizelyVariableMap.size(), optimizelyVariableMap.size());
        assertEquals(expectedOptimizelyVariableMap, optimizelyVariableMap);
    }

    @Test
    public void testGetAudiencesMap() {
        Map<String, String> actualAudiencesMap = optimizelyConfigService.getAudiencesMap(
            asList(
                new OptimizelyAudience(
                    "123456",
                    "test_audience_1",
                    "[\"and\", [\"or\", \"1\", \"2\"], \"3\"]"
                )
            )
        );

        Map<String, String> expectedAudiencesMap = optimizelyConfigService.getAudiencesMap(expectedConfig.getAudiences());

        assertEquals(expectedAudiencesMap, actualAudiencesMap);
    }

    private ProjectConfig generateOptimizelyConfig() {
        return new DatafileProjectConfig(
            "2360254204",
            true,
            true,
            true,
            "3918735994",
            "1480511547",
            "ValidProjectConfigV4",
            "production",
            "4",
            asList(
                new Attribute(
                    "553339214",
                    "house"
                ),
                new Attribute(
                    "58339410",
                    "nationality"
                )
            ),
            Collections.<Audience>emptyList(),
            Collections.<Audience>emptyList(),
            asList(
                new EventType(
                  "3785620495",
                    "basic_event",
                    asList("1323241596", "2738374745", "3042640549", "3262035800", "3072915611")
                ),
                new EventType(
                    "3195631717",
                    "event_with_paused_experiment",
                    asList("2667098701")
                )
            ),
            asList(
                new Experiment(
                    "1323241596",
                    "basic_experiment",
                    "Running",
                    "1630555626",
                    Collections.<String>emptyList(),
                    null,
                    asList(
                        new Variation(
                            "1423767502",
                            "A",
                            Collections.<FeatureVariableUsageInstance>emptyList()
                        ),
                        new Variation(
                            "3433458314",
                            "B",
                            Collections.<FeatureVariableUsageInstance>emptyList()
                        )
                    ),
                    Collections.singletonMap("Harry Potter", "A"),
                    asList(
                        new TrafficAllocation(
                            "1423767502",
                            5000
                        ),
                        new TrafficAllocation(
                            "3433458314",
                            10000
                        )
                    )
                ),
                new Experiment(
                    "3262035800",
                    "multivariate_experiment",
                    "Running",
                    "3262035800",
                    asList("3468206642"),
                    null,
                    asList(
                        new Variation(
                            "1880281238",
                            "Fred",
                            true,
                            asList(
                                new FeatureVariableUsageInstance(
                                    "675244127",
                                    "F"
                                ),
                                new FeatureVariableUsageInstance(
                                    "4052219963",
                                    "red"
                                )
                            )
                        ),
                        new Variation(
                            "3631049532",
                            "Feorge",
                            true,
                            asList(
                                new FeatureVariableUsageInstance(
                                    "675244127",
                                    "F"
                                ),
                                new FeatureVariableUsageInstance(
                                    "4052219963",
                                    "eorge"
                                )
                            )
                        )
                    ),
                    Collections.singletonMap("Fred", "Fred"),
                    asList(
                        new TrafficAllocation(
                            "1880281238",
                            2500
                        ),
                        new TrafficAllocation(
                            "3631049532",
                            5000
                        ),
                        new TrafficAllocation(
                            "4204375027",
                            7500
                        ),
                        new TrafficAllocation(
                            "2099211198",
                            10000
                        )
                    )
                )
            ),
            asList(
                new FeatureFlag(
                    "4195505407",
                    "boolean_feature",
                    "",
                    Collections.<String>emptyList(),
                    Collections.<FeatureVariable>emptyList()
                ),
                new FeatureFlag(
                    "3263342226",
                    "multi_variate_feature",
                    "813411034",
                    asList("3262035800"),
                    asList(
                        new FeatureVariable(
                            "675244127",
                            "first_letter",
                            "H",
                            FeatureVariable.VariableStatus.ACTIVE,
                            FeatureVariable.STRING_TYPE,
                            null
                        ),
                        new FeatureVariable(
                            "4052219963",
                            "rest_of_name",
                            "arry",
                            FeatureVariable.VariableStatus.ACTIVE,
                            FeatureVariable.STRING_TYPE,
                            null
                        )
                    )
                )
            ),
            Collections.<Group>emptyList(),
            Collections.<Rollout>emptyList()
        );
    }

    OptimizelyConfig getExpectedConfig() {
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = new HashMap<>();
        optimizelyExperimentMap.put(
            "multivariate_experiment",
            new OptimizelyExperiment(
                "3262035800",
                "multivariate_experiment",
                new HashMap<String, OptimizelyVariation>() {{
                        put(
                            "Feorge",
                            new OptimizelyVariation(
                                "3631049532",
                                "Feorge",
                                true,
                                new HashMap<String, OptimizelyVariable>() {{
                                    put(
                                        "first_letter",
                                        new OptimizelyVariable(
                                            "675244127",
                                            "first_letter",
                                            "string",
                                            "F"
                                        )
                                    );
                                    put(
                                        "rest_of_name",
                                        new OptimizelyVariable(
                                            "4052219963",
                                            "rest_of_name",
                                            "string",
                                            "eorge"
                                        )
                                    );
                                }}
                            )
                        );
                        put(
                            "Fred",
                            new OptimizelyVariation(
                                "1880281238",
                                "Fred",
                                true,
                                new HashMap<String, OptimizelyVariable>() {{
                                    put(
                                        "first_letter",
                                        new OptimizelyVariable(
                                            "675244127",
                                            "first_letter",
                                            "string",
                                            "F"
                                        )
                                    );
                                    put(
                                        "rest_of_name",
                                        new OptimizelyVariable(
                                            "4052219963",
                                            "rest_of_name",
                                            "string",
                                            "red"
                                        )
                                    );
                                }}
                            )
                        );
                }},
                ""
            )
        );
        optimizelyExperimentMap.put(
            "basic_experiment",
            new OptimizelyExperiment(
                "1323241596",
                "basic_experiment",
                new HashMap<String, OptimizelyVariation>() {{
                    put(
                        "A",
                        new OptimizelyVariation(
                            "1423767502",
                            "A",
                            null,
                            Collections.emptyMap()
                        )
                    );
                    put(
                        "B",
                        new OptimizelyVariation(
                            "3433458314",
                            "B",
                            null,
                            Collections.emptyMap()
                        )
                    );
                }},
                ""
            )
        );

        Map<String, OptimizelyFeature> optimizelyFeatureMap = new HashMap<>();
        optimizelyFeatureMap.put(
            "multi_variate_feature",
            new OptimizelyFeature(
                "3263342226",
                "multi_variate_feature",
                new HashMap<String, OptimizelyExperiment>() {{
                    put(
                        "multivariate_experiment",
                        new OptimizelyExperiment(
                            "3262035800",
                            "multivariate_experiment",
                            new HashMap<String, OptimizelyVariation>() {{
                                put(
                                    "Feorge",
                                    new OptimizelyVariation(
                                        "3631049532",
                                        "Feorge",
                                        true,
                                        new HashMap<String, OptimizelyVariable>() {{
                                            put(
                                                "first_letter",
                                                new OptimizelyVariable(
                                                    "675244127",
                                                    "first_letter",
                                                    "string",
                                                    "F"
                                                )
                                            );
                                            put(
                                                "rest_of_name",
                                                new OptimizelyVariable(
                                                    "4052219963",
                                                    "rest_of_name",
                                                    "string",
                                                    "eorge"
                                                )
                                            );
                                        }}
                                    )
                                );
                                put(
                                    "Fred",
                                    new OptimizelyVariation(
                                        "1880281238",
                                        "Fred",
                                        true,
                                        new HashMap<String, OptimizelyVariable>() {{
                                            put(
                                                "first_letter",
                                                new OptimizelyVariable(
                                                    "675244127",
                                                    "first_letter",
                                                    "string",
                                                    "F"
                                                )
                                            );
                                            put(
                                                "rest_of_name",
                                                new OptimizelyVariable(
                                                    "4052219963",
                                                    "rest_of_name",
                                                    "string",
                                                    "red"
                                                )
                                            );
                                        }}
                                    )
                                );
                            }},
                            ""
                        )
                    );
                }},
                new HashMap<String, OptimizelyVariable>() {{
                    put(
                        "first_letter",
                        new OptimizelyVariable(
                            "675244127",
                            "first_letter",
                            "string",
                            "H"
                        )
                    );
                    put(
                        "rest_of_name",
                        new OptimizelyVariable(
                            "4052219963",
                            "rest_of_name",
                            "string",
                            "arry"
                        )
                    );
                }},
                asList(
                    new OptimizelyExperiment(
                        "3262035800",
                        "multivariate_experiment",
                        new HashMap<String, OptimizelyVariation>() {{
                            put(
                                "Feorge",
                                new OptimizelyVariation(
                                    "3631049532",
                                    "Feorge",
                                    true,
                                    new HashMap<String, OptimizelyVariable>() {{
                                        put(
                                            "first_letter",
                                            new OptimizelyVariable(
                                                "675244127",
                                                "first_letter",
                                                "string",
                                                "F"
                                            )
                                        );
                                        put(
                                            "rest_of_name",
                                            new OptimizelyVariable(
                                                "4052219963",
                                                "rest_of_name",
                                                "string",
                                                "eorge"
                                            )
                                        );
                                    }}
                                )
                            );
                            put(
                                "Fred",
                                new OptimizelyVariation(
                                    "1880281238",
                                    "Fred",
                                    true,
                                    new HashMap<String, OptimizelyVariable>() {{
                                        put(
                                            "first_letter",
                                            new OptimizelyVariable(
                                                "675244127",
                                                "first_letter",
                                                "string",
                                                "F"
                                            )
                                        );
                                        put(
                                            "rest_of_name",
                                            new OptimizelyVariable(
                                                "4052219963",
                                                "rest_of_name",
                                                "string",
                                                "red"
                                            )
                                        );
                                    }}
                                )
                            );
                        }},
                        ""
                    )
                ),
                Collections.<OptimizelyExperiment>emptyList()
            )
        );
        optimizelyFeatureMap.put(
            "boolean_feature",
            new OptimizelyFeature(
                "4195505407",
                "boolean_feature",
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.<OptimizelyExperiment>emptyList(),
                Collections.<OptimizelyExperiment>emptyList()
            )
        );

        return new OptimizelyConfig(
            optimizelyExperimentMap,
            optimizelyFeatureMap,
            "1480511547",
            "ValidProjectConfigV4",
            "production",
            asList(
                new OptimizelyAttribute(
                    "553339214",
                    "house"
                ),
                new OptimizelyAttribute(
                    "58339410",
                    "nationality"
                )
            ),
            asList(
                new OptimizelyEvent(
                    "3785620495",
                    "basic_event",
                    asList("1323241596", "2738374745", "3042640549", "3262035800", "3072915611")
                ),
                new OptimizelyEvent(
                    "3195631717",
                    "event_with_paused_experiment",
                    asList("2667098701")
                )
            ),
            asList(
                new OptimizelyAudience(
                    "123456",
                    "test_audience_1",
                    "[\"and\", [\"or\", \"1\", \"2\"], \"3\"]"
                )
            ),
            null
        );
    }
}
