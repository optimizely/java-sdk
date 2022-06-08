/**
 *
 *    Copyright 2016-2017, 2019, Optimizely and contributors
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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;
import org.junit.Assert;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Helper class that provides common functionality and resources for testing {@link DatafileProjectConfig}.
 */
public final class DatafileProjectConfigTestUtils {

    private static final ProjectConfig VALID_PROJECT_CONFIG_V2 = generateValidProjectConfigV2();

    private static ProjectConfig generateValidProjectConfigV2() {
        List<Experiment> experiments = asList(
            new Experiment("223", "etag1", "Running", "1",
                singletonList("100"),
                null,
                asList(new Variation("276", "vtag1"),
                    new Variation("277", "vtag2")),
                Collections.singletonMap("testUser1", "vtag1"),
                asList(new TrafficAllocation("276", 3500),
                    new TrafficAllocation("277", 9000)),
                ""),
            new Experiment("118", "etag2", "Not started", "2",
                singletonList("100"),
                null,
                asList(new Variation("278", "vtag3"),
                    new Variation("279", "vtag4")),
                Collections.singletonMap("testUser3", "vtag3"),
                asList(new TrafficAllocation("278", 4500),
                    new TrafficAllocation("279", 9000)),
                ""),
            new Experiment("119", "etag3", "Not started", null,
                singletonList("100"),
                null,
                asList(new Variation("280", "vtag5"),
                    new Variation("281", "vtag6")),
                Collections.singletonMap("testUser4", "vtag5"),
                asList(new TrafficAllocation("280", 4500),
                    new TrafficAllocation("281", 9000)),
                "")
        );

        List<Attribute> attributes = singletonList(new Attribute("134", "browser_type"));

        List<String> singleExperimentId = singletonList("223");
        List<String> multipleExperimentIds = asList("118", "223");
        List<EventType> events = asList(new EventType("971", "clicked_cart", singleExperimentId),
            new EventType("098", "Total Revenue", singleExperimentId),
            new EventType("099", "clicked_purchase", multipleExperimentIds),
            new EventType("100", "no_running_experiments", singletonList("118")));

        List<Condition> userAttributes = new ArrayList<Condition>();
        userAttributes.add(new UserAttribute("browser_type", "custom_attribute", null, "firefox"));

        OrCondition orInner = new OrCondition(userAttributes);

        NotCondition notCondition = new NotCondition(orInner);
        List<Condition> outerOrList = new ArrayList<Condition>();
        outerOrList.add(notCondition);

        OrCondition orOuter = new OrCondition(outerOrList);
        List<Condition> andList = new ArrayList<Condition>();
        andList.add(orOuter);

        AndCondition andCondition = new AndCondition(andList);

        List<Audience> audiences = singletonList(new Audience("100", "not_firefox_users", andCondition));

        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        userIdToVariationKeyMap.put("testUser1", "e1_vtag1");
        userIdToVariationKeyMap.put("testUser2", "e1_vtag2");

        List<Experiment> randomGroupExperiments = asList(
            new Experiment("301", "group_etag2", "Running", "3",
                singletonList("100"),
                null,
                asList(new Variation("282", "e2_vtag1"),
                    new Variation("283", "e2_vtag2")),
                Collections.<String, String>emptyMap(),
                asList(new TrafficAllocation("282", 5000),
                    new TrafficAllocation("283", 10000)),
                "42"),
            new Experiment("300", "group_etag1", "Running", "4",
                singletonList("100"),
                null,
                asList(new Variation("280", "e1_vtag1"),
                    new Variation("281", "e1_vtag2")),
                userIdToVariationKeyMap,
                asList(new TrafficAllocation("280", 3000),
                    new TrafficAllocation("281", 10000)),
                "42")
        );

        List<Experiment> overlappingGroupExperiments = asList(
            new Experiment("302", "overlapping_etag1", "Running", "5",
                singletonList("100"),
                null,
                asList(new Variation("284", "e1_vtag1"),
                    new Variation("285", "e1_vtag2")),
                userIdToVariationKeyMap,
                asList(new TrafficAllocation("284", 1500),
                    new TrafficAllocation("285", 3000)),
                "43")
        );

        Group randomPolicyGroup = new Group("42", "random",
            randomGroupExperiments,
            asList(new TrafficAllocation("300", 3000),
                new TrafficAllocation("301", 9000),
                new TrafficAllocation("", 10000)));
        Group overlappingPolicyGroup = new Group("43", "overlapping",
            overlappingGroupExperiments,
            Collections.<TrafficAllocation>emptyList());
        List<Group> groups = asList(randomPolicyGroup, overlappingPolicyGroup);

        return new DatafileProjectConfig("789", "1234", "2", "42", groups, experiments, attributes, events, audiences);
    }

    private static final ProjectConfig NO_AUDIENCE_PROJECT_CONFIG_V2 = generateNoAudienceProjectConfigV2();

    private static ProjectConfig generateNoAudienceProjectConfigV2() {
        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        userIdToVariationKeyMap.put("testUser1", "vtag1");
        userIdToVariationKeyMap.put("testUser2", "vtag2");

        List<Experiment> experiments = asList(
            new Experiment("223", "etag1", "Running", "1",
                Collections.<String>emptyList(),
                null,
                asList(new Variation("276", "vtag1"),
                    new Variation("277", "vtag2")),
                userIdToVariationKeyMap,
                asList(new TrafficAllocation("276", 3500),
                    new TrafficAllocation("277", 9000)),
                ""),
            new Experiment("118", "etag2", "Not started", "2",
                Collections.<String>emptyList(),
                null,
                asList(new Variation("278", "vtag3"),
                    new Variation("279", "vtag4")),
                Collections.<String, String>emptyMap(),
                asList(new TrafficAllocation("278", 4500),
                    new TrafficAllocation("279", 9000)),
                ""),
            new Experiment("119", "etag3", "Launched", "3",
                Collections.<String>emptyList(),
                null,
                asList(new Variation("280", "vtag5"),
                    new Variation("281", "vtag6")),
                Collections.<String, String>emptyMap(),
                asList(new TrafficAllocation("280", 5000),
                    new TrafficAllocation("281", 10000)),
                "")
        );

        List<Attribute> attributes = singletonList(new Attribute("134", "browser_type"));

        List<String> singleExperimentId = singletonList("223");
        List<String> multipleExperimentIds = asList("118", "223");
        List<EventType> events = asList(
            new EventType("971", "clicked_cart", singleExperimentId),
            new EventType("098", "Total Revenue", singleExperimentId),
            new EventType("099", "clicked_purchase", multipleExperimentIds),
            new EventType("100", "launched_exp_event", singletonList("119")),
            new EventType("101", "event_with_launched_and_running_experiments", Arrays.asList("119", "223"))
        );

        return new DatafileProjectConfig("789", "1234", "2", "42", Collections.<Group>emptyList(), experiments, attributes,
            events, Collections.<Audience>emptyList());
    }

    private static final ProjectConfig VALID_PROJECT_CONFIG_V3 = generateValidProjectConfigV3();

    private static ProjectConfig generateValidProjectConfigV3() {
        List<FeatureVariableUsageInstance> variationVtag1VariableUsageInstances = asList(
            new FeatureVariableUsageInstance("6", "True"),
            new FeatureVariableUsageInstance("2", "10"),
            new FeatureVariableUsageInstance("3", "string_var_vtag1"),
            new FeatureVariableUsageInstance("4", "5.3")
        );

        List<FeatureVariableUsageInstance> variationVtag2VariableUsageInstances = asList(
            new FeatureVariableUsageInstance("6", "False"),
            new FeatureVariableUsageInstance("2", "20"),
            new FeatureVariableUsageInstance("3", "string_var_vtag2"),
            new FeatureVariableUsageInstance("4", "6.3")
        );

        List<Experiment> experiments = asList(
            new Experiment("223", "etag1", "Running", "1",
                singletonList("100"),
                null,
                asList(new Variation("276", "vtag1", variationVtag1VariableUsageInstances),
                    new Variation("277", "vtag2", variationVtag2VariableUsageInstances)),
                Collections.singletonMap("testUser1", "vtag1"),
                asList(new TrafficAllocation("276", 3500),
                    new TrafficAllocation("277", 9000)),
                ""),
            new Experiment("118", "etag2", "Not started", "2",
                singletonList("100"),
                null,
                asList(new Variation("278", "vtag3", Collections.<FeatureVariableUsageInstance>emptyList()),
                    new Variation("279", "vtag4", Collections.<FeatureVariableUsageInstance>emptyList())),
                Collections.singletonMap("testUser3", "vtag3"),
                asList(new TrafficAllocation("278", 4500),
                    new TrafficAllocation("279", 9000)),
                "")
        );

        List<Attribute> attributes = singletonList(new Attribute("134", "browser_type"));

        List<String> singleExperimentId = singletonList("223");
        List<String> multipleExperimentIds = asList("118", "223");
        List<EventType> events = asList(new EventType("971", "clicked_cart", singleExperimentId),
            new EventType("098", "Total Revenue", singleExperimentId),
            new EventType("099", "clicked_purchase", multipleExperimentIds),
            new EventType("100", "no_running_experiments", singletonList("118")));

        List<Condition> userAttributes = new ArrayList<Condition>();
        userAttributes.add(new UserAttribute("browser_type", "custom_attribute", null, "firefox"));

        OrCondition orInner = new OrCondition(userAttributes);

        NotCondition notCondition = new NotCondition(orInner);
        List<Condition> outerOrList = new ArrayList<Condition>();
        outerOrList.add(notCondition);

        OrCondition orOuter = new OrCondition(outerOrList);
        List<Condition> andList = new ArrayList<Condition>();
        andList.add(orOuter);

        AndCondition andCondition = new AndCondition(andList);

        List<Audience> audiences = singletonList(new Audience("100", "not_firefox_users", andCondition));

        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        userIdToVariationKeyMap.put("testUser1", "e1_vtag1");
        userIdToVariationKeyMap.put("testUser2", "e1_vtag2");

        List<Experiment> randomGroupExperiments = asList(
            new Experiment("301", "group_etag2", "Running", "3",
                singletonList("100"),
                null,
                asList(new Variation("282", "e2_vtag1", Collections.<FeatureVariableUsageInstance>emptyList()),
                    new Variation("283", "e2_vtag2", Collections.<FeatureVariableUsageInstance>emptyList())),
                Collections.<String, String>emptyMap(),
                asList(new TrafficAllocation("282", 5000),
                    new TrafficAllocation("283", 10000)),
                "42"),
            new Experiment("300", "group_etag1", "Running", "4",
                singletonList("100"),
                null,
                asList(new Variation("280", "e1_vtag1",
                        Collections.singletonList(new FeatureVariableUsageInstance("7", "True"))),
                    new Variation("281", "e1_vtag2",
                        Collections.singletonList(new FeatureVariableUsageInstance("7", "False")))),
                userIdToVariationKeyMap,
                asList(new TrafficAllocation("280", 3000),
                    new TrafficAllocation("281", 10000)),
                "42")
        );

        List<Experiment> overlappingGroupExperiments = asList(
            new Experiment("302", "overlapping_etag1", "Running", "5",
                singletonList("100"),
                null,
                asList(new Variation("284", "e1_vtag1", Collections.<FeatureVariableUsageInstance>emptyList()),
                    new Variation("285", "e1_vtag2", Collections.<FeatureVariableUsageInstance>emptyList())),
                userIdToVariationKeyMap,
                asList(new TrafficAllocation("284", 1500),
                    new TrafficAllocation("285", 3000)),
                "43")
        );

        Group randomPolicyGroup = new Group("42", "random",
            randomGroupExperiments,
            asList(new TrafficAllocation("300", 3000),
                new TrafficAllocation("301", 9000),
                new TrafficAllocation("", 10000)));
        Group overlappingPolicyGroup = new Group("43", "overlapping",
            overlappingGroupExperiments,
            Collections.<TrafficAllocation>emptyList());
        List<Group> groups = asList(randomPolicyGroup, overlappingPolicyGroup);

        return new DatafileProjectConfig("789", "1234", "3", "42", groups, experiments, attributes, events, audiences,
            true);
    }

    private static final ProjectConfig NO_AUDIENCE_PROJECT_CONFIG_V3 = generateNoAudienceProjectConfigV3();

    private static ProjectConfig generateNoAudienceProjectConfigV3() {
        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        userIdToVariationKeyMap.put("testUser1", "vtag1");
        userIdToVariationKeyMap.put("testUser2", "vtag2");

        List<Experiment> experiments = asList(
            new Experiment("223", "etag1", "Running", "1",
                Collections.<String>emptyList(),
                null,
                asList(new Variation("276", "vtag1", Collections.<FeatureVariableUsageInstance>emptyList()),
                    new Variation("277", "vtag2", Collections.<FeatureVariableUsageInstance>emptyList())),
                userIdToVariationKeyMap,
                asList(new TrafficAllocation("276", 3500),
                    new TrafficAllocation("277", 9000)),
                ""),
            new Experiment("118", "etag2", "Not started", "2",
                Collections.<String>emptyList(),
                null,
                asList(new Variation("278", "vtag3", Collections.<FeatureVariableUsageInstance>emptyList()),
                    new Variation("279", "vtag4", Collections.<FeatureVariableUsageInstance>emptyList())),
                Collections.<String, String>emptyMap(),
                asList(new TrafficAllocation("278", 4500),
                    new TrafficAllocation("279", 9000)),
                ""),
            new Experiment("119", "etag3", "Launched", "3",
                Collections.<String>emptyList(),
                null,
                asList(new Variation("280", "vtag5"),
                    new Variation("281", "vtag6")),
                Collections.<String, String>emptyMap(),
                asList(new TrafficAllocation("280", 5000),
                    new TrafficAllocation("281", 10000)),
                "")
        );

        List<Attribute> attributes = singletonList(new Attribute("134", "browser_type"));

        List<String> singleExperimentId = singletonList("223");
        List<String> multipleExperimentIds = asList("118", "223");
        List<EventType> events = asList(
            new EventType("971", "clicked_cart", singleExperimentId),
            new EventType("098", "Total Revenue", singleExperimentId),
            new EventType("099", "clicked_purchase", multipleExperimentIds),
            new EventType("100", "launched_exp_event", singletonList("119")),
            new EventType("101", "event_with_launched_and_running_experiments", Arrays.asList("119", "223"))
        );

        return new DatafileProjectConfig("789", "1234", "3", "42", Collections.<Group>emptyList(), experiments, attributes,
            events, Collections.<Audience>emptyList(), true);
    }

    private static final ProjectConfig VALID_PROJECT_CONFIG_V4 = generateValidProjectConfigV4();

    private static ProjectConfig generateValidProjectConfigV4() {
        return ValidProjectConfigV4.generateValidProjectConfigV4();
    }

    private DatafileProjectConfigTestUtils() {
    }

    public static String validConfigJsonV2() throws IOException {
        return Resources.toString(Resources.getResource("config/valid-project-config-v2.json"), Charsets.UTF_8);
    }

    public static String noAudienceProjectConfigJsonV2() throws IOException {
        return Resources.toString(Resources.getResource("config/no-audience-project-config-v2.json"), Charsets.UTF_8);
    }

    public static String validConfigJsonV3() throws IOException {
        return Resources.toString(Resources.getResource("config/valid-project-config-v3.json"), Charsets.UTF_8);
    }

    public static String noAudienceProjectConfigJsonV3() throws IOException {
        return Resources.toString(Resources.getResource("config/no-audience-project-config-v3.json"), Charsets.UTF_8);
    }

    public static String validConfigJsonV4() throws IOException {
        return Resources.toString(Resources.getResource("config/valid-project-config-v4.json"), Charsets.UTF_8);
    }

    public static String nullFeatureEnabledConfigJsonV4()  throws IOException {
        return Resources.toString(Resources.getResource("config/null-featureEnabled-config-v4.json"), Charsets.UTF_8);
    }

    /**
     * @return the expected {@link DatafileProjectConfig} for the json produced by {@link #validConfigJsonV2()} ()}
     */
    public static ProjectConfig validProjectConfigV2() {
        return VALID_PROJECT_CONFIG_V2;
    }

    /**
     * @return the expected {@link DatafileProjectConfig} for the json produced by {@link #noAudienceProjectConfigJsonV2()}
     */
    public static ProjectConfig noAudienceProjectConfigV2() {
        return NO_AUDIENCE_PROJECT_CONFIG_V2;
    }

    /**
     * @return the expected {@link DatafileProjectConfig} for the json produced by {@link #validConfigJsonV3()} ()}
     */
    public static ProjectConfig validProjectConfigV3() {
        return VALID_PROJECT_CONFIG_V3;
    }

    /**
     * @return the expected {@link DatafileProjectConfig} for the json produced by {@link #noAudienceProjectConfigJsonV3()}
     */
    public static ProjectConfig noAudienceProjectConfigV3() {
        return NO_AUDIENCE_PROJECT_CONFIG_V3;
    }

    public static ProjectConfig validProjectConfigV4() {
        return VALID_PROJECT_CONFIG_V4;
    }

    /**
     * @return the expected {@link DatafileProjectConfig} for the json produced by {@link #invalidProjectConfigV5()}
     */
    public static String invalidProjectConfigV5() throws IOException {
        return Resources.toString(Resources.getResource("config/invalid-project-config-v5.json"), Charsets.UTF_8);
    }

    /**
     * Asserts that the provided project configs are equivalent.
     * TODO this signature is backwards should be (ProjectConfig expected, ProjectConfig actual)
     */
    public static void verifyProjectConfig(@CheckForNull ProjectConfig actual, @Nonnull ProjectConfig expected) {
        assertNotNull(actual);

        // verify the project-level values
        Assert.assertEquals(actual.getAccountId(), expected.getAccountId());
        Assert.assertEquals(actual.getProjectId(), expected.getProjectId());
        Assert.assertEquals(actual.getVersion(), expected.getVersion());
        Assert.assertEquals(actual.getRevision(), expected.getRevision());

        verifyAttributes(actual.getAttributes(), expected.getAttributes());
        verifyAudiences(actual.getAudiences(), expected.getAudiences());
        verifyAudiences(actual.getTypedAudiences(), expected.getTypedAudiences());
        verifyEvents(actual.getEventTypes(), expected.getEventTypes());
        verifyExperiments(actual.getExperiments(), expected.getExperiments());
        verifyFeatureFlags(actual.getFeatureFlags(), expected.getFeatureFlags());
        verifyGroups(actual.getGroups(), expected.getGroups());
        verifyRollouts(actual.getRollouts(), expected.getRollouts());
    }

    /**
     * Asserts that the provided experiment configs are equivalent.
     */
    private static void verifyExperiments(List<Experiment> actual, List<Experiment> expected) {
        Assert.assertEquals(actual.size(), expected.size());

        for (int i = 0; i < actual.size(); i++) {
            Experiment actualExperiment = actual.get(i);
            Experiment expectedExperiment = expected.get(i);

            Assert.assertEquals(actualExperiment.getId(), expectedExperiment.getId());
            Assert.assertEquals(actualExperiment.getKey(), expectedExperiment.getKey());
            Assert.assertEquals(actualExperiment.getGroupId(), expectedExperiment.getGroupId());
            Assert.assertEquals(actualExperiment.getStatus(), expectedExperiment.getStatus());
            Assert.assertEquals(actualExperiment.getAudienceIds(), expectedExperiment.getAudienceIds());
            Assert.assertEquals(actualExperiment.getAudienceConditions(), expectedExperiment.getAudienceConditions());
            Assert.assertEquals(actualExperiment.getUserIdToVariationKeyMap(),
                expectedExperiment.getUserIdToVariationKeyMap());

            verifyVariations(actualExperiment.getVariations(), expectedExperiment.getVariations());
            verifyTrafficAllocations(actualExperiment.getTrafficAllocation(),
                expectedExperiment.getTrafficAllocation());
        }
    }

    private static void verifyFeatureFlags(List<FeatureFlag> actual, List<FeatureFlag> expected) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < actual.size(); i++) {
            FeatureFlag actualFeatureFlag = actual.get(i);
            FeatureFlag expectedFeatureFlag = expected.get(i);

            assertEquals(expectedFeatureFlag, actualFeatureFlag);
        }
    }

    /**
     * Asserts that the provided variation configs are equivalent.
     */
    private static void verifyVariations(List<Variation> actual, List<Variation> expected) {
        Assert.assertEquals(actual.size(), expected.size());

        for (int i = 0; i < actual.size(); i++) {
            Variation actualVariation = actual.get(i);
            Variation expectedVariation = expected.get(i);

            Assert.assertEquals(actualVariation.getId(), expectedVariation.getId());
            Assert.assertEquals(actualVariation.getKey(), expectedVariation.getKey());
            verifyFeatureVariableInstances(actualVariation.getFeatureVariableUsageInstances(),
                expectedVariation.getFeatureVariableUsageInstances());
        }
    }

    /**
     * Asserts that the provided traffic allocation configs are equivalent.
     */
    private static void verifyTrafficAllocations(List<TrafficAllocation> actual,
                                                 List<TrafficAllocation> expected) {
        Assert.assertEquals(actual.size(), expected.size());

        for (int i = 0; i < actual.size(); i++) {
            TrafficAllocation actualDistribution = actual.get(i);
            TrafficAllocation expectedDistribution = expected.get(i);

            Assert.assertEquals(actualDistribution.getEntityId(), expectedDistribution.getEntityId());
            assertEquals("expectedDistribution: " + expectedDistribution.toString() +
                    "is not equal to the actualDistribution: " + actualDistribution.toString(),
                expectedDistribution.getEndOfRange(), actualDistribution.getEndOfRange());
        }
    }

    /**
     * Asserts that the provided attributes configs are equivalent.
     */
    private static void verifyAttributes(List<Attribute> actual, List<Attribute> expected) {
        Assert.assertEquals(actual.size(), expected.size());

        for (int i = 0; i < actual.size(); i++) {
            Attribute actualAttribute = actual.get(i);
            Attribute expectedAttribute = expected.get(i);

            Assert.assertEquals(actualAttribute.getId(), expectedAttribute.getId());
            Assert.assertEquals(actualAttribute.getKey(), expectedAttribute.getKey());
            Assert.assertEquals(actualAttribute.getSegmentId(), expectedAttribute.getSegmentId());
        }
    }

    /**
     * Asserts that the provided event configs are equivalent.
     */
    private static void verifyEvents(List<EventType> actual, List<EventType> expected) {
        Assert.assertEquals(actual.size(), expected.size());

        for (int i = 0; i < actual.size(); i++) {
            EventType actualEvent = actual.get(i);
            EventType expectedEvent = expected.get(i);

            Assert.assertEquals(actualEvent.getExperimentIds(), expectedEvent.getExperimentIds());
            Assert.assertEquals(actualEvent.getId(), expectedEvent.getId());
            Assert.assertEquals(actualEvent.getKey(), expectedEvent.getKey());
        }
    }

    /**
     * Asserts that the provided audience configs are equivalent.
     */
    private static void verifyAudiences(List<Audience> actual, List<Audience> expected) {
        Assert.assertEquals(actual.size(), expected.size());

        for (int i = 0; i < actual.size(); i++) {
            Audience actualAudience = actual.get(i);
            Audience expectedAudience = expected.get(i);

            Assert.assertEquals(actualAudience.getId(), expectedAudience.getId());
            Assert.assertEquals(actualAudience.getKey(), expectedAudience.getKey());
            Assert.assertEquals(actualAudience.getConditions(), expectedAudience.getConditions());
        }
    }

    /**
     * Assert that the provided group configs are equivalent.
     */
    private static void verifyGroups(List<Group> actual, List<Group> expected) {
        Assert.assertEquals(actual.size(), expected.size());

        for (int i = 0; i < actual.size(); i++) {
            Group actualGroup = actual.get(i);
            Group expectedGroup = expected.get(i);

            Assert.assertEquals(actualGroup.getId(), expectedGroup.getId());
            Assert.assertEquals(actualGroup.getPolicy(), expectedGroup.getPolicy());
            verifyTrafficAllocations(actualGroup.getTrafficAllocation(), expectedGroup.getTrafficAllocation());
            verifyExperiments(actualGroup.getExperiments(), expectedGroup.getExperiments());
        }
    }

    private static void verifyRollouts(List<Rollout> actual, List<Rollout> expected) {
        if (expected == null) {
            assertNull(actual);
        } else {
            assertEquals(expected.size(), actual.size());

            for (int i = 0; i < actual.size(); i++) {
                Rollout actualRollout = actual.get(i);
                Rollout expectedRollout = expected.get(i);

                assertEquals(expectedRollout.getId(), actualRollout.getId());
                verifyExperiments(actualRollout.getExperiments(), expectedRollout.getExperiments());
            }
        }
    }

    /**
     * Verify that the provided variation-level feature variable usage instances are equivalent.
     */
    private static void verifyFeatureVariableInstances(List<FeatureVariableUsageInstance> actual,
                                                    List<FeatureVariableUsageInstance> expected) {
        // if using V2, feature variable instances will be null
        if (expected == null) {
            assertNull(actual);
        } else {
            Assert.assertEquals(actual.size(), expected.size());

            for (int i = 0; i < actual.size(); i++) {
                FeatureVariableUsageInstance actualFeatureVariableUsageInstance = actual.get(i);
                FeatureVariableUsageInstance expectedFeatureVariableUsageInstance = expected.get(i);

                Assert.assertEquals(actualFeatureVariableUsageInstance.getId(), expectedFeatureVariableUsageInstance.getId());
                Assert.assertEquals(actualFeatureVariableUsageInstance.getValue(), expectedFeatureVariableUsageInstance.getValue());
            }
        }
    }

    public static <T> List<T> createListOfObjects(T... elements) {
        ArrayList<T> list = new ArrayList<T>(elements.length);
        for (T element : elements) {
            list.add(element);
        }
        return list;
    }

    public static <K, V> Map<K, V> createMapOfObjects(List<K> keys, List<V> values) {
        HashMap<K, V> map = new HashMap<K, V>(keys.size());
        if (keys.size() == values.size()) {
            Iterator<K> keysIterator = keys.iterator();
            Iterator<V> valuesIterator = values.iterator();
            while (keysIterator.hasNext() && valuesIterator.hasNext()) {
                K key = keysIterator.next();
                V value = valuesIterator.next();
                map.put(key, value);
            }
        }
        return map;
    }
}
