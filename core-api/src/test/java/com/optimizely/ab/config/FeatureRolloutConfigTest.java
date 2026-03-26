/**
 *
 *    Copyright 2026, Optimizely and contributors
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
package com.optimizely.ab.config;

import com.optimizely.ab.config.parser.ConfigParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for Feature Rollout support in {@link DatafileProjectConfig}.
 */
public class FeatureRolloutConfigTest {

    private ProjectConfig projectConfig;

    @Before
    public void setUp() throws ConfigParseException, IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("config/feature-rollout-config.json");
        assertNotNull("Test fixture not found", is);
        byte[] bytes = is.readAllBytes();
        String datafile = new String(bytes, StandardCharsets.UTF_8);
        projectConfig = new DatafileProjectConfig.Builder().withDatafile(datafile).build();
    }

    /**
     * Test 1: Backward compatibility - experiments without type field have type=null.
     */
    @Test
    public void experimentWithoutTypeFieldHasNullType() {
        Experiment experiment = projectConfig.getExperimentKeyMapping().get("no_type_experiment");
        assertNotNull("Experiment should exist", experiment);
        assertNull("Type should be null for experiments without type field", experiment.getType());
    }

    /**
     * Test 2: Core injection - feature_rollout experiments get everyone else variation
     * and trafficAllocation (endOfRange=10000) injected.
     */
    @Test
    public void featureRolloutExperimentGetsEveryoneElseVariationInjected() {
        Experiment experiment = projectConfig.getExperimentKeyMapping().get("feature_rollout_experiment");
        assertNotNull("Experiment should exist", experiment);
        assertEquals(Experiment.TYPE_FR, experiment.getType());

        // Should have 2 variations: original + everyone else
        assertEquals("Should have 2 variations after injection", 2, experiment.getVariations().size());

        // Check the injected variation
        Variation injectedVariation = experiment.getVariations().get(1);
        assertEquals("everyone_else_var", injectedVariation.getId());
        assertEquals("everyone_else_variation", injectedVariation.getKey());

        // Check the injected traffic allocation
        List<TrafficAllocation> trafficAllocations = experiment.getTrafficAllocation();
        assertEquals("Should have 2 traffic allocations after injection", 2, trafficAllocations.size());
        TrafficAllocation injectedAllocation = trafficAllocations.get(1);
        assertEquals("everyone_else_var", injectedAllocation.getEntityId());
        assertEquals(10000, injectedAllocation.getEndOfRange());
    }

    /**
     * Test 3: Variation maps updated - all variation lookup maps contain the injected variation.
     */
    @Test
    public void variationMapsContainInjectedVariation() {
        Experiment experiment = projectConfig.getExperimentKeyMapping().get("feature_rollout_experiment");
        assertNotNull("Experiment should exist", experiment);

        // Check variationKeyToVariationMap
        Map<String, Variation> keyMap = experiment.getVariationKeyToVariationMap();
        assertTrue("Key map should contain injected variation",
            keyMap.containsKey("everyone_else_variation"));

        // Check variationIdToVariationMap
        Map<String, Variation> idMap = experiment.getVariationIdToVariationMap();
        assertTrue("ID map should contain injected variation",
            idMap.containsKey("everyone_else_var"));
    }

    /**
     * Test 4: Non-rollout unchanged - A/B experiments are not modified by injection logic.
     */
    @Test
    public void abTestExperimentNotModified() {
        Experiment experiment = projectConfig.getExperimentKeyMapping().get("ab_test_experiment");
        assertNotNull("Experiment should exist", experiment);
        assertEquals(Experiment.TYPE_AB, experiment.getType());

        // Should still have exactly 2 original variations
        assertEquals("A/B test should keep original 2 variations", 2, experiment.getVariations().size());
        assertEquals("control", experiment.getVariations().get(0).getKey());
        assertEquals("treatment", experiment.getVariations().get(1).getKey());

        // Should still have exactly 2 original traffic allocations
        assertEquals("A/B test should keep original 2 traffic allocations",
            2, experiment.getTrafficAllocation().size());
    }

    /**
     * Test 5: No rollout edge case - feature_rollout experiment with empty rolloutId
     * does not crash (silent skip).
     */
    @Test
    public void featureRolloutWithEmptyRolloutIdDoesNotCrash() {
        Experiment experiment = projectConfig.getExperimentKeyMapping().get("rollout_no_rollout_id_experiment");
        assertNotNull("Experiment should exist", experiment);
        assertEquals(Experiment.TYPE_FR, experiment.getType());

        // Should keep only original variation since rollout cannot be resolved
        assertEquals("Should keep only original variation", 1, experiment.getVariations().size());
        assertEquals("rollout_no_rollout_variation", experiment.getVariations().get(0).getKey());
    }

    /**
     * Test 6: Type field parsed - experiments with type field in the datafile
     * have the value correctly preserved after config parsing.
     */
    @Test
    public void typeFieldCorrectlyParsed() {
        Experiment rolloutExp = projectConfig.getExperimentKeyMapping().get("feature_rollout_experiment");
        assertNotNull(rolloutExp);
        assertEquals(Experiment.TYPE_FR, rolloutExp.getType());

        Experiment abExp = projectConfig.getExperimentKeyMapping().get("ab_test_experiment");
        assertNotNull(abExp);
        assertEquals(Experiment.TYPE_AB, abExp.getType());

        Experiment noTypeExp = projectConfig.getExperimentKeyMapping().get("no_type_experiment");
        assertNotNull(noTypeExp);
        assertNull(noTypeExp.getType());
    }
}
