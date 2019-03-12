/**
 *
 *    Copyright 2016-2019, Optimizely and contributors
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

import ch.qos.logback.classic.Level;
import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


import com.optimizely.ab.internal.LogbackVerifier;
import com.optimizely.ab.internal.ControlAttribute;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link DatafileProjectConfig}.
 */
public class DatafileProjectConfigTest {

    private ProjectConfig projectConfig;

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Before
    public void initialize() {
        projectConfig = DatafileProjectConfigTestUtils.validProjectConfigV3();
    }

    /**
     * Verify that {@link DatafileProjectConfig#toString()} doesn't throw an exception.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void toStringDoesNotFail() throws Exception {
        projectConfig.toString();
    }

    /**
     * Asserts that {@link DatafileProjectConfig#getExperimentsForEventKey(String)}
     * returns the respective experiment ids for experiments using an event,
     * provided that the event parameter is valid.
     */
    @Test
    public void verifyGetExperimentsForValidEvent() throws Exception {
        Experiment experiment223 = projectConfig.getExperimentIdMapping().get("223");
        Experiment experiment118 = projectConfig.getExperimentIdMapping().get("118");
        List<Experiment> expectedSingleExperiment = asList(experiment223);
        List<Experiment> actualSingleExperiment = projectConfig.getExperimentsForEventKey("clicked_cart");
        assertThat(actualSingleExperiment, is(expectedSingleExperiment));

        List<Experiment> expectedMultipleExperiments = asList(experiment118, experiment223);
        List<Experiment> actualMultipleExperiments = projectConfig.getExperimentsForEventKey("clicked_purchase");
        assertThat(actualMultipleExperiments, is(expectedMultipleExperiments));
    }

    /**
     * Asserts that {@link DatafileProjectConfig#getExperimentsForEventKey(String)} returns an empty List
     * when given an invalid event key.
     */
    @Test
    public void verifyGetExperimentsForInvalidEvent() throws Exception {
        List<Experiment> expectedExperiments = Collections.emptyList();
        List<Experiment> actualExperiments = projectConfig.getExperimentsForEventKey("a_fake_event");
        assertThat(actualExperiments, is(expectedExperiments));
    }

    /**
     * Asserts that getAudience returns the respective audience, provided the
     * audience ID parameter is valid.
     */
    @Test
    public void verifyGetAudienceConditionsFromValidId() throws Exception {
        List<Condition> userAttributes = new ArrayList<Condition>();
        userAttributes.add(new UserAttribute("browser_type", "custom_attribute", null, "firefox"));

        OrCondition orInner = new OrCondition(userAttributes);

        NotCondition notCondition = new NotCondition(orInner);
        List<Condition> outerOrList = new ArrayList<Condition>();
        outerOrList.add(notCondition);

        OrCondition orOuter = new OrCondition(outerOrList);
        List<Condition> andList = new ArrayList<Condition>();
        andList.add(orOuter);

        Condition expectedConditions = new AndCondition(andList);
        Condition actualConditions = projectConfig.getAudience("100").getConditions();
        assertThat(actualConditions, is(expectedConditions));
    }

    /**
     * Asserts that getAudience returns null given an invalid audience ID parameter.
     */
    @Test
    public void verifyGetAudienceFromInvalidId() throws Exception {
        assertNull(projectConfig.getAudience("invalid_id"));
    }

    /**
     * Asserts that anonymizeIP is set to false if not explicitly passed into the constructor (in the case of V2
     * projects).
     *
     * @throws Exception
     */
    @Test
    public void verifyAnonymizeIPIsFalseByDefault() throws Exception {
        ProjectConfig v2ProjectConfig = DatafileProjectConfigTestUtils.validProjectConfigV2();
        assertFalse(v2ProjectConfig.getAnonymizeIP());
    }

    @Test
    public void getAttributeIDWhenAttributeKeyIsFromAttributeKeyMapping() {
        ProjectConfig projectConfig = DatafileProjectConfigTestUtils.validProjectConfigV4();
        String attributeID = projectConfig.getAttributeId(projectConfig, "house");
        assertEquals(attributeID, "553339214");
    }

    @Test
    public void getAttributeIDWhenAttributeKeyIsUsingReservedKey() {
        ProjectConfig projectConfig = DatafileProjectConfigTestUtils.validProjectConfigV4();
        String attributeID = projectConfig.getAttributeId(projectConfig, "$opt_user_agent");
        assertEquals(attributeID, ControlAttribute.USER_AGENT_ATTRIBUTE.toString());
    }

    @Test
    public void getAttributeIDWhenAttributeKeyUnrecognizedAttribute() {
        ProjectConfig projectConfig = DatafileProjectConfigTestUtils.validProjectConfigV4();
        String invalidAttribute = "empty";
        String attributeID = projectConfig.getAttributeId(projectConfig, invalidAttribute);
        assertNull(attributeID);
        logbackVerifier.expectMessage(Level.DEBUG, "Unrecognized Attribute \"" + invalidAttribute + "\"");
    }

    @Test
    public void getAttributeIDWhenAttributeKeyPrefixIsMatched() {
        ProjectConfig projectConfig = DatafileProjectConfigTestUtils.validProjectConfigV4();
        String attributeWithReservedPrefix = "$opt_test";
        String attributeID = projectConfig.getAttributeId(projectConfig, attributeWithReservedPrefix);
        assertEquals(attributeID, "583394100");
        logbackVerifier.expectMessage(Level.WARN, "Attribute " + attributeWithReservedPrefix + " unexpectedly" +
            " has reserved prefix $opt_; using attribute ID instead of reserved attribute name.");
    }

}
