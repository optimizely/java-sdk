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
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.decision.audience.FullStackAudienceEvaluator;
import com.optimizely.ab.decision.bucketer.Bucketer;
import com.optimizely.ab.decision.bucketer.MurmurhashBucketer;
import com.optimizely.ab.decision.experiment.ExperimentDecisionService;
import com.optimizely.ab.decision.experiment.service.ExperimentBucketerService;
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

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV2;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ExperimentBucketerTest {

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    private ProjectConfig projectConfig;
    private ExperimentDecisionService experimentBucketerService;

    @Before
    public void setUp() {
        projectConfig = validProjectConfigV2();
    }

    /**
     * Given an experiment with 4 variations, verify that bucket values are correctly mapped to the associated range.
     */
    @Test
    public void bucketToMultipleVariations() {
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

        Experiment experiment = new Experiment("1234",
            "exp_key",
            "Running",
            "1",
            audienceIds,
            null,
            variations,
            Collections.<String, String>emptyMap(), trafficAllocations, "");

        final AtomicInteger bucketValue = new AtomicInteger();
        experimentBucketerService = new ExperimentBucketerService.Builder()
            .withBucketer(testBucketAlgorithm(bucketValue))
            .withAudienceEvaluator(new FullStackAudienceEvaluator())
            .build();

        // verify bucketing to the first variation
        bucketValue.set(0);
        UserContext userContext = new UserContext.Builder()
            .withUserId("user1")
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(experiment, userContext).variation, is(variations.get(0)));
        bucketValue.set(500);
        userContext = new UserContext.Builder()
            .withUserId("user2")
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(experiment, userContext).variation, is(variations.get(0)));
        bucketValue.set(999);
        userContext = new UserContext.Builder()
            .withUserId("user3")
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(experiment, userContext).variation, is(variations.get(0)));

        // verify the second variation
        bucketValue.set(1000);
        userContext = new UserContext.Builder()
            .withUserId("user4")
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(experiment, userContext).variation, is(variations.get(1)));
        bucketValue.set(4000);
        userContext = new UserContext.Builder()
            .withUserId("user5")
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(experiment, userContext).variation, is(variations.get(1)));
        bucketValue.set(4999);
        userContext = new UserContext.Builder()
            .withUserId("user6")
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(experiment, userContext).variation, is(variations.get(1)));

        // ...and the rest
        bucketValue.set(5100);
        userContext = new UserContext.Builder()
            .withUserId("user7")
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(experiment, userContext).variation, is(variations.get(2)));
        bucketValue.set(6500);
        userContext = new UserContext.Builder()
            .withUserId("user8")
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(experiment, userContext).variation, is(variations.get(3)));
    }

    /**
     * Verify that in certain cases, users aren't assigned any variation and null is returned.
     */
    @Test
    public void bucketToControl() {
        String bucketingId = "blah";

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
        experimentBucketerService = new ExperimentBucketerService.Builder()
            .withBucketer(testBucketAlgorithm(bucketValue))
            .withAudienceEvaluator(new FullStackAudienceEvaluator())
            .build();

        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket 0 to user with bucketingId \"" + bucketingId + "\" when bucketing to a variation.");
        logbackVerifier.expectMessage(Level.INFO, "User with bucketingId \"" + bucketingId + "\" is in variation \"var1\" of experiment \"exp_key\".");

        // verify bucketing to the first variation
        bucketValue.set(0);
        UserContext userContext = new UserContext.Builder()
            .withUserId(bucketingId)
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(experiment, userContext).variation, is(variations.get(0)));

        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket 1000 to user with bucketingId \"" + bucketingId + "\" when bucketing to a variation.");
        logbackVerifier.expectMessage(Level.INFO, "User with bucketingId \"" + bucketingId + "\" is not in any variation of experiment \"exp_key\".");

        // verify bucketing to no variation (null)
        bucketValue.set(1000);
        userContext = new UserContext.Builder()
            .withUserId(bucketingId)
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertNull(experimentBucketerService.getDecision(experiment, userContext).variation);
    }

    /**
     * Verify that {@link ExperimentBucketerService#getDecision(Experiment, UserContext)} returns the proper variation when a user is
     * in the group experiment.
     */
    @Test
    public void bucketUserInExperiment() {
        final AtomicInteger bucketValue = new AtomicInteger();
        experimentBucketerService = new ExperimentBucketerService.Builder()
            .withBucketer(testBucketAlgorithm(bucketValue))
            .withAudienceEvaluator(new FullStackAudienceEvaluator())
            .build();
        bucketValue.set(3000);

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
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(groupExperiment, userContext).variation, is(groupExperiment.getVariations().get(0)));
    }

    /**
     * Verify that {@link ExperimentBucketerService#getDecision(Experiment, UserContext)} doesn't return a variation when a user isn't bucketed
     * into the group experiment.
     */
    @Test
    public void bucketUserNotInExperiment() {
        final AtomicInteger bucketValue = new AtomicInteger();
        experimentBucketerService = new ExperimentBucketerService.Builder()
            .withBucketer(testBucketAlgorithm(bucketValue))
            .withAudienceEvaluator(new FullStackAudienceEvaluator())
            .build();
        bucketValue.set(3000);

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
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertNull(experimentBucketerService.getDecision(groupExperiment, userContext).variation);
    }

    /**
     * Verify that {@link ExperimentBucketerService#getDecision(Experiment, UserContext)} doesn't return a variation when the user is bucketed to
     * the traffic space of a deleted experiment within a random group.
     */
    @Test
    public void bucketUserToDeletedExperimentSpace() {
        final AtomicInteger bucketValue = new AtomicInteger();
        final int bucketIntVal = 9000;
        experimentBucketerService = new ExperimentBucketerService.Builder()
            .withBucketer(testBucketAlgorithm(bucketValue))
            .withAudienceEvaluator(new FullStackAudienceEvaluator())
            .build();
        bucketValue.set(bucketIntVal);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(0).getExperiments();
        Experiment groupExperiment = groupExperiments.get(1);

        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket " + bucketIntVal + " to user with bucketingId \"blah\" during experiment bucketing.");
        logbackVerifier.expectMessage(Level.INFO, "User with bucketingId \"blah\" is not in any experiment of group 42.");
        UserContext userContext = new UserContext.Builder()
            .withUserId("blah")
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertNull(experimentBucketerService.getDecision(groupExperiment, userContext).variation);
    }

    /**
     * Verify that {@link ExperimentBucketerService#getDecision(Experiment, UserContext)} returns a variation when the user falls into an
     * experiment within an overlapping group.
     */
    @Test
    public void bucketUserToVariationInOverlappingGroupExperiment()  {
        final AtomicInteger bucketValue = new AtomicInteger();
        experimentBucketerService = new ExperimentBucketerService.Builder()
            .withBucketer(testBucketAlgorithm(bucketValue))
            .withAudienceEvaluator(new FullStackAudienceEvaluator())
            .build();
        bucketValue.set(0);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(1).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        Variation expectedVariation = groupExperiment.getVariations().get(0);

        logbackVerifier.expectMessage(
            Level.INFO,
            "User with bucketingId \"blah\" is in variation \"e1_vtag1\" of experiment \"overlapping_etag1\".");
        UserContext userContext = new UserContext.Builder()
            .withUserId("blah")
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(groupExperiment, userContext).variation, is(expectedVariation));
    }

    /**
     * Verify that {@link ExperimentBucketerService#getDecision(Experiment, UserContext)} doesn't return a variation when the user doesn't fall
     * into an experiment within an overlapping group.
     */
    @Test
    public void bucketUserNotInOverlappingGroupExperiment() {
        final AtomicInteger bucketValue = new AtomicInteger();
        experimentBucketerService = new ExperimentBucketerService.Builder()
            .withBucketer(testBucketAlgorithm(bucketValue))
            .withAudienceEvaluator(new FullStackAudienceEvaluator())
            .build();
        bucketValue.set(3000);

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(1).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);

        logbackVerifier.expectMessage(Level.INFO,
            "User with bucketingId \"blah\" is not in any variation of experiment \"overlapping_etag1\".");
        UserContext userContext = new UserContext.Builder()
            .withUserId("blah")
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertNull(experimentBucketerService.getDecision(groupExperiment, userContext).variation);
    }

    @Test
    public void testBucketWithBucketingId() {
        final AtomicInteger bucketValue = new AtomicInteger();
        experimentBucketerService = new ExperimentBucketerService.Builder()
            .withBucketer(testBucketAlgorithm(bucketValue))
            .withAudienceEvaluator(new FullStackAudienceEvaluator())
            .build();
        bucketValue.set(0);
        String bucketingId = "blah";
        String userId = "blahUser";

        ProjectConfig projectConfig = validProjectConfigV2();
        List<Experiment> groupExperiments = projectConfig.getGroups().get(1).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        Variation expectedVariation = groupExperiment.getVariations().get(0);

        logbackVerifier.expectMessage(
            Level.INFO,
            "User with bucketingId \"" + bucketingId + "\" is in variation \"e1_vtag1\" of experiment \"overlapping_etag1\".");
        UserContext userContext = new UserContext.Builder()
            .withUserId(bucketingId)
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        assertThat(experimentBucketerService.getDecision(groupExperiment, userContext).variation, is(expectedVariation));
    }

    @Test
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public void testBucketWithNullBucketingId() {
        final AtomicInteger bucketValue = new AtomicInteger();
        experimentBucketerService = new ExperimentBucketerService.Builder()
            .withBucketer(testBucketAlgorithm(bucketValue))
            .withAudienceEvaluator(new FullStackAudienceEvaluator())
            .build();
        bucketValue.set(0);

        List<Experiment> groupExperiments = projectConfig.getGroups().get(1).getExperiments();
        Experiment groupExperiment = groupExperiments.get(0);
        UserContext userContext = new UserContext.Builder()
            .withUserId(null)
            .withProjectConfig(projectConfig)
            .withAttributes(Collections.<String, String>emptyMap())
            .build();
        try {
            experimentBucketerService.getDecision(groupExperiment, userContext);
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    /**
     * Sets up a mock algorithm that returns an expected bucket value.
     *
     * @param bucketValue the expected bucket value holder
     * @return the mock bucket algorithm
     */
    private static Bucketer testBucketAlgorithm(final AtomicInteger bucketValue) {
        return new MurmurhashBucketer() {
            @Override
            public int generateBucketValue(int hashCode) {
                return bucketValue.get();
            }
        };
    }
}
