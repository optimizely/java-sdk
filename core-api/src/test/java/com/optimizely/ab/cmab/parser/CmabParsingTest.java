/**
 *
 *    Copyright 2025 Optimizely and contributors
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
package com.optimizely.ab.cmab.parser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.optimizely.ab.config.Cmab;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Group;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.config.parser.ConfigParser;
import com.optimizely.ab.config.parser.GsonConfigParser;
import com.optimizely.ab.config.parser.JacksonConfigParser;
import com.optimizely.ab.config.parser.JsonConfigParser;
import com.optimizely.ab.config.parser.JsonSimpleConfigParser;

/**
 * Tests CMAB parsing across all config parsers using real datafile
 */
@RunWith(Parameterized.class)
public class CmabParsingTest {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"JsonSimpleConfigParser", new JsonSimpleConfigParser()},
            {"GsonConfigParser", new GsonConfigParser()},
            {"JacksonConfigParser", new JacksonConfigParser()},
            {"JsonConfigParser", new JsonConfigParser()}
        });
    }

    private final String parserName;
    private final ConfigParser parser;

    public CmabParsingTest(String parserName, ConfigParser parser) {
        this.parserName = parserName;
        this.parser = parser;
    }

    private String loadCmabDatafile() throws IOException {
        return Resources.toString(Resources.getResource("config/cmab-config.json"), Charsets.UTF_8);
    }

    @Test
    public void testParseExperimentWithValidCmab() throws IOException, ConfigParseException {
        String datafile = loadCmabDatafile();
        ProjectConfig config = parser.parseProjectConfig(datafile);

        Experiment experiment = config.getExperimentKeyMapping().get("exp_with_cmab");
        assertNotNull("Experiment 'exp_with_cmab' should exist in " + parserName, experiment);

        Cmab cmab = experiment.getCmab();
        assertNotNull("CMAB should not be null for experiment with CMAB in " + parserName, cmab);

        assertEquals("Should have 2 attribute IDs in " + parserName, 2, cmab.getAttributeIds().size());
        assertTrue("Should contain attribute '10401066117' in " + parserName,
            cmab.getAttributeIds().contains("10401066117"));
        assertTrue("Should contain attribute '10401066170' in " + parserName,
            cmab.getAttributeIds().contains("10401066170"));
        assertEquals("Traffic allocation should be 4000 in " + parserName, 4000, cmab.getTrafficAllocation());
    }

    @Test
    public void testParseExperimentWithoutCmab() throws IOException, ConfigParseException {
        String datafile = loadCmabDatafile();
        ProjectConfig config = parser.parseProjectConfig(datafile);

        Experiment experiment = config.getExperimentKeyMapping().get("exp_without_cmab");
        assertNotNull("Experiment 'exp_without_cmab' should exist in " + parserName, experiment);
        assertNull("CMAB should be null when not specified in " + parserName, experiment.getCmab());
    }

    @Test
    public void testParseExperimentWithEmptyAttributeIds() throws IOException, ConfigParseException {
        String datafile = loadCmabDatafile();
        ProjectConfig config = parser.parseProjectConfig(datafile);

        Experiment experiment = config.getExperimentKeyMapping().get("exp_with_empty_cmab");
        assertNotNull("Experiment 'exp_with_empty_cmab' should exist in " + parserName, experiment);

        Cmab cmab = experiment.getCmab();
        assertNotNull("CMAB should not be null even with empty attributeIds in " + parserName, cmab);
        assertTrue("AttributeIds should be empty in " + parserName, cmab.getAttributeIds().isEmpty());
        assertEquals("Traffic allocation should be 2000 in " + parserName, 2000, cmab.getTrafficAllocation());
    }

    @Test
    public void testParseExperimentWithNullCmab() throws IOException, ConfigParseException {
        String datafile = loadCmabDatafile();
        ProjectConfig config = parser.parseProjectConfig(datafile);

        Experiment experiment = config.getExperimentKeyMapping().get("exp_with_null_cmab");
        assertNotNull("Experiment 'exp_with_null_cmab' should exist in " + parserName, experiment);
        assertNull("CMAB should be null when explicitly set to null in " + parserName, experiment.getCmab());
    }

    @Test
    public void testParseGroupExperimentWithCmab() throws IOException, ConfigParseException {
        String datafile = loadCmabDatafile();
        ProjectConfig config = parser.parseProjectConfig(datafile);

        // Find the group experiment
        Experiment groupExperiment = null;
        for (Group group : config.getGroups()) {
            for (Experiment exp : group.getExperiments()) {
                if ("group_exp_with_cmab".equals(exp.getKey())) {
                    groupExperiment = exp;
                    break;
                }
            }
        }

        assertNotNull("Group experiment 'group_exp_with_cmab' should exist in " + parserName, groupExperiment);

        Cmab cmab = groupExperiment.getCmab();
        assertNotNull("Group experiment CMAB should not be null in " + parserName, cmab);
        assertEquals("Should have 1 attribute ID in " + parserName, 1, cmab.getAttributeIds().size());
        assertEquals("Should contain correct attribute in " + parserName,
            "10401066117", cmab.getAttributeIds().get(0));
        assertEquals("Traffic allocation should be 6000 in " + parserName, 6000, cmab.getTrafficAllocation());
    }

    @Test
    public void testParseAllExperimentsFromDatafile() throws IOException, ConfigParseException {
        String datafile = loadCmabDatafile();
        ProjectConfig config = parser.parseProjectConfig(datafile);

        // Check all expected experiments exist
        assertTrue("Should have 'exp_with_cmab' in " + parserName,
            config.getExperimentKeyMapping().containsKey("exp_with_cmab"));
        assertTrue("Should have 'exp_without_cmab' in " + parserName,
            config.getExperimentKeyMapping().containsKey("exp_without_cmab"));
        assertTrue("Should have 'exp_with_empty_cmab' in " + parserName,
            config.getExperimentKeyMapping().containsKey("exp_with_empty_cmab"));
        assertTrue("Should have 'exp_with_null_cmab' in " + parserName,
            config.getExperimentKeyMapping().containsKey("exp_with_null_cmab"));
    }

    @Test
    public void testParseProjectConfigStructure() throws IOException, ConfigParseException {
        String datafile = loadCmabDatafile();
        ProjectConfig config = parser.parseProjectConfig(datafile);

        // Verify basic project config data
        assertEquals("Project ID should match in " + parserName, "10431130345", config.getProjectId());
        assertEquals("Account ID should match in " + parserName, "10367498574", config.getAccountId());
        assertEquals("Version should match in " + parserName, "4", config.getVersion());
        assertEquals("Revision should match in " + parserName, "241", config.getRevision());

        // Verify component counts based on your cmab-config.json
        assertEquals("Should have 5 experiments in " + parserName, 5, config.getExperiments().size());
        assertEquals("Should have 2 audiences in " + parserName, 2, config.getAudiences().size());
        assertEquals("Should have 2 attributes in " + parserName, 2, config.getAttributes().size());
        assertEquals("Should have 1 event in " + parserName, 1, config.getEventTypes().size());
        assertEquals("Should have 1 group in " + parserName, 1, config.getGroups().size());
        assertEquals("Should have 1 feature flag in " + parserName, 1, config.getFeatureFlags().size());
    }

    @Test
    public void testCmabFieldsAreCorrectlyParsed() throws IOException, ConfigParseException {
        String datafile = loadCmabDatafile();
        ProjectConfig config = parser.parseProjectConfig(datafile);

        // Test experiment with full CMAB
        Experiment expWithCmab = config.getExperimentKeyMapping().get("exp_with_cmab");
        Cmab cmab = expWithCmab.getCmab();

        assertNotNull("CMAB object should exist in " + parserName, cmab);
        assertEquals("CMAB should have exactly 2 attributes in " + parserName,
            Arrays.asList("10401066117", "10401066170"), cmab.getAttributeIds());
        assertEquals("CMAB traffic allocation should be 4000 in " + parserName, 4000, cmab.getTrafficAllocation());

        // Test experiment with empty CMAB
        Experiment expWithEmptyCmab = config.getExperimentKeyMapping().get("exp_with_empty_cmab");
        Cmab emptyCmab = expWithEmptyCmab.getCmab();

        assertNotNull("Empty CMAB object should exist in " + parserName, emptyCmab);
        assertTrue("CMAB attributeIds should be empty in " + parserName, emptyCmab.getAttributeIds().isEmpty());
        assertEquals("Empty CMAB traffic allocation should be 2000 in " + parserName,
            2000, emptyCmab.getTrafficAllocation());
    }

    @Test
    public void testExperimentIdsAndKeysMatch() throws IOException, ConfigParseException {
        String datafile = loadCmabDatafile();
        ProjectConfig config = parser.parseProjectConfig(datafile);

        // Verify experiment IDs and keys from your datafile
        Experiment expWithCmab = config.getExperimentKeyMapping().get("exp_with_cmab");
        assertEquals("exp_with_cmab ID should match in " + parserName, "10390977673", expWithCmab.getId());

        Experiment expWithoutCmab = config.getExperimentKeyMapping().get("exp_without_cmab");
        assertEquals("exp_without_cmab ID should match in " + parserName, "10420810910", expWithoutCmab.getId());

        Experiment expWithEmptyCmab = config.getExperimentKeyMapping().get("exp_with_empty_cmab");
        assertEquals("exp_with_empty_cmab ID should match in " + parserName, "10420810911", expWithEmptyCmab.getId());

        Experiment expWithNullCmab = config.getExperimentKeyMapping().get("exp_with_null_cmab");
        assertEquals("exp_with_null_cmab ID should match in " + parserName, "10420810912", expWithNullCmab.getId());
    }

    @Test
    public void testCmabDoesNotAffectOtherExperimentFields() throws IOException, ConfigParseException {
        String datafile = loadCmabDatafile();
        ProjectConfig config = parser.parseProjectConfig(datafile);

        Experiment expWithCmab = config.getExperimentKeyMapping().get("exp_with_cmab");

        // Verify other fields are still parsed correctly
        assertEquals("Experiment status should be parsed correctly in " + parserName,
            "Running", expWithCmab.getStatus());
        assertEquals("Experiment should have correct layer ID in " + parserName,
            "10420273888", expWithCmab.getLayerId());
        assertEquals("Experiment should have 2 variations in " + parserName,
            2, expWithCmab.getVariations().size());
        assertEquals("Experiment should have 1 audience in " + parserName,
            1, expWithCmab.getAudienceIds().size());
        assertEquals("Experiment should have correct audience ID in " + parserName,
            "13389141123", expWithCmab.getAudienceIds().get(0));
    }
}