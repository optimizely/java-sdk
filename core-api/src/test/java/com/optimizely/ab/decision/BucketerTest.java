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

import ch.qos.logback.classic.Level;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.decision.experiment.CompositeExperimentDecisionService;
import com.optimizely.ab.decision.experiment.IExperimentDecisionService;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV2;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.optimizely.ab.event.internal.UserContext;
import com.optimizely.ab.internal.LogbackVerifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class BucketerTest {

    private IExperimentDecisionService decisionService;
    private ProjectConfig projectConfig;
    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Before
    public void setUp() {
        projectConfig = validProjectConfigV2();
    }


    /**
     * Verify that {@link Bucketer#bucket(Experiment, String, ProjectConfig)} returns the proper variation when a user is
     * in the group experiment.
     */
    @Test
    public void bucketUserInExperiment() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        bucketValue.set(3000);
        Bucketer bucketer = testBucketAlgorithm(bucketValue);
        decisionService = new CompositeExperimentDecisionService(bucketer);
        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        logbackVerifier.expectMessage(Level.DEBUG,
            "Assigned bucket 3000 to user with bucketingId \"blah\" during experiment bucketing.");
        logbackVerifier.expectMessage(Level.INFO, "User with bucketingId \"blah\" is in experiment \"group_etag2\" of group 42.");
        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket 3000 to user with bucketingId \"blah\" when bucketing to a variation.");
        logbackVerifier.expectMessage(Level.INFO,
            "User with bucketingId \"blah\" is in variation \"e2_vtag1\" of experiment \"group_etag2\".");
        UserContext userContext = new UserContext.Builder()
            .withUserId("blah")
            .withProjectConfig(projectConfig)
            .build();

        assertThat(decisionService.getDecision(groupExperiment, userContext).variation, is(groupExperiment.getVariations().get(0)));
    }

    @Test
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public void testBucketWithNullBucketingId() {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer bucketer = testBucketAlgorithm(bucketValue);
        bucketValue.set(0);
        decisionService = new CompositeExperimentDecisionService(bucketer);
        List<Experiment> groupExperiments = projectConfig.getGroups().get(1).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        UserContext userContext = new UserContext.Builder()
            .withUserId(null)
            .withProjectConfig(projectConfig)
            .build();
        try {
            decisionService.getDecision(groupExperiment, userContext);
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testBucketWithBucketingId() {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer bucketer = testBucketAlgorithm(bucketValue);
        bucketValue.set(0);
        String bucketingId = "blah";
        String userId = "blahUser";
        decisionService = new CompositeExperimentDecisionService(bucketer);
        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(1).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        Variation expectedVariation = groupExperiment.getVariations().get(0);
        UserContext userContext = new UserContext.Builder()
            .withUserId(bucketingId)
            .withProjectConfig(projectConfig)
            .build();
        logbackVerifier.expectMessage(
            Level.INFO,
            "User with bucketingId \"" + bucketingId + "\" is in variation \"e1_vtag1\" of experiment \"overlapping_etag1\".");
        assertThat(decisionService.getDecision(groupExperiment, userContext).variation, is(expectedVariation));

    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String, ProjectConfig)} doesn't return a variation when the user doesn't fall
     * into an experiment within an overlapping group.
     */
    @Test
    public void bucketUserNotInOverlappingGroupExperiment() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer bucketer = testBucketAlgorithm(bucketValue);
        bucketValue.set(3000);
        decisionService = new CompositeExperimentDecisionService(bucketer);
        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(1).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);

        logbackVerifier.expectMessage(Level.INFO,
            "User with bucketingId \"blah\" is not in any variation of experiment \"overlapping_etag1\".");
        UserContext userContext = new UserContext.Builder()
            .withUserId("blah")
            .withProjectConfig(projectConfig)
            .build();
        assertNull(decisionService.getDecision(groupExperiment, userContext).variation);
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String, ProjectConfig)} returns a variation when the user falls into an
     * experiment within an overlapping group.
     */
    @Test
    public void bucketUserToVariationInOverlappingGroupExperiment() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer bucketer = testBucketAlgorithm(bucketValue);
        bucketValue.set(0);
        decisionService = new CompositeExperimentDecisionService(bucketer);
        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(1).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        Variation expectedVariation = groupExperiment.getVariations().get(0);
        UserContext userContext = new UserContext.Builder()
            .withUserId("blah")
            .withProjectConfig(projectConfig)
            .build();
        logbackVerifier.expectMessage(
            Level.INFO,
            "User with bucketingId \"blah\" is in variation \"e1_vtag1\" of experiment \"overlapping_etag1\".");
        assertThat(decisionService.getDecision(groupExperiment, userContext).variation, is(expectedVariation));
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String, ProjectConfig)} doesn't return a variation when the user is bucketed to
     * the traffic space of a deleted experiment within a random group.
     */
    @Test
    public void bucketUserToDeletedExperimentSpace() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        final int bucketIntVal = 9000;
        Bucketer bucketer = testBucketAlgorithm(bucketValue);
        bucketValue.set(bucketIntVal);
        decisionService = new CompositeExperimentDecisionService(bucketer);
        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(1);

        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket " + bucketIntVal + " to user with bucketingId \"blah\" during experiment bucketing.");
        logbackVerifier.expectMessage(Level.INFO, "User with bucketingId \"blah\" is not in any experiment of group 42.");
        UserContext userContext = new UserContext.Builder()
            .withUserId("blah")
            .withProjectConfig(projectConfig)
            .build();
        assertNull(decisionService.getDecision(groupExperiment, userContext).variation);
    }

    /**
     * Verify that {@link Bucketer#bucket(Experiment, String, ProjectConfig)} doesn't return a variation when a user isn't bucketed
     * into the group experiment.
     */
    @Test
    public void bucketUserNotInExperiment() throws Exception {
        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer bucketer = testBucketAlgorithm(bucketValue);
        bucketValue.set(3000);
        decisionService = new CompositeExperimentDecisionService(bucketer);
        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(1);
        // the user should be bucketed to a different experiment than the one provided, resulting in no variation being
        // returned.
        logbackVerifier.expectMessage(Level.DEBUG,
            "Assigned bucket 3000 to user with bucketingId \"blah\" during experiment bucketing.");
        logbackVerifier.expectMessage(Level.INFO,
            "User with bucketingId \"blah\" is not in experiment \"group_etag1\" of group 42");
        UserContext userContext = new UserContext.Builder()
            .withUserId("blah")
            .withProjectConfig(projectConfig)
            .build();
        assertNull(decisionService.getDecision(groupExperiment, userContext).variation);
    }

    /**
     * Verify that in certain cases, users aren't assigned any variation and null is returned.
     */
    @Test
    public void bucketToControl() throws Exception {
        String bucketingId = "blah";
        String userId = "user1";

        List<String> audienceIds = Collections.emptyList();

        List<Variation> variations = Collections.singletonList(
            new Variation("1", "var1")
        );

        List<TrafficAllocation> trafficAllocations = Collections.singletonList(
            new TrafficAllocation("1", 999)
        );

        Experiment experiment = new Experiment("1234", "exp_key", "Running", "1", audienceIds, null, variations,
            Collections.<String, String>emptyMap(), trafficAllocations, "");

        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer bucketer = testBucketAlgorithm(bucketValue);

        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket 0 to user with bucketingId \"" + bucketingId + "\" when bucketing to a variation.");
        logbackVerifier.expectMessage(Level.INFO, "User with bucketingId \"" + bucketingId + "\" is in variation \"var1\" of experiment \"exp_key\".");

        // verify bucketing to the first variation
        bucketValue.set(0);
        decisionService = new CompositeExperimentDecisionService(bucketer);
        UserContext userContext = new UserContext.Builder()
            .withUserId(bucketingId)
            .withProjectConfig(projectConfig)
            .build();
        assertThat(decisionService.getDecision(experiment, userContext).variation, is(variations.get(0)));

        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket 1000 to user with bucketingId \"" + bucketingId + "\" when bucketing to a variation.");
        logbackVerifier.expectMessage(Level.INFO, "User with bucketingId \"" + bucketingId + "\" is not in any variation of experiment \"exp_key\".");

        // verify bucketing to no variation (null)
        bucketValue.set(1000);
        assertNull(decisionService.getDecision(experiment, userContext).variation);
    }

    /**
     * Given an experiment with 4 variations, verify that bucket values are correctly mapped to the associated range.
     */
    @Test
    public void bucketToMultipleVariations() throws Exception {
        List<String> audienceIds = Collections.emptyList();

        // create an experiment with 4 variations using ranges: [0 -> 999, 1000 -> 4999, 5000 -> 5999, 6000 -> 9999]
        List<Variation> variations = Arrays.asList(
            new Variation("1", "var1"),
            new Variation("2", "var2"),
            new Variation("3", "var3"),
            new Variation("4", "var4")
        );

        List<TrafficAllocation> trafficAllocations = Arrays.asList(
            new TrafficAllocation("1", 1000),
            new TrafficAllocation("2", 5000),
            new TrafficAllocation("3", 6000),
            new TrafficAllocation("4", 10000)
        );

        Experiment experiment = new Experiment("1234", "exp_key", "Running", "1", audienceIds, null, variations,
            Collections.<String, String>emptyMap(), trafficAllocations, "");

        final AtomicInteger bucketValue = new AtomicInteger();
        Bucketer bucketer = testBucketAlgorithm(bucketValue);
        decisionService = new CompositeExperimentDecisionService(bucketer);
        // verify bucketing to the first variation
        bucketValue.set(0);
        assertThat(decisionService.getDecision(experiment, new UserContext.Builder()
            .withUserId("user1")
            .withProjectConfig(projectConfig)
            .build()).variation, is(variations.get(0)));
        bucketValue.set(500);
        assertThat(decisionService.getDecision(experiment, new UserContext.Builder()
            .withUserId("user2")
            .withProjectConfig(projectConfig)
            .build()).variation, is(variations.get(0)));
        bucketValue.set(999);
        assertThat(decisionService.getDecision(experiment, new UserContext.Builder()
            .withUserId("user3")
            .withProjectConfig(projectConfig)
            .build()).variation, is(variations.get(0)));

        // verify the second variation
        bucketValue.set(1000);
        assertThat(decisionService.getDecision(experiment, new UserContext.Builder()
            .withUserId("user4")
            .withProjectConfig(projectConfig)
            .build()).variation, is(variations.get(1)));
        bucketValue.set(4000);
        assertThat(decisionService.getDecision(experiment, new UserContext.Builder()
            .withUserId("user5")
            .withProjectConfig(projectConfig)
            .build()).variation, is(variations.get(1)));
        bucketValue.set(4999);
        assertThat(decisionService.getDecision(experiment, new UserContext.Builder()
            .withUserId("user6")
            .withProjectConfig(projectConfig)
            .build()).variation, is(variations.get(1)));

        // ...and the rest
        bucketValue.set(5100);
        assertThat(decisionService.getDecision(experiment, new UserContext.Builder()
            .withUserId("user7")
            .withProjectConfig(projectConfig)
            .build()).variation, is(variations.get(2)));
        bucketValue.set(6500);
        assertThat(decisionService.getDecision(experiment, new UserContext.Builder()
            .withUserId("user8")
            .withProjectConfig(projectConfig)
            .build()).variation, is(variations.get(3)));
    }

    //======== Helper methods ========//

    /**
     * Sets up a mock algorithm that returns an expected bucket value.
     *
     * @param bucketValue the expected bucket value holder
     * @return the mock bucket algorithm
     */
    private static Bucketer testBucketAlgorithm(final AtomicInteger bucketValue) {
        return new Bucketer() {
            @Override
            public int generateBucketValue(int hashCode) {
                return bucketValue.get();
            }
        };
    }
}
