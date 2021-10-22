/****************************************************************************
 * Copyright 2017-2020, Optimizely, Inc. and contributors                   *
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
package com.optimizely.ab.bucketing;

import ch.qos.logback.classic.Level;
import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.config.*;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.internal.ControlAttribute;
import com.optimizely.ab.internal.LogbackVerifier;
import com.optimizely.ab.optimizelydecision.DecisionResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.util.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.*;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.*;
import static com.optimizely.ab.config.ValidProjectConfigV4.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class DecisionServiceTest {

    private static final String genericUserId = "genericUserId";
    private static final String whitelistedUserId = "testUser1";
    private static final String userProfileId = "userProfileId";

    @Rule
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ErrorHandler mockErrorHandler;

    private ProjectConfig noAudienceProjectConfig;
    private ProjectConfig v4ProjectConfig;
    private ProjectConfig validProjectConfig;
    private Experiment whitelistedExperiment;
    private Variation whitelistedVariation;
    private DecisionService decisionService;

    private Optimizely optimizely;

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Before
    public void setUp() throws Exception {
        validProjectConfig = validProjectConfigV3();
        v4ProjectConfig = validProjectConfigV4();
        noAudienceProjectConfig = noAudienceProjectConfigV3();
        whitelistedExperiment = validProjectConfig.getExperimentIdMapping().get("223");
        whitelistedVariation = whitelistedExperiment.getVariationKeyToVariationMap().get("vtag1");
        Bucketer bucketer = new Bucketer();
        decisionService = spy(new DecisionService(bucketer, mockErrorHandler, null));
        this.optimizely = Optimizely.builder().build();
    }


    //========= getVariation tests =========/

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, OptimizelyUserContext, ProjectConfig)}
     * gives precedence to forced variation bucketing over audience evaluation.
     */
    @Test
    public void getVariationWhitelistedPrecedesAudienceEval() throws Exception {
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation expectedVariation = experiment.getVariations().get(0);

        // user excluded without audiences and whitelisting
        assertNull(decisionService.getVariation(
            experiment,
            optimizely.createUserContext(
                genericUserId,
                Collections.emptyMap()),
            validProjectConfig).getResult());

        logbackVerifier.expectMessage(Level.INFO, "User \"" + whitelistedUserId + "\" is forced in variation \"vtag1\".");

        // no attributes provided for a experiment that has an audience
        assertThat(decisionService.getVariation(experiment, optimizely.createUserContext(whitelistedUserId, Collections.emptyMap()), validProjectConfig).getResult(), is(expectedVariation));

        verify(decisionService).getWhitelistedVariation(eq(experiment), eq(whitelistedUserId));
        verify(decisionService, never()).getStoredVariation(eq(experiment), any(UserProfile.class), any(ProjectConfig.class));
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, OptimizelyUserContext, ProjectConfig)}
     * gives precedence to forced variation bucketing over whitelisting.
     */
    @Test
    public void getForcedVariationBeforeWhitelisting() throws Exception {
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation whitelistVariation = experiment.getVariations().get(0);
        Variation expectedVariation = experiment.getVariations().get(1);

        // user excluded without audiences and whitelisting
        assertNull(decisionService.getVariation(experiment, optimizely.createUserContext(genericUserId, Collections.<String, Object>emptyMap()), validProjectConfig).getResult());

        // set the runtimeForcedVariation
        decisionService.setForcedVariation(experiment, whitelistedUserId, expectedVariation.getKey());
        // no attributes provided for a experiment that has an audience
        assertThat(decisionService.getVariation(experiment, optimizely.createUserContext(whitelistedUserId, Collections.<String, Object>emptyMap()), validProjectConfig).getResult(), is(expectedVariation));

        //verify(decisionService).getForcedVariation(experiment.getKey(), whitelistedUserId);
        verify(decisionService, never()).getStoredVariation(eq(experiment), any(UserProfile.class), any(ProjectConfig.class));
        assertEquals(decisionService.getWhitelistedVariation(experiment, whitelistedUserId).getResult(), whitelistVariation);
        assertTrue(decisionService.setForcedVariation(experiment, whitelistedUserId, null));
        assertNull(decisionService.getForcedVariation(experiment, whitelistedUserId).getResult());
        assertThat(decisionService.getVariation(experiment, optimizely.createUserContext(whitelistedUserId, Collections.<String, Object>emptyMap()), validProjectConfig).getResult(), is(whitelistVariation));
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, OptimizelyUserContext, ProjectConfig)}
     * gives precedence to forced variation bucketing over audience evaluation.
     */
    @Test
    public void getVariationForcedPrecedesAudienceEval() throws Exception {
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation expectedVariation = experiment.getVariations().get(1);

        // user excluded without audiences and whitelisting
        assertNull(decisionService.getVariation(experiment, optimizely.createUserContext(genericUserId, Collections.<String, Object>emptyMap()), validProjectConfig).getResult());

        // set the runtimeForcedVariation
        decisionService.setForcedVariation(experiment, genericUserId, expectedVariation.getKey());
        // no attributes provided for a experiment that has an audience
        assertThat(decisionService.getVariation(experiment, optimizely.createUserContext(genericUserId, Collections.<String, Object>emptyMap()), validProjectConfig).getResult(), is(expectedVariation));

        verify(decisionService, never()).getStoredVariation(eq(experiment), any(UserProfile.class), eq(validProjectConfig));
        assertEquals(decisionService.setForcedVariation(experiment, genericUserId, null), true);
        assertNull(decisionService.getForcedVariation(experiment, genericUserId).getResult());
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, OptimizelyUserContext, ProjectConfig)}
     * gives precedence to forced variation bucketing over user profile.
     */
    @Test
    public void getVariationForcedBeforeUserProfile() throws Exception {
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation variation = experiment.getVariations().get(0);
        Decision decision = new Decision(variation.getId());
        UserProfile userProfile = new UserProfile(userProfileId,
            Collections.singletonMap(experiment.getId(), decision));
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap());

        DecisionService decisionService = spy(new DecisionService(new Bucketer(), mockErrorHandler, userProfileService));

        // ensure that normal users still get excluded from the experiment when they fail audience evaluation
        assertNull(decisionService.getVariation(experiment, optimizely.createUserContext(genericUserId, Collections.<String, Object>emptyMap()), validProjectConfig).getResult());

        // ensure that a user with a saved user profile, sees the same variation regardless of audience evaluation
        assertEquals(variation,
            decisionService.getVariation(experiment, optimizely.createUserContext(userProfileId, Collections.<String, Object>emptyMap()), validProjectConfig).getResult());

        Variation forcedVariation = experiment.getVariations().get(1);
        decisionService.setForcedVariation(experiment, userProfileId, forcedVariation.getKey());
        assertEquals(forcedVariation,
            decisionService.getVariation(experiment, optimizely.createUserContext(userProfileId, Collections.<String, Object>emptyMap()), validProjectConfig).getResult());
        assertTrue(decisionService.setForcedVariation(experiment, userProfileId, null));
        assertNull(decisionService.getForcedVariation(experiment, userProfileId).getResult());
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, OptimizelyUserContext, ProjectConfig)}
     * gives precedence to user profile over audience evaluation.
     */
    @Test
    public void getVariationEvaluatesUserProfileBeforeAudienceTargeting() throws Exception {
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation variation = experiment.getVariations().get(0);
        Decision decision = new Decision(variation.getId());
        UserProfile userProfile = new UserProfile(userProfileId,
            Collections.singletonMap(experiment.getId(), decision));
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap());

        DecisionService decisionService = spy(new DecisionService(new Bucketer(), mockErrorHandler, userProfileService));

        // ensure that normal users still get excluded from the experiment when they fail audience evaluation
        assertNull(decisionService.getVariation(experiment, optimizely.createUserContext(genericUserId, Collections.<String, Object>emptyMap()), validProjectConfig).getResult());

        // ensure that a user with a saved user profile, sees the same variation regardless of audience evaluation
        assertEquals(variation,
            decisionService.getVariation(experiment, optimizely.createUserContext(userProfileId, Collections.<String, Object>emptyMap()), validProjectConfig).getResult());

    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, OptimizelyUserContext, ProjectConfig)}
     * gives a null variation on a Experiment that is not running. Set the forced variation.
     * And, test to make sure that after setting forced variation, the getVariation still returns
     * null.
     */
    @Test
    public void getVariationOnNonRunningExperimentWithForcedVariation() {
        Experiment experiment = validProjectConfig.getExperiments().get(1);
        assertFalse(experiment.isRunning());
        Variation variation = experiment.getVariations().get(0);

        // ensure that the not running variation returns null with no forced variation set.
        assertNull(decisionService.getVariation(experiment, optimizely.createUserContext("userId", Collections.<String, Object>emptyMap()), validProjectConfig).getResult());

        // we call getVariation 3 times on an experiment that is not running.
        logbackVerifier.expectMessage(Level.INFO,
            "Experiment \"etag2\" is not running.", 3);

        // set a forced variation on the user that got back null
        assertTrue(decisionService.setForcedVariation(experiment, "userId", variation.getKey()));

        // ensure that a user with a forced variation set
        // still gets back a null variation if the variation is not running.
        assertNull(decisionService.getVariation(experiment, optimizely.createUserContext("userId", Collections.<String, Object>emptyMap()), validProjectConfig).getResult());

        // set the forced variation back to null
        assertTrue(decisionService.setForcedVariation(experiment, "userId", null));
        // test one more time that the getVariation returns null for the experiment that is not running.
        assertNull(decisionService.getVariation(experiment, optimizely.createUserContext("userid", Collections.<String, Object>emptyMap()), validProjectConfig).getResult());


    }

    //========== get Variation for Feature tests ==========//

    /**
     * Verify that {@link DecisionService#getVariationForFeature(FeatureFlag, OptimizelyUserContext, ProjectConfig)}
     * returns null when the {@link FeatureFlag} is not used in any experiments or rollouts.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void getVariationForFeatureReturnsNullWhenFeatureFlagExperimentIdsIsEmpty() {
        FeatureFlag emptyFeatureFlag = mock(FeatureFlag.class);
        when(emptyFeatureFlag.getExperimentIds()).thenReturn(Collections.<String>emptyList());
        String featureKey = "testFeatureFlagKey";
        when(emptyFeatureFlag.getKey()).thenReturn(featureKey);
        when(emptyFeatureFlag.getRolloutId()).thenReturn("");

        logbackVerifier.expectMessage(Level.INFO,
            "The feature flag \"" + featureKey + "\" is not used in any experiments.");
        logbackVerifier.expectMessage(Level.INFO,
            "The feature flag \"" + featureKey + "\" is not used in a rollout.");
        logbackVerifier.expectMessage(Level.INFO,
            "The user \"" + genericUserId + "\" was not bucketed into a rollout for feature flag \"" +
                featureKey + "\".");

        FeatureDecision featureDecision = decisionService.getVariationForFeature(
            emptyFeatureFlag,
            optimizely.createUserContext(genericUserId, Collections.<String, Object>emptyMap()),
            validProjectConfig).getResult();
        assertNull(featureDecision.variation);
        assertNull(featureDecision.decisionSource);

        verify(emptyFeatureFlag, times(1)).getExperimentIds();
        verify(emptyFeatureFlag, times(1)).getRolloutId();
        verify(emptyFeatureFlag, times(3)).getKey();
    }

    /**
     * Verify that {@link DecisionService#getVariationForFeature(FeatureFlag, OptimizelyUserContext, ProjectConfig)}
     * returns null when the user is not bucketed into any experiments or rollouts for the {@link FeatureFlag}.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void getVariationForFeatureReturnsNullWhenItGetsNoVariationsForExperimentsAndRollouts() {
        FeatureFlag spyFeatureFlag = spy(FEATURE_FLAG_MULTI_VARIATE_FEATURE);

        // do not bucket to any experiments
        doReturn(DecisionResponse.nullNoReasons()).when(decisionService).getVariation(
            any(Experiment.class),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class),
            anyObject()
        );
        // do not bucket to any rollouts
        doReturn(DecisionResponse.responseNoReasons(new FeatureDecision(null, null, null))).when(decisionService).getVariationForFeatureInRollout(
            any(FeatureFlag.class),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class)
        );

        // try to get a variation back from the decision service for the feature flag
        FeatureDecision featureDecision = decisionService.getVariationForFeature(
            spyFeatureFlag,
            optimizely.createUserContext(genericUserId, Collections.emptyMap()),
            validProjectConfig
        ).getResult();
        assertNull(featureDecision.variation);
        assertNull(featureDecision.decisionSource);

        logbackVerifier.expectMessage(Level.INFO,
            "The user \"" + genericUserId + "\" was not bucketed into a rollout for feature flag \"" +
                FEATURE_MULTI_VARIATE_FEATURE_KEY + "\".");

        verify(spyFeatureFlag, times(2)).getExperimentIds();
        verify(spyFeatureFlag, times(2)).getKey();
    }

    /**
     * Verify that {@link DecisionService#getVariationForFeature(FeatureFlag, OptimizelyUserContext, ProjectConfig)}
     * returns the variation of the experiment a user gets bucketed into for an experiment.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void getVariationForFeatureReturnsVariationReturnedFromGetVariation() {
        FeatureFlag spyFeatureFlag = spy(ValidProjectConfigV4.FEATURE_FLAG_MUTEX_GROUP_FEATURE);

        doReturn(DecisionResponse.nullNoReasons()).when(decisionService).getVariation(
            eq(ValidProjectConfigV4.EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class),
            anyObject()
        );

        doReturn(DecisionResponse.responseNoReasons(ValidProjectConfigV4.VARIATION_MUTEX_GROUP_EXP_2_VAR_1)).when(decisionService).getVariation(
            eq(ValidProjectConfigV4.EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class),
            anyObject()
        );

        FeatureDecision featureDecision = decisionService.getVariationForFeature(
            spyFeatureFlag,
            optimizely.createUserContext(genericUserId, Collections.emptyMap()),
            v4ProjectConfig
        ).getResult();
        assertEquals(ValidProjectConfigV4.VARIATION_MUTEX_GROUP_EXP_2_VAR_1, featureDecision.variation);
        assertEquals(FeatureDecision.DecisionSource.FEATURE_TEST, featureDecision.decisionSource);

        verify(spyFeatureFlag, times(2)).getExperimentIds();
        verify(spyFeatureFlag, times(2)).getKey();
    }

    /**
     * Verify that when getting a {@link Variation} for a {@link FeatureFlag} in
     * {@link DecisionService#getVariationForFeature(FeatureFlag, OptimizelyUserContext, ProjectConfig)},
     * check first if the user is bucketed to an {@link Experiment}
     * then check if the user is not bucketed to an experiment,
     * check for a {@link Rollout}.
     */
    @Test
    public void getVariationForFeatureReturnsVariationFromExperimentBeforeRollout() {
        FeatureFlag featureFlag = FEATURE_FLAG_MULTI_VARIATE_FEATURE;
        Experiment featureExperiment = v4ProjectConfig.getExperimentIdMapping().get(featureFlag.getExperimentIds().get(0));
        assertNotNull(featureExperiment);
        Rollout featureRollout = v4ProjectConfig.getRolloutIdMapping().get(featureFlag.getRolloutId());
        Variation experimentVariation = featureExperiment.getVariations().get(0);
        Experiment rolloutExperiment = featureRollout.getExperiments().get(0);
        Variation rolloutVariation = rolloutExperiment.getVariations().get(0);

        // return variation for experiment
        doReturn(DecisionResponse.responseNoReasons(experimentVariation))
            .when(decisionService).getVariation(
            eq(featureExperiment),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class),
            anyObject()
        );

        // return variation for rollout
        doReturn(DecisionResponse.responseNoReasons(new FeatureDecision(rolloutExperiment, rolloutVariation, FeatureDecision.DecisionSource.ROLLOUT)))
            .when(decisionService).getVariationForFeatureInRollout(
            eq(featureFlag),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class)
        );

        // make sure we get the right variation back
        FeatureDecision featureDecision = decisionService.getVariationForFeature(
            featureFlag,
            optimizely.createUserContext(genericUserId, Collections.<String, Object>emptyMap()),
            v4ProjectConfig
        ).getResult();
        assertEquals(experimentVariation, featureDecision.variation);
        assertEquals(FeatureDecision.DecisionSource.FEATURE_TEST, featureDecision.decisionSource);

        // make sure we do not even check for rollout bucketing
        verify(decisionService, never()).getVariationForFeatureInRollout(
            any(FeatureFlag.class),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class)
        );

        // make sure we ask for experiment bucketing once
        verify(decisionService, times(1)).getVariation(
            any(Experiment.class),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class),
            anyObject()
        );
    }

    /**
     * Verify that when getting a {@link Variation} for a {@link FeatureFlag} in
     * {@link DecisionService#getVariationForFeature(FeatureFlag, OptimizelyUserContext, ProjectConfig)},
     * check first if the user is bucketed to an {@link Rollout}
     * if the user is not bucketed to an experiment.
     */
    @Test
    public void getVariationForFeatureReturnsVariationFromRolloutWhenExperimentFails() {
        FeatureFlag featureFlag = FEATURE_FLAG_MULTI_VARIATE_FEATURE;
        Experiment featureExperiment = v4ProjectConfig.getExperimentIdMapping().get(featureFlag.getExperimentIds().get(0));
        assertNotNull(featureExperiment);
        Rollout featureRollout = v4ProjectConfig.getRolloutIdMapping().get(featureFlag.getRolloutId());
        Experiment rolloutExperiment = featureRollout.getExperiments().get(0);
        Variation rolloutVariation = rolloutExperiment.getVariations().get(0);

        // return variation for experiment
        doReturn(DecisionResponse.nullNoReasons())
            .when(decisionService).getVariation(
            eq(featureExperiment),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class),
            anyObject()
        );

        // return variation for rollout
        doReturn(DecisionResponse.responseNoReasons(new FeatureDecision(rolloutExperiment, rolloutVariation, FeatureDecision.DecisionSource.ROLLOUT)))
            .when(decisionService).getVariationForFeatureInRollout(
            eq(featureFlag),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class)
        );

        // make sure we get the right variation back
        FeatureDecision featureDecision = decisionService.getVariationForFeature(
            featureFlag,
            optimizely.createUserContext(genericUserId, Collections.<String, Object>emptyMap()),
            v4ProjectConfig
        ).getResult();
        assertEquals(rolloutVariation, featureDecision.variation);
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT, featureDecision.decisionSource);

        // make sure we do not even check for rollout bucketing
        verify(decisionService, times(1)).getVariationForFeatureInRollout(
            any(FeatureFlag.class),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class)
        );

        // make sure we ask for experiment bucketing once
        verify(decisionService, times(1)).getVariation(
            any(Experiment.class),
            any(OptimizelyUserContext.class),
            any(ProjectConfig.class),
            anyObject()
        );

        logbackVerifier.expectMessage(
            Level.INFO,
            "The user \"" + genericUserId + "\" was bucketed into a rollout for feature flag \"" +
                featureFlag.getKey() + "\"."
        );
    }

    //========== getVariationForFeatureInRollout tests ==========//

    /**
     * Verify that {@link DecisionService#getVariationForFeatureInRollout(FeatureFlag, OptimizelyUserContext, ProjectConfig)}
     * returns null when trying to bucket a user into a {@link FeatureFlag}
     * that does not have a {@link Rollout} attached.
     */
    @Test
    public void getVariationForFeatureInRolloutReturnsNullWhenFeatureIsNotAttachedToRollout() {
        FeatureFlag mockFeatureFlag = mock(FeatureFlag.class);
        when(mockFeatureFlag.getRolloutId()).thenReturn("");
        String featureKey = "featureKey";
        when(mockFeatureFlag.getKey()).thenReturn(featureKey);

        FeatureDecision featureDecision = decisionService.getVariationForFeatureInRollout(
            mockFeatureFlag,
            optimizely.createUserContext(genericUserId, Collections.<String, Object>emptyMap()),
            validProjectConfig
        ).getResult();
        assertNull(featureDecision.variation);
        assertNull(featureDecision.decisionSource);

        logbackVerifier.expectMessage(
            Level.INFO,
            "The feature flag \"" + featureKey + "\" is not used in a rollout."
        );
    }

    /**
     * Verify that {@link DecisionService#getVariationForFeatureInRollout(FeatureFlag, OptimizelyUserContext, ProjectConfig)}
     * return null when a user is excluded from every rule of a rollout due to traffic allocation.
     */
    @Test
    public void getVariationForFeatureInRolloutReturnsNullWhenUserIsExcludedFromAllTraffic() {
        Bucketer mockBucketer = mock(Bucketer.class);
        when(mockBucketer.bucket(any(Experiment.class), anyString(), any(ProjectConfig.class))).thenReturn(DecisionResponse.nullNoReasons());

        DecisionService decisionService = new DecisionService(
            mockBucketer,
            mockErrorHandler,
            null
        );

        FeatureDecision featureDecision = decisionService.getVariationForFeatureInRollout(
            FEATURE_FLAG_MULTI_VARIATE_FEATURE,
            optimizely.createUserContext(genericUserId, Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            v4ProjectConfig
        ).getResult();
        assertNull(featureDecision.variation);
        assertNull(featureDecision.decisionSource);

        // with fall back bucketing, the user has at most 2 chances to get bucketed with traffic allocation
        // one chance with the audience rollout rule
        // one chance with the everyone else rule
        verify(mockBucketer, atMost(2)).bucket(any(Experiment.class), anyString(), any(ProjectConfig.class));
    }

    /**
     * Verify that {@link DecisionService#getVariationForFeatureInRollout(FeatureFlag, OptimizelyUserContext, ProjectConfig)}
     * returns null when a user is excluded from every rule of a rollout due to targeting
     * and also fails traffic allocation in the everyone else rollout.
     */
    @Test
    public void getVariationForFeatureInRolloutReturnsNullWhenUserFailsAllAudiencesAndTraffic() {
        Bucketer mockBucketer = mock(Bucketer.class);
        when(mockBucketer.bucket(any(Experiment.class), anyString(), any(ProjectConfig.class))).thenReturn(DecisionResponse.nullNoReasons());

        DecisionService decisionService = new DecisionService(mockBucketer, mockErrorHandler, null);

        FeatureDecision featureDecision = decisionService.getVariationForFeatureInRollout(
            FEATURE_FLAG_MULTI_VARIATE_FEATURE,
            optimizely.createUserContext(genericUserId, Collections.<String, Object>emptyMap()),
            v4ProjectConfig
        ).getResult();
        assertNull(featureDecision.variation);
        assertNull(featureDecision.decisionSource);

        // user is only bucketed once for the everyone else rule
        verify(mockBucketer, times(1)).bucket(any(Experiment.class), anyString(), any(ProjectConfig.class));
    }

    /**
     * Verify that {@link DecisionService#getVariationForFeatureInRollout(FeatureFlag, OptimizelyUserContext, ProjectConfig)}
     * returns the variation of "Everyone Else" rule
     * when the user fails targeting for all rules, but is bucketed into the "Everyone Else" rule.
     */
    @Test
    public void getVariationForFeatureInRolloutReturnsVariationWhenUserFailsAllAudienceButSatisfiesTraffic() {
        Bucketer mockBucketer = mock(Bucketer.class);
        Rollout rollout = ROLLOUT_2;
        Experiment everyoneElseRule = rollout.getExperiments().get(rollout.getExperiments().size() - 1);
        Variation expectedVariation = everyoneElseRule.getVariations().get(0);
        when(mockBucketer.bucket(eq(everyoneElseRule), anyString(), any(ProjectConfig.class))).thenReturn(DecisionResponse.responseNoReasons(expectedVariation));

        DecisionService decisionService = new DecisionService(
            mockBucketer,
            mockErrorHandler,
            null
        );

        FeatureDecision featureDecision = decisionService.getVariationForFeatureInRollout(
            FEATURE_FLAG_MULTI_VARIATE_FEATURE,
            optimizely.createUserContext(genericUserId, Collections.<String, Object>emptyMap()),
            v4ProjectConfig
        ).getResult();
        logbackVerifier.expectMessage(Level.DEBUG, "Evaluating audiences for rule \"1\": [3468206642].");
        logbackVerifier.expectMessage(Level.INFO, "Audiences for rule \"1\" collectively evaluated to null.");
        logbackVerifier.expectMessage(Level.DEBUG, "Evaluating audiences for rule \"2\": [3988293898].");
        logbackVerifier.expectMessage(Level.INFO, "Audiences for rule \"2\" collectively evaluated to null.");
        logbackVerifier.expectMessage(Level.DEBUG, "Evaluating audiences for rule \"3\": [4194404272].");
        logbackVerifier.expectMessage(Level.INFO, "Audiences for rule \"3\" collectively evaluated to null.");
        logbackVerifier.expectMessage(Level.DEBUG, "User \"genericUserId\" meets conditions for targeting rule \"Everyone Else\".");

        assertEquals(expectedVariation, featureDecision.variation);
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT, featureDecision.decisionSource);

        // verify user is only bucketed once for everyone else rule
        verify(mockBucketer, times(1)).bucket(any(Experiment.class), anyString(), any(ProjectConfig.class));
    }

    /**
     * Verify that {@link DecisionService#getVariationForFeatureInRollout(FeatureFlag, OptimizelyUserContext, ProjectConfig)}
     * returns the variation of "Everyone Else" rule
     * when the user passes targeting for a rule, but was failed the traffic allocation for that rule,
     * and is bucketed successfully into the "Everyone Else" rule.
     */
    @Test
    public void getVariationForFeatureInRolloutReturnsVariationWhenUserFailsTrafficInRuleAndPassesInEveryoneElse() {
        Bucketer mockBucketer = mock(Bucketer.class);
        Rollout rollout = ROLLOUT_2;
        Experiment everyoneElseRule = rollout.getExperiments().get(rollout.getExperiments().size() - 1);
        Variation expectedVariation = everyoneElseRule.getVariations().get(0);
        when(mockBucketer.bucket(any(Experiment.class), anyString(), any(ProjectConfig.class))).thenReturn(DecisionResponse.nullNoReasons());
        when(mockBucketer.bucket(eq(everyoneElseRule), anyString(), any(ProjectConfig.class))).thenReturn(DecisionResponse.responseNoReasons(expectedVariation));

        DecisionService decisionService = new DecisionService(
            mockBucketer,
            mockErrorHandler,
            null
        );

        FeatureDecision featureDecision = decisionService.getVariationForFeatureInRollout(
            FEATURE_FLAG_MULTI_VARIATE_FEATURE,
            optimizely.createUserContext(genericUserId, Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            v4ProjectConfig
        ).getResult();
        assertEquals(expectedVariation, featureDecision.variation);
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT, featureDecision.decisionSource);

        logbackVerifier.expectMessage(Level.DEBUG, "User \"genericUserId\" meets conditions for targeting rule \"Everyone Else\".");

        // verify user is only bucketed once for everyone else rule
        verify(mockBucketer, times(2)).bucket(any(Experiment.class), anyString(), any(ProjectConfig.class));
    }

    /**
     * Verify that {@link DecisionService#getVariationForFeatureInRollout(FeatureFlag, OptimizelyUserContext, ProjectConfig)}
     * returns the variation of "Everyone Else" rule
     * when the user passes targeting for a rule, but was failed the traffic allocation for that rule,
     * and is bucketed successfully into the "Everyone Else" rule.
     * Fallback bucketing should not evaluate any other audiences.
     * Even though the user would satisfy a later rollout rule, they are never evaluated for it or bucketed into it.
     */
    @Test
    public void getVariationForFeatureInRolloutReturnsVariationWhenUserFailsTrafficInRuleButWouldPassForAnotherRuleAndPassesInEveryoneElse() {
        Bucketer mockBucketer = mock(Bucketer.class);
        Rollout rollout = ROLLOUT_2;
        Experiment englishCitizensRule = rollout.getExperiments().get(2);
        Variation englishCitizenVariation = englishCitizensRule.getVariations().get(0);
        Experiment everyoneElseRule = rollout.getExperiments().get(rollout.getExperiments().size() - 1);
        Variation expectedVariation = everyoneElseRule.getVariations().get(0);
        when(mockBucketer.bucket(any(Experiment.class), anyString(), any(ProjectConfig.class))).thenReturn(DecisionResponse.nullNoReasons());
        when(mockBucketer.bucket(eq(everyoneElseRule), anyString(), any(ProjectConfig.class))).thenReturn(DecisionResponse.responseNoReasons(expectedVariation));
        when(mockBucketer.bucket(eq(englishCitizensRule), anyString(), any(ProjectConfig.class))).thenReturn(DecisionResponse.responseNoReasons(englishCitizenVariation));

        DecisionService decisionService = new DecisionService(
            mockBucketer,
            mockErrorHandler,
            null
        );

        FeatureDecision featureDecision = decisionService.getVariationForFeatureInRollout(
            FEATURE_FLAG_MULTI_VARIATE_FEATURE,
            optimizely.createUserContext(genericUserId, DatafileProjectConfigTestUtils.createMapOfObjects(
                DatafileProjectConfigTestUtils.createListOfObjects(
                    ATTRIBUTE_HOUSE_KEY, ATTRIBUTE_NATIONALITY_KEY
                ),
                DatafileProjectConfigTestUtils.createListOfObjects(
                    AUDIENCE_GRYFFINDOR_VALUE, AUDIENCE_ENGLISH_CITIZENS_VALUE
                )
            )),
            v4ProjectConfig
        ).getResult();
        assertEquals(expectedVariation, featureDecision.variation);
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT, featureDecision.decisionSource);

        // verify user is only bucketed once for everyone else rule
        verify(mockBucketer, times(2)).bucket(any(Experiment.class), anyString(), any(ProjectConfig.class));
    }

    /**
     * Verify that {@link DecisionService#getVariationForFeatureInRollout(FeatureFlag, OptimizelyUserContext, ProjectConfig)}
     * returns the variation of "English Citizens" rule
     * when the user fails targeting for previous rules, but passes targeting and traffic for Rule 3.
     */
    @Test
    public void getVariationForFeatureInRolloutReturnsVariationWhenUserFailsTargetingInPreviousRulesButPassesRule3() {
        Bucketer mockBucketer = mock(Bucketer.class);
        Rollout rollout = ROLLOUT_2;
        Experiment englishCitizensRule = rollout.getExperiments().get(2);
        Variation englishCitizenVariation = englishCitizensRule.getVariations().get(0);
        Experiment everyoneElseRule = rollout.getExperiments().get(rollout.getExperiments().size() - 1);
        Variation everyoneElseVariation = everyoneElseRule.getVariations().get(0);
        when(mockBucketer.bucket(any(Experiment.class), anyString(), any(ProjectConfig.class))).thenReturn(DecisionResponse.nullNoReasons());
        when(mockBucketer.bucket(eq(everyoneElseRule), anyString(), any(ProjectConfig.class))).thenReturn(DecisionResponse.responseNoReasons(everyoneElseVariation));
        when(mockBucketer.bucket(eq(englishCitizensRule), anyString(), any(ProjectConfig.class))).thenReturn(DecisionResponse.responseNoReasons(englishCitizenVariation));

        DecisionService decisionService = new DecisionService(mockBucketer, mockErrorHandler, null);

        FeatureDecision featureDecision = decisionService.getVariationForFeatureInRollout(
            FEATURE_FLAG_MULTI_VARIATE_FEATURE,
            optimizely.createUserContext(genericUserId, Collections.singletonMap(ATTRIBUTE_NATIONALITY_KEY, AUDIENCE_ENGLISH_CITIZENS_VALUE)),
            v4ProjectConfig
        ).getResult();
        assertEquals(englishCitizenVariation, featureDecision.variation);
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT, featureDecision.decisionSource);
        logbackVerifier.expectMessage(Level.INFO, "Audiences for rule \"2\" collectively evaluated to null");
        logbackVerifier.expectMessage(Level.DEBUG, "Evaluating audiences for rule \"3\": [4194404272].");
        logbackVerifier.expectMessage(Level.DEBUG, "Starting to evaluate audience \"4194404272\" with conditions: [and, [or, [or, {name='nationality', type='custom_attribute', match='exact', value='English'}]]].");
        logbackVerifier.expectMessage(Level.DEBUG, "Audience \"4194404272\" evaluated to true.");
        logbackVerifier.expectMessage(Level.INFO, "Audiences for rule \"3\" collectively evaluated to true");
        // verify user is only bucketed once for everyone else rule
        verify(mockBucketer, times(1)).bucket(any(Experiment.class), anyString(), any(ProjectConfig.class));
    }

    @Test
    public void getVariationFromDeliveryRuleTest() {
        int index = 3;
        List<Experiment> rules = ROLLOUT_2.getExperiments();
        Experiment experiment = ROLLOUT_2.getExperiments().get(index);
        Variation expectedVariation = null;
        for (Variation variation : experiment.getVariations()) {
            if (variation.getKey().equals("3137445031")) {
                expectedVariation = variation;
            }
        }
        DecisionResponse<AbstractMap.SimpleEntry> decisionResponse = decisionService.getVariationFromDeliveryRule(
            v4ProjectConfig,
            FEATURE_FLAG_MULTI_VARIATE_FEATURE.getKey(),
            rules,
            index,
            optimizely.createUserContext(genericUserId, Collections.singletonMap(ATTRIBUTE_NATIONALITY_KEY, AUDIENCE_ENGLISH_CITIZENS_VALUE))
        );

        Variation variation = (Variation) decisionResponse.getResult().getKey();
        Boolean skipToEveryoneElse = (Boolean) decisionResponse.getResult().getValue();
        assertNotNull(decisionResponse.getResult());
        assertNotNull(variation);
        assertNotNull(expectedVariation);
        assertEquals(expectedVariation, variation);
        assertFalse(skipToEveryoneElse);
    }

    @Test
    public void getVariationFromExperimentRuleTest() {
        int index = 3;
        Experiment experiment = ROLLOUT_2.getExperiments().get(index);
        Variation expectedVariation = null;
        for (Variation variation : experiment.getVariations()) {
            if (variation.getKey().equals("3137445031")) {
                expectedVariation = variation;
            }
        }
        DecisionResponse<Variation> decisionResponse = decisionService.getVariationFromExperimentRule(
            v4ProjectConfig,
            FEATURE_FLAG_MULTI_VARIATE_FEATURE.getKey(),
            experiment,
            optimizely.createUserContext(genericUserId, Collections.singletonMap(ATTRIBUTE_NATIONALITY_KEY, AUDIENCE_ENGLISH_CITIZENS_VALUE)),
            Collections.emptyList()
        );

        assertEquals(expectedVariation, decisionResponse.getResult());
    }

    //========= white list tests ==========/

    /**
     * Test {@link DecisionService#getWhitelistedVariation(Experiment, String)} correctly returns a whitelisted variation.
     */
    @Test
    public void getWhitelistedReturnsForcedVariation() {
        logbackVerifier.expectMessage(Level.INFO, "User \"" + whitelistedUserId + "\" is forced in variation \""
            + whitelistedVariation.getKey() + "\".");
        assertEquals(whitelistedVariation, decisionService.getWhitelistedVariation(whitelistedExperiment, whitelistedUserId).getResult());
    }

    /**
     * Verify that {@link DecisionService#getWhitelistedVariation(Experiment, String)} returns null
     * when an invalid variation key is found in the forced variations mapping.
     */
    @Test
    public void getWhitelistedWithInvalidVariation() throws Exception {
        String userId = "testUser1";
        String invalidVariationKey = "invalidVarKey";

        List<Variation> variations = Collections.singletonList(
            new Variation("1", "var1")
        );

        List<TrafficAllocation> trafficAllocations = Collections.singletonList(
            new TrafficAllocation("1", 1000)
        );

        Map<String, String> userIdToVariationKeyMap = Collections.singletonMap(userId, invalidVariationKey);

        Experiment experiment = new Experiment("1234", "exp_key", "Running", "1", Collections.<String>emptyList(),
            null, variations, userIdToVariationKeyMap, trafficAllocations);

        logbackVerifier.expectMessage(
            Level.ERROR,
            "Variation \"" + invalidVariationKey + "\" is not in the datafile. Not activating user \"" + userId + "\".");

        assertNull(decisionService.getWhitelistedVariation(experiment, userId).getResult());
    }

    /**
     * Verify that {@link DecisionService#getWhitelistedVariation(Experiment, String)} returns null when user is not whitelisted.
     */
    @Test
    public void getWhitelistedReturnsNullWhenUserIsNotWhitelisted() throws Exception {
        assertNull(decisionService.getWhitelistedVariation(whitelistedExperiment, genericUserId).getResult());
    }

    //======== User Profile tests =========//

    /**
     * Verify that {@link DecisionService#getStoredVariation(Experiment, UserProfile, ProjectConfig)} returns a variation that is
     * stored in the provided {@link UserProfile}.
     */
    @SuppressFBWarnings
    @Test
    public void bucketReturnsVariationStoredInUserProfile() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation variation = experiment.getVariations().get(0);
        Decision decision = new Decision(variation.getId());

        UserProfile userProfile = new UserProfile(userProfileId,
            Collections.singletonMap(experiment.getId(), decision));
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap());

        Bucketer bucketer = new Bucketer();
        DecisionService decisionService = new DecisionService(bucketer, mockErrorHandler, userProfileService);

        logbackVerifier.expectMessage(Level.INFO,
            "Returning previously activated variation \"" + variation.getKey() + "\" of experiment \"" + experiment.getKey() + "\""
                + " for user \"" + userProfileId + "\" from user profile.");

        // ensure user with an entry in the user profile is bucketed into the corresponding stored variation
        assertEquals(variation,
            decisionService.getVariation(experiment, optimizely.createUserContext(userProfileId, Collections.emptyMap()), noAudienceProjectConfig).getResult());

        verify(userProfileService).lookup(userProfileId);
    }

    /**
     * Verify that {@link DecisionService#getStoredVariation(Experiment, UserProfile, ProjectConfig)} returns null and logs properly
     * when there is no stored variation for that user in that {@link Experiment} in the {@link UserProfileService}.
     */
    @Test
    public void getStoredVariationLogsWhenLookupReturnsNull() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);

        Bucketer bucketer = new Bucketer();
        UserProfileService userProfileService = mock(UserProfileService.class);
        UserProfile userProfile = new UserProfile(userProfileId, Collections.<String, Decision>emptyMap());
        when(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap());

        DecisionService decisionService = new DecisionService(bucketer, mockErrorHandler, userProfileService);

        logbackVerifier.expectMessage(Level.INFO, "No previously activated variation of experiment " +
            "\"" + experiment.getKey() + "\" for user \"" + userProfileId + "\" found in user profile.");

        assertNull(decisionService.getStoredVariation(experiment, userProfile, noAudienceProjectConfig).getResult());
    }

    /**
     * Verify that {@link DecisionService#getStoredVariation(Experiment, UserProfile, ProjectConfig)} returns null
     * when a {@link UserProfile} is present, contains a decision for the experiment in question,
     * but the variation ID for that decision does not exist in the datafile.
     */
    @Test
    public void getStoredVariationReturnsNullWhenVariationIsNoLongerInConfig() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final String storedVariationId = "missingVariation";
        final Decision storedDecision = new Decision(storedVariationId);
        final Map<String, Decision> storedDecisions = new HashMap<String, Decision>();
        storedDecisions.put(experiment.getId(), storedDecision);
        final UserProfile storedUserProfile = new UserProfile(userProfileId,
            storedDecisions);

        Bucketer bucketer = mock(Bucketer.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.lookup(userProfileId)).thenReturn(storedUserProfile.toMap());

        DecisionService decisionService = new DecisionService(bucketer, mockErrorHandler, userProfileService);

        logbackVerifier.expectMessage(Level.INFO,
            "User \"" + userProfileId + "\" was previously bucketed into variation with ID \"" + storedVariationId + "\" for " +
                "experiment \"" + experiment.getKey() + "\", but no matching variation " +
                "was found for that user. We will re-bucket the user.");

        assertNull(decisionService.getStoredVariation(experiment, storedUserProfile, noAudienceProjectConfig).getResult());
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, OptimizelyUserContext, ProjectConfig)}
     * saves a {@link Variation}of an {@link Experiment} for a user when a {@link UserProfileService} is present.
     */
    @SuppressFBWarnings
    @Test
    public void getVariationSavesBucketedVariationIntoUserProfile() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation variation = experiment.getVariations().get(0);
        Decision decision = new Decision(variation.getId());

        UserProfileService userProfileService = mock(UserProfileService.class);
        UserProfile originalUserProfile = new UserProfile(userProfileId,
            new HashMap<String, Decision>());
        when(userProfileService.lookup(userProfileId)).thenReturn(originalUserProfile.toMap());
        UserProfile expectedUserProfile = new UserProfile(userProfileId,
            Collections.singletonMap(experiment.getId(), decision));

        Bucketer mockBucketer = mock(Bucketer.class);
        when(mockBucketer.bucket(eq(experiment), eq(userProfileId), eq(noAudienceProjectConfig))).thenReturn(DecisionResponse.responseNoReasons(variation));

        DecisionService decisionService = new DecisionService(mockBucketer, mockErrorHandler, userProfileService);

        assertEquals(variation, decisionService.getVariation(
            experiment, optimizely.createUserContext(userProfileId, Collections.emptyMap()), noAudienceProjectConfig).getResult()
        );
        logbackVerifier.expectMessage(Level.INFO,
            String.format("Saved variation \"%s\" of experiment \"%s\" for user \"" + userProfileId + "\".", variation.getId(),
                experiment.getId()));

        verify(userProfileService).save(eq(expectedUserProfile.toMap()));
    }

    /**
     * Verify that {@link DecisionService#getVariation(Experiment, OptimizelyUserContext, ProjectConfig)} logs correctly
     * when a {@link UserProfileService} is present but fails to save an activation.
     */
    @Test
    public void bucketLogsCorrectlyWhenUserProfileFailsToSave() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation variation = experiment.getVariations().get(0);
        Decision decision = new Decision(variation.getId());
        Bucketer bucketer = new Bucketer();
        UserProfileService userProfileService = mock(UserProfileService.class);
        doThrow(new Exception()).when(userProfileService).save(anyMapOf(String.class, Object.class));

        Map<String, Decision> experimentBucketMap = new HashMap<String, Decision>();
        experimentBucketMap.put(experiment.getId(), decision);
        UserProfile expectedUserProfile = new UserProfile(userProfileId,
            experimentBucketMap);
        UserProfile saveUserProfile = new UserProfile(userProfileId,
            new HashMap<String, Decision>());

        DecisionService decisionService = new DecisionService(bucketer, mockErrorHandler, userProfileService);


        decisionService.saveVariation(experiment, variation, saveUserProfile);

        logbackVerifier.expectMessage(Level.WARN,
            String.format("Failed to save variation \"%s\" of experiment \"%s\" for user \"" + userProfileId + "\".", variation.getId(),
                experiment.getId()));

        verify(userProfileService).save(eq(expectedUserProfile.toMap()));
    }

    /**
     * Verify that a {@link UserProfile} is saved when the user is brand new and did not have anything returned from
     * {@link UserProfileService#lookup(String)}.
     */
    @Test
    public void getVariationSavesANewUserProfile() throws Exception {
        final Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation variation = experiment.getVariations().get(0);
        final Decision decision = new Decision(variation.getId());
        final UserProfile expectedUserProfile = new UserProfile(userProfileId,
            Collections.singletonMap(experiment.getId(), decision));

        Bucketer bucketer = mock(Bucketer.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        DecisionService decisionService = new DecisionService(bucketer, mockErrorHandler, userProfileService);

        when(bucketer.bucket(eq(experiment), eq(userProfileId), eq(noAudienceProjectConfig))).thenReturn(DecisionResponse.responseNoReasons(variation));
        when(userProfileService.lookup(userProfileId)).thenReturn(null);

        assertEquals(variation, decisionService.getVariation(experiment, optimizely.createUserContext(userProfileId, Collections.emptyMap()), noAudienceProjectConfig).getResult());
        verify(userProfileService).save(expectedUserProfile.toMap());
    }

    @Test
    public void getVariationBucketingId() throws Exception {
        Bucketer bucketer = mock(Bucketer.class);
        DecisionService decisionService = spy(new DecisionService(bucketer, mockErrorHandler, null));
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation expectedVariation = experiment.getVariations().get(0);

        when(bucketer.bucket(eq(experiment), eq("bucketId"), eq(validProjectConfig))).thenReturn(DecisionResponse.responseNoReasons(expectedVariation));

        Map<String, Object> attr = new HashMap();
        attr.put(ControlAttribute.BUCKETING_ATTRIBUTE.toString(), "bucketId");
        // user excluded without audiences and whitelisting
        assertThat(decisionService.getVariation(experiment, optimizely.createUserContext(genericUserId, attr), validProjectConfig).getResult(), is(expectedVariation));

    }

    /**
     * Verify that {@link DecisionService#getVariationForFeatureInRollout(FeatureFlag, OptimizelyUserContext, ProjectConfig)}
     * uses bucketing ID to bucket the user into rollouts.
     */
    @Test
    public void getVariationForRolloutWithBucketingId() {
        Experiment rolloutRuleExperiment = ROLLOUT_3_EVERYONE_ELSE_RULE;
        Variation rolloutVariation = ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION;
        FeatureFlag featureFlag = FEATURE_FLAG_SINGLE_VARIABLE_INTEGER;
        String bucketingId = "user_bucketing_id";
        String userId = "user_id";
        Map<String, Object> attributes = new HashMap();
        attributes.put(ControlAttribute.BUCKETING_ATTRIBUTE.toString(), bucketingId);

        Bucketer bucketer = mock(Bucketer.class);
        when(bucketer.bucket(eq(rolloutRuleExperiment), eq(userId), eq(v4ProjectConfig))).thenReturn(DecisionResponse.nullNoReasons());
        when(bucketer.bucket(eq(rolloutRuleExperiment), eq(bucketingId), eq(v4ProjectConfig))).thenReturn(DecisionResponse.responseNoReasons(rolloutVariation));

        DecisionService decisionService = spy(new DecisionService(
            bucketer,
            mockErrorHandler,
            null
        ));

        FeatureDecision expectedFeatureDecision = new FeatureDecision(
            rolloutRuleExperiment,
            rolloutVariation,
            FeatureDecision.DecisionSource.ROLLOUT);

        FeatureDecision featureDecision = decisionService.getVariationForFeature(featureFlag, optimizely.createUserContext(userId, attributes), v4ProjectConfig).getResult();

        assertEquals(expectedFeatureDecision, featureDecision);
    }

    /**
     * Invalid User IDs
     * <p>
     * User ID is null
     * User ID is an empty string
     * Invalid Experiment IDs
     * <p>
     * Experiment key does not exist in the datafile
     * Experiment key is null
     * Experiment key is an empty string
     * Invalid Variation IDs [set only]
     * <p>
     * Variation key does not exist in the datafile
     * Variation key is null
     * Variation key is an empty string
     * Multiple set calls [set only]
     * <p>
     * Call set variation with different variations on one user/experiment to confirm that each set is expected.
     * Set variation on multiple variations for one user.
     * Set variations for multiple users.
     */
    /* UserID test */
    @Test
    @SuppressFBWarnings("NP")
    public void setForcedVariationNullUserId() {
        Experiment experiment = validProjectConfig.getExperimentKeyMapping().get("etag1");
        boolean b = decisionService.setForcedVariation(experiment, null, "vtag1");
        assertFalse(b);
    }

    @Test
    @SuppressFBWarnings("NP")
    public void getForcedVariationNullUserId() {
        Experiment experiment = validProjectConfig.getExperimentKeyMapping().get("etag1");
        assertNull(decisionService.getForcedVariation(experiment, null).getResult());
    }

    @Test
    public void setForcedVariationEmptyUserId() {
        Experiment experiment = validProjectConfig.getExperimentKeyMapping().get("etag1");
        assertTrue(decisionService.setForcedVariation(experiment, "", "vtag1"));
    }

    @Test
    public void getForcedVariationEmptyUserId() {
        Experiment experiment = validProjectConfig.getExperimentKeyMapping().get("etag1");
        assertNull(decisionService.getForcedVariation(experiment, "").getResult());
    }

    /* Invalid Variation Id (set only */
    @Test
    public void setForcedVariationWrongVariationKey() {
        Experiment experiment = validProjectConfig.getExperimentKeyMapping().get("etag1");
        assertFalse(decisionService.setForcedVariation(experiment, "testUser1", "vtag3"));
    }

    @Test
    public void setForcedVariationNullVariationKey() {
        Experiment experiment = validProjectConfig.getExperimentKeyMapping().get("etag1");
        assertFalse(decisionService.setForcedVariation(experiment, "testUser1", null));
        assertNull(decisionService.getForcedVariation(experiment, "testUser1").getResult());
    }

    @Test
    public void setForcedVariationEmptyVariationKey() {
        Experiment experiment = validProjectConfig.getExperimentKeyMapping().get("etag1");
        assertFalse(decisionService.setForcedVariation(experiment, "testUser1", ""));
    }

    /* Multiple set calls (set only */
    @Test
    public void setForcedVariationDifferentVariations() {
        Experiment experiment = validProjectConfig.getExperimentKeyMapping().get("etag1");
        assertTrue(decisionService.setForcedVariation(experiment, "testUser1", "vtag1"));
        assertTrue(decisionService.setForcedVariation(experiment, "testUser1", "vtag2"));
        assertEquals(decisionService.getForcedVariation(experiment, "testUser1").getResult().getKey(), "vtag2");
        assertTrue(decisionService.setForcedVariation(experiment, "testUser1", null));
    }

    @Test
    public void setForcedVariationMultipleVariationsExperiments() {
        Experiment experiment1 = validProjectConfig.getExperimentKeyMapping().get("etag1");
        Experiment experiment2 = validProjectConfig.getExperimentKeyMapping().get("etag2");

        assertTrue(decisionService.setForcedVariation(experiment1, "testUser1", "vtag1"));
        assertTrue(decisionService.setForcedVariation(experiment1, "testUser2", "vtag2"));

        assertTrue(decisionService.setForcedVariation(experiment2, "testUser1", "vtag3"));
        assertTrue(decisionService.setForcedVariation(experiment2, "testUser2", "vtag4"));

        assertEquals(decisionService.getForcedVariation(experiment1, "testUser1").getResult().getKey(), "vtag1");
        assertEquals(decisionService.getForcedVariation(experiment1, "testUser2").getResult().getKey(), "vtag2");

        assertEquals(decisionService.getForcedVariation(experiment2, "testUser1").getResult().getKey(), "vtag3");
        assertEquals(decisionService.getForcedVariation(experiment2, "testUser2").getResult().getKey(), "vtag4");

        assertTrue(decisionService.setForcedVariation(experiment1, "testUser1", null));
        assertTrue(decisionService.setForcedVariation(experiment1, "testUser2", null));

        assertTrue(decisionService.setForcedVariation(experiment2, "testUser1", null));
        assertTrue(decisionService.setForcedVariation(experiment2, "testUser2", null));

        assertNull(decisionService.getForcedVariation(experiment1, "testUser1").getResult());
        assertNull(decisionService.getForcedVariation(experiment1, "testUser2").getResult());

        assertNull(decisionService.getForcedVariation(experiment2, "testUser1").getResult());
        assertNull(decisionService.getForcedVariation(experiment2, "testUser2").getResult());
    }

    @Test
    public void setForcedVariationMultipleUsers() {
        Experiment experiment1 = validProjectConfig.getExperimentKeyMapping().get("etag1");
        Experiment experiment2 = validProjectConfig.getExperimentKeyMapping().get("etag2");

        assertTrue(decisionService.setForcedVariation(experiment1, "testUser1", "vtag1"));
        assertTrue(decisionService.setForcedVariation(experiment1, "testUser2", "vtag1"));
        assertTrue(decisionService.setForcedVariation(experiment1, "testUser3", "vtag1"));
        assertTrue(decisionService.setForcedVariation(experiment1, "testUser4", "vtag1"));

        assertEquals(decisionService.getForcedVariation(experiment1, "testUser1").getResult().getKey(), "vtag1");
        assertEquals(decisionService.getForcedVariation(experiment1, "testUser2").getResult().getKey(), "vtag1");
        assertEquals(decisionService.getForcedVariation(experiment1, "testUser3").getResult().getKey(), "vtag1");
        assertEquals(decisionService.getForcedVariation(experiment1, "testUser4").getResult().getKey(), "vtag1");

        assertTrue(decisionService.setForcedVariation(experiment1, "testUser1", null));
        assertTrue(decisionService.setForcedVariation(experiment1, "testUser2", null));
        assertTrue(decisionService.setForcedVariation(experiment1, "testUser3", null));
        assertTrue(decisionService.setForcedVariation(experiment1, "testUser4", null));

        assertNull(decisionService.getForcedVariation(experiment1, "testUser1").getResult());
        assertNull(decisionService.getForcedVariation(experiment1, "testUser2").getResult());

        assertNull(decisionService.getForcedVariation(experiment2, "testUser1").getResult());
        assertNull(decisionService.getForcedVariation(experiment2, "testUser2").getResult());
    }

}
