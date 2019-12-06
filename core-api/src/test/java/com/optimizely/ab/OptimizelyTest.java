/****************************************************************************
 * Copyright 2016-2019, Optimizely, Inc. and contributors                   *
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
package com.optimizely.ab;

import ch.qos.logback.classic.Level;
import com.google.common.collect.ImmutableMap;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.bucketing.FeatureDecision;
import com.optimizely.ab.config.*;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfig;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.error.RaiseExceptionErrorHandler;
import com.optimizely.ab.event.BatchEventProcessor;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.EventProcessor;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.UserEventFactory;
import com.optimizely.ab.internal.LogbackVerifier;
import com.optimizely.ab.internal.ControlAttribute;
import com.optimizely.ab.notification.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.*;
import static com.optimizely.ab.config.ValidProjectConfigV4.*;
import static com.optimizely.ab.event.LogEvent.RequestMethod;
import static com.optimizely.ab.notification.DecisionNotification.ExperimentDecisionNotificationBuilder.EXPERIMENT_KEY;
import static com.optimizely.ab.notification.DecisionNotification.ExperimentDecisionNotificationBuilder.VARIATION_KEY;
import static com.optimizely.ab.notification.DecisionNotification.FeatureVariableDecisionNotificationBuilder.*;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the top-level {@link Optimizely} class.
 */
@RunWith(Parameterized.class)
public class OptimizelyTest {

    @Parameterized.Parameters(name = "{index}: Version: {2}")
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
            {
                validConfigJsonV2(),
                noAudienceProjectConfigJsonV2(),
                2,
                (Function<EventHandler, EventProcessor>) (eventHandler) -> null
            },
            {
                validConfigJsonV3(),
                noAudienceProjectConfigJsonV3(),  // FIX-ME this is not a valid v3 datafile
                3,
                (Function<EventHandler, EventProcessor>) (eventHandler) -> null
            },
            {
                validConfigJsonV4(),
                validConfigJsonV4(),
                4,
                (Function<EventHandler, EventProcessor>) (eventHandler) -> null
            },
            {
                validConfigJsonV4(),
                validConfigJsonV4(),
                4,
                (Function<EventHandler, EventProcessor>) (eventHandler) -> BatchEventProcessor.builder()
                    .withEventHandler(eventHandler)
                    .build()
            }
        });
    }

    @Rule
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public MockitoRule rule = MockitoJUnit.rule();

    public ExpectedException thrown = ExpectedException.none();
    public LogbackVerifier logbackVerifier = new LogbackVerifier();
    public OptimizelyRule optimizelyBuilder = new OptimizelyRule();
    public EventHandlerRule eventHandler = new EventHandlerRule();

    @Rule
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public RuleChain ruleChain = RuleChain.outerRule(thrown)
        .around(logbackVerifier)
        .around(eventHandler)
        .around(optimizelyBuilder);

    @Mock
    EventHandler mockEventHandler;
    @Mock
    Bucketer mockBucketer;
    @Mock
    DecisionService mockDecisionService;

    private static final String genericUserId = "genericUserId";
    private static final String testUserId = "userId";
    private static final String testBucketingId = "bucketingId";
    private static final String testBucketingIdKey = ControlAttribute.BUCKETING_ATTRIBUTE.toString();

    @Parameterized.Parameter(0)
    public String validDatafile;

    @Parameterized.Parameter(1)
    public String noAudienceDatafile;

    @Parameterized.Parameter(2)
    public int datafileVersion;

    @Parameterized.Parameter(3)
    public Function<EventHandler, EventProcessor> eventProcessorSupplier;

    private ProjectConfig validProjectConfig;
    private ProjectConfig noAudienceProjectConfig;

    @Before
    public void setUp() throws Exception {
        validProjectConfig = new DatafileProjectConfig.Builder().withDatafile(validDatafile).build();
        noAudienceProjectConfig = new DatafileProjectConfig.Builder().withDatafile(noAudienceDatafile).build();

        // FIX-ME
        //assertEquals(validProjectConfig.getVersion(), noAudienceProjectConfig.getVersion());

        optimizelyBuilder
            .withEventProcessor(eventProcessorSupplier.apply(eventHandler))
            .withEventHandler(eventHandler)
            .withConfig(validProjectConfig);
    }

    @Test
    public void testClose() throws Exception {
        EventHandler mockEventHandler = mock(
            EventHandler.class,
            withSettings().extraInterfaces(AutoCloseable.class)
        );

        ProjectConfigManager mockProjectConfigManager = mock(
            ProjectConfigManager.class,
            withSettings().extraInterfaces(AutoCloseable.class)
        );

        EventProcessor mockEventProcessor = mock(
            EventProcessor.class,
            withSettings().extraInterfaces(AutoCloseable.class)
        );

        Optimizely optimizely = Optimizely.builder()
            .withEventHandler(mockEventHandler)
            .withEventProcessor(mockEventProcessor)
            .withConfigManager(mockProjectConfigManager)
            .build();

        optimizely.close();

        verify((AutoCloseable) mockEventHandler).close();
        verify((AutoCloseable) mockProjectConfigManager).close();
        verify((AutoCloseable) mockEventProcessor).close();

    }

    //======== activate tests ========//

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEnd() throws Exception {
        Experiment activatedExperiment;
        Map<String, Object> testUserAttributes = new HashMap<>();

        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            testUserAttributes.put("browser_type", "chrome");
        }

        logbackVerifier.expectMessage(Level.DEBUG, "This decision will not be saved since the UserProfileService is null.");
        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertNotNull(actualVariation);

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, testUserAttributes);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEndWithTypedAudienceInt() throws Exception {
        assumeTrue(datafileVersion >= 4);

        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping()
            .get(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY);
        Map<String, Object> testUserAttributes = Collections.singletonMap(ATTRIBUTE_INTEGER_KEY, 2); // should be gt 1.

        logbackVerifier.expectMessage(Level.DEBUG, "This decision will not be saved since the UserProfileService is null.");
        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertNotNull(actualVariation);

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, testUserAttributes);
    }

    @Test
    public void testOptimizelyConfig() throws Exception {
        Optimizely optimizely = optimizelyBuilder.build();
        OptimizelyConfig optimizelyConfig = optimizely.getOptimizelyConfig();
        assertNotNull(optimizelyConfig);
    }

    /**
     * Verify that activating using typed audiences works for numeric match exact using double and integer.
     */
    @Test
    public void activateEndToEndWithTypedAudienceIntExactDouble() throws Exception {
        assumeTrue(datafileVersion >= 4);

        Map<String, Object> testUserAttributes = Collections.singletonMap(ATTRIBUTE_INTEGER_KEY, 1.0);
        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY);

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertNotNull(actualVariation);

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, testUserAttributes);
    }

    /**
     * Verify that activating using typed audiences works for numeric match exact using double and integer.
     */
    @Test
    public void activateEndToEndWithTypedAudienceIntExact() throws Exception {
        assumeTrue(datafileVersion >= 4);

        Map<String, Object> testUserAttributes = Collections.singletonMap(ATTRIBUTE_INTEGER_KEY, 1);
        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY);

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertNotNull(actualVariation);

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, testUserAttributes);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEndWithTypedAudienceBool() throws Exception {
        assumeTrue(datafileVersion >= 4);

        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping()
            .get(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY);
        Map<String, Object> testUserAttributes = Collections.singletonMap(ATTRIBUTE_BOOLEAN_KEY, true); // should be eq true.

        logbackVerifier.expectMessage(Level.DEBUG, "This decision will not be saved since the UserProfileService is null.");
        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertNotNull(actualVariation);

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, testUserAttributes);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEndWithTypedAudienceDouble() throws Exception {
        assumeTrue(datafileVersion >= 4);

        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping()
            .get(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY);
        Map<String, Object> testUserAttributes = Collections.singletonMap(ATTRIBUTE_DOUBLE_KEY, 99.9); // should be lt 100.

        logbackVerifier.expectMessage(Level.DEBUG, "This decision will not be saved since the UserProfileService is null.");
        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");

        // activate the experiment
        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertNotNull(actualVariation);

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, testUserAttributes);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEndWithTypedAudienceBoolWithAndAudienceConditions() throws Exception {
        assumeTrue(datafileVersion >= 4);

        Map<String, Object> testUserAttributes = Collections.singletonMap(ATTRIBUTE_BOOLEAN_KEY, true);
        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_KEY);

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertNull(actualVariation);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEndWithTypedAudienceWithAnd() throws Exception {
        assumeTrue(datafileVersion >= 4);

        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping()
            .get(EXPERIMENT_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_KEY);
        Map<String, Object> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_DOUBLE_KEY, 99.9); // should be lt 100.
        testUserAttributes.put(ATTRIBUTE_BOOLEAN_KEY, true); // should be eq true.
        testUserAttributes.put(ATTRIBUTE_INTEGER_KEY, 2); // should be gt 1.

        logbackVerifier.expectMessage(Level.DEBUG, "This decision will not be saved since the UserProfileService is null.");
        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertNotNull(actualVariation);

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, testUserAttributes);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} DOES NOT dispatch an impression event
     * when the user isn't bucketed to a variation.
     */
    @Test
    public void activateForNullVariation() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Map<String, String> testUserAttributes = Collections.singletonMap("browser_type", "chromey");

        when(mockBucketer.bucket(activatedExperiment, testBucketingId, validProjectConfig)).thenReturn(null);

        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
            activatedExperiment.getKey() + "\".");

        Optimizely optimizely = optimizelyBuilder.withBucketing(mockBucketer).build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertNull(actualVariation);
    }

    /**
     * Verify the case were {@link Optimizely#activate(Experiment, String)} is called with an {@link Experiment}
     * that is not present in the current {@link DatafileProjectConfig}. We should NOT throw an error in that case.
     * <p>
     * This may happen if an experiment is retrieved from the project config, the project config is updated and the
     * referenced experiment removed, then activate is called given the now removed experiment.
     * Could also happen if an experiment was manually created and passed through.
     */
    @Test
    public void activateWhenExperimentIsNotInProject() throws Exception {
        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = optimizelyBuilder
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        Variation actualVariation = optimizely.activate(unknownExperiment, testUserId);
        assertNotNull(actualVariation);

        eventHandler.expectImpression(unknownExperiment.getId(), actualVariation.getId(), testUserId);
    }

    /**
     * Verify that the {@link Optimizely#activate(String, String, Map<String, String>)} call
     * uses forced variation to force the user into the second variation.  The mock bucket returns
     * the first variation. Then remove the forced variation and confirm that the forced variation is null.
     */
    @Test
    public void activateWithExperimentKeyForced() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation forcedVariation = activatedExperiment.getVariations().get(1);

        Map<String, String> testUserAttributes = new HashMap<>();
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            testUserAttributes.put("browser_type", "chrome");
        }

        Optimizely optimizely = optimizelyBuilder.build();
        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, forcedVariation.getKey());

        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertThat(actualVariation, is(forcedVariation));

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, testUserAttributes);

        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, null);
        assertNull(optimizely.getForcedVariation(activatedExperiment.getKey(), testUserId));
    }

    /**
     * Verify that the {@link Optimizely#getVariation(String, String, Map<String, String>)} call
     * uses forced variation to force the user into the second variation.  The mock bucket returns
     * the first variation. Then remove the forced variation and confirm that the forced variation is null.
     */
    @Test
    public void getVariationWithExperimentKeyForced() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation forcedVariation = activatedExperiment.getVariations().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(1);

        Map<String, String> testUserAttributes = new HashMap<>();
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            testUserAttributes.put("browser_type", "chrome");
        }

        Optimizely optimizely = optimizelyBuilder.build();
        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, forcedVariation.getKey());

        Variation actualVariation = optimizely.getVariation(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertThat(actualVariation, is(forcedVariation));

        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, null);
        assertNull(optimizely.getForcedVariation(activatedExperiment.getKey(), testUserId));

        actualVariation = optimizely.getVariation(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertThat(actualVariation, is(bucketedVariation));
    }

    /**
     * Verify that the {@link Optimizely#activate(String, String, Map<String, String>)} call
     * uses forced variation to force the user into the second variation.  The mock bucket returns
     * the first variation. Then remove the forced variation and confirm that the forced variation is null.
     */
    @Test
    public void isFeatureEnabledWithExperimentKeyForced() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        Variation forcedVariation = activatedExperiment.getVariations().get(0);

        Optimizely optimizely = optimizelyBuilder.build();
        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, forcedVariation.getKey());

        // activate the experiment
        assertTrue(optimizely.isFeatureEnabled(FEATURE_FLAG_MULTI_VARIATE_FEATURE.getKey(), testUserId));
        eventHandler.expectImpression(activatedExperiment.getId(), forcedVariation.getId(), testUserId);

        assertTrue(optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, null));
        assertNull(optimizely.getForcedVariation(activatedExperiment.getKey(), testUserId));
        assertFalse(optimizely.isFeatureEnabled(FEATURE_FLAG_MULTI_VARIATE_FEATURE.getKey(), testUserId));
    }

    /**
     * Verify that the {@link Optimizely#activate(String, String, Map<String, String>)} call
     * correctly builds an endpoint url and request params
     * and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateWithExperimentKey() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(1);

        Map<String, String> testUserAttributes = new HashMap<>();
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            testUserAttributes.put("browser_type", "chrome");
        }

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertThat(actualVariation, is(bucketedVariation));

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, testUserAttributes);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} handles the case where an unknown experiment
     * (i.e., not in the config) is passed through and a {@link NoOpErrorHandler} is used by default.
     */
    @Test
    public void activateWithUnknownExperimentKeyAndNoOpErrorHandler() throws Exception {
        Experiment unknownExperiment = createUnknownExperiment();
        Optimizely optimizely = optimizelyBuilder.withErrorHandler(new NoOpErrorHandler()).build();

        logbackVerifier.expectMessage(Level.ERROR, "Experiment \"unknown_experiment\" is not in the datafile.");
        logbackVerifier.expectMessage(Level.INFO,
            "Not activating user \"userId\" for experiment \"unknown_experiment\".");

        Variation actualVariation = optimizely.activate(unknownExperiment.getKey(), testUserId);
        assertNull(actualVariation);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} passes through attributes.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void activateWithAttributes() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(1);
        Attribute attributeString = validProjectConfig.getAttributes().get(0);

        Map<String, String> attr = Collections.singletonMap(attributeString.getKey(), "attributeValue");

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, attr);
        assertThat(actualVariation, is(bucketedVariation));

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, attr);
    }

    @Test
    public void activateWithTypedAttributes() throws Exception {
        assumeTrue(datafileVersion >= 4);

        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(1);

        Attribute attributeString = validProjectConfig.getAttributes().get(0);
        Attribute attributeBoolean = validProjectConfig.getAttributes().get(3);
        Attribute attributeInteger = validProjectConfig.getAttributes().get(4);
        Attribute attributeDouble = validProjectConfig.getAttributes().get(5);

        Map<String, Object> attr = new HashMap<>();
        attr.put(attributeString.getKey(), "attributeValue");
        attr.put(attributeBoolean.getKey(), true);
        attr.put(attributeInteger.getKey(), 3);
        attr.put(attributeDouble.getKey(), 3.123);

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, attr);
        assertThat(actualVariation, is(bucketedVariation));

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, attr);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map<String, String>)} handles the case
     * where an unknown attribute (i.e., not in the config) is passed through.
     * <p>
     * In this case, the eventual payload will NOT include the unknownAttribute
     */
    @Test
    @SuppressWarnings("unchecked")
    public void activateWithUnknownAttribute() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(1);

        Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put("unknownAttribute", "dimValue");

        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");

        Optimizely optimizely = optimizelyBuilder.withErrorHandler(new RaiseExceptionErrorHandler()).build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertThat(actualVariation, is(bucketedVariation));

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} if passed null attributes than it returns null attributes.
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void activateWithNullAttributes() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(1);

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, null);
        assertThat(actualVariation, is(bucketedVariation));

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} gracefully handles null attribute values.
     * Null values are striped within the EventFactory. Not sure the intent of this test.
     */
    @Test
    public void activateWithNullAttributeValues() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(1);
        Attribute attribute = validProjectConfig.getAttributes().get(0);

        Map<String, String> attributes = Collections.singletonMap(attribute.getKey(), null);

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, attributes);
        assertThat(actualVariation, is(bucketedVariation));

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} returns null when the experiment id corresponds to a
     * non-running experiment.
     */
    @Test
    public void activateDraftExperiment() throws Exception {
        Experiment inactiveExperiment;
        if (datafileVersion == 4) {
            inactiveExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_PAUSED_EXPERIMENT_KEY);
        } else {
            inactiveExperiment = validProjectConfig.getExperiments().get(1);
        }

        Optimizely optimizely = optimizelyBuilder.build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"" + inactiveExperiment.getKey() +
            "\" is not running.");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
            inactiveExperiment.getKey() + "\".");

        Variation variation = optimizely.activate(inactiveExperiment.getKey(), testUserId);
        assertNull(variation);
    }

    /**
     * Verify that a user who falls in an experiment's audience is assigned a variation.
     */
    @Test
    public void activateUserInAudience() throws Exception {
        Experiment experimentToCheck = validProjectConfig.getExperiments().get(0);

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(experimentToCheck.getKey(), testUserId);
        assertNotNull(actualVariation);

        eventHandler.expectImpression(experimentToCheck.getId(), actualVariation.getId(), testUserId);
    }

    /**
     * Verify that if user ID sent is null will return null variation.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void activateUserIDIsNull() throws Exception {
        Experiment experimentToCheck = validProjectConfig.getExperiments().get(0);

        Optimizely optimizely = optimizelyBuilder.build();
        Variation nullVariation = optimizely.activate(experimentToCheck.getKey(), null);
        assertNull(nullVariation);

        logbackVerifier.expectMessage(Level.ERROR, "The user ID parameter must be nonnull.");
    }

    /**
     * Verify that if Experiment key sent is null will return null variation.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void activateExperimentKeyIsNull() throws Exception {
        Optimizely optimizely = optimizelyBuilder.build();
        Variation nullVariation = optimizely.activate((String) null, testUserId);
        assertNull(nullVariation);

        logbackVerifier.expectMessage(Level.ERROR, "The experimentKey parameter must be nonnull.");
    }

    /**
     * Verify that a user not in any of an experiment's audiences isn't assigned to a variation.
     */
    @Test
    public void activateUserNotInAudience() throws Exception {
        Experiment experimentToCheck;
        if (datafileVersion == 4) {
            experimentToCheck = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        } else {
            experimentToCheck = validProjectConfig.getExperiments().get(0);
        }

        Map<String, String> testUserAttributes = Collections.singletonMap("browser_type", "firefox");

        logbackVerifier.expectMessage(Level.INFO,
            "User \"userId\" does not meet conditions to be in experiment \"" +
                experimentToCheck.getKey() + "\".");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
            experimentToCheck.getKey() + "\".");

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(experimentToCheck.getKey(), testUserId, testUserAttributes);
        assertNull(actualVariation);
    }

    /**
     * Verify that when no audiences are provided, the user is included in the experiment (i.e., no audiences means
     * the experiment is targeted to "everyone").
     */
    @Test
    public void activateUserWithNoAudiences() throws Exception {
        Experiment experimentToCheck = noAudienceProjectConfig.getExperiments().get(0);
        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(experimentToCheck.getKey(), testUserId);
        assertNotNull(actualVariation);

        eventHandler.expectImpression(experimentToCheck.getId(), actualVariation.getId(), testUserId);
    }

    /**
     * Verify that when an experiment has audiences, but no attributes are provided, the user is not assigned a
     * variation.
     */
    @Test
    public void activateUserNoAttributesWithAudiences() throws Exception {
        Experiment experiment;
        if (datafileVersion == 4) {
            experiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        } else {
            experiment = validProjectConfig.getExperiments().get(0);
        }

        Optimizely optimizely = optimizelyBuilder.build();

        /**
         * TBD: This should be fixed.  The v4 datafile does not contain the same condition so
         * results are different.  We have made a change around 9/7/18 where we evaluate audience
         * regardless if you pass in a attribute list or not.  In this case there is a not("broswer_type = "firefox")
         * This causes the user to be bucketed now because they don't have browser_type set to firefox.
         */
        if (datafileVersion == 4) {
            assertNull(optimizely.activate(experiment.getKey(), testUserId));
        } else {
            Variation actualVariation = optimizely.activate(experiment.getKey(), testUserId);
            assertNotNull(actualVariation);
            eventHandler.expectImpression(experiment.getId(), actualVariation.getId(), testUserId, Collections.emptyMap());
        }
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} return a variation when provided an empty string.
     */
    @Test
    public void activateWithEmptyUserId() throws Exception {
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        String experimentKey = experiment.getKey();

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(experimentKey, "");
        assertNotNull(actualVariation);

        eventHandler.expectImpression(experiment.getId(), actualVariation.getId(), "", Collections.emptyMap());
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} returns a variation when given matching
     * user attributes.
     */
    @Test
    public void activateForGroupExperimentWithMatchingAttributes() throws Exception {
        Experiment experiment = validProjectConfig.getGroups()
            .get(0)
            .getExperiments()
            .get(0);
        String userId = testUserId;

        Map<String, String> attributes = new HashMap<>();
        if (datafileVersion == 4) {
            attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
            userId = "user";  // To make sure the user gets allocated.
        } else {
            attributes.put("browser_type", "chrome");
        }

        Optimizely optimizely = optimizelyBuilder.build();
        Variation actualVariation = optimizely.activate(experiment.getKey(), userId, attributes);
        assertNotNull(actualVariation);

        eventHandler.expectImpression(experiment.getId(), actualVariation.getId(), userId, attributes);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} doesn't return a variation when given
     * non-matching user attributes.
     */
    @Test
    public void activateForGroupExperimentWithNonMatchingAttributes() throws Exception {
        Experiment experiment = validProjectConfig.getGroups()
            .get(0)
            .getExperiments()
            .get(0);

        Optimizely optimizely = optimizelyBuilder.build();

        String experimentKey = experiment.getKey();
        logbackVerifier.expectMessage(
            Level.INFO,
            "User \"user\" does not meet conditions to be in experiment \"" + experimentKey + "\".");
        logbackVerifier.expectMessage(Level.INFO,
            "Not activating user \"user\" for experiment \"" + experimentKey + "\".");
        assertNull(optimizely.activate(experiment.getKey(), "user",
            Collections.singletonMap("browser_type", "firefox")));
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} gives precedence to forced variation bucketing
     * over audience evaluation.
     */
    @Test
    public void activateForcedVariationPrecedesAudienceEval() throws Exception {
        Experiment experiment;
        String whitelistedUserId;
        Variation expectedVariation;
        if (datafileVersion == 4) {
            experiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
            whitelistedUserId = MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED;
            expectedVariation = experiment.getVariationKeyToVariationMap().get(VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY);
        } else {
            experiment = validProjectConfig.getExperiments().get(0);
            whitelistedUserId = "testUser1";
            expectedVariation = experiment.getVariations().get(0);
        }

        Optimizely optimizely = optimizelyBuilder.build();

        logbackVerifier.expectMessage(Level.INFO, "User \"" + whitelistedUserId + "\" is forced in variation \"" +
            expectedVariation.getKey() + "\".");
        // no attributes provided for a experiment that has an audience
        assertTrue(experiment.getUserIdToVariationKeyMap().containsKey(whitelistedUserId));
        Variation actualVariation = optimizely.activate(experiment.getKey(), whitelistedUserId);
        assertThat(actualVariation, is(expectedVariation));

        eventHandler.expectImpression(experiment.getId(), actualVariation.getId(), whitelistedUserId);

    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} gives precedence to experiment status over forced
     * variation bucketing.
     */
    @Test
    public void activateExperimentStatusPrecedesForcedVariation() throws Exception {
        Experiment experiment;
        String whitelistedUserId;
        if (datafileVersion == 4) {
            experiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_PAUSED_EXPERIMENT_KEY);
            whitelistedUserId = PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL;
        } else {
            experiment = validProjectConfig.getExperiments().get(1);
            whitelistedUserId = "testUser3";
        }

        Optimizely optimizely = optimizelyBuilder.build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"" + experiment.getKey() + "\" is not running.");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"" + whitelistedUserId +
            "\" for experiment \"" + experiment.getKey() + "\".");
        // testUser3 has a corresponding forced variation, but experiment status should be checked first
        assertTrue(experiment.getUserIdToVariationKeyMap().containsKey(whitelistedUserId));
        assertNull(optimizely.activate(experiment.getKey(), whitelistedUserId));
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} doesn't dispatch an event for an experiment with a
     * "Launched" status.
     */
    @Test
    public void activateLaunchedExperimentDoesNotDispatchEvent() throws Exception {
        Experiment launchedExperiment = datafileVersion == 4 ?
            noAudienceProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_LAUNCHED_EXPERIMENT_KEY) :
            noAudienceProjectConfig.getExperiments().get(2);

        Optimizely optimizely = optimizelyBuilder.withConfig(noAudienceProjectConfig).build();
        Variation expectedVariation = launchedExperiment.getVariations().get(0);

        // Force variation to launched experiment.
        optimizely.setForcedVariation(launchedExperiment.getKey(), testUserId, expectedVariation.getKey());

        logbackVerifier.expectMessage(Level.INFO,
            "Experiment has \"Launched\" status so not dispatching event during activation.");
        Variation variation = optimizely.activate(launchedExperiment.getKey(), testUserId);
        assertNotNull(variation);
        assertThat(variation.getKey(), is(expectedVariation.getKey()));
    }

    /**
     * Verify that we don't attempt to activate the user when the Optimizely instance is not valid
     */
    @Test
    public void activateWithInvalidDatafile() throws Exception {
        Optimizely optimizely = optimizelyBuilder
            .withDatafile(invalidProjectConfigV5())
            .withConfig(null)
            .withBucketing(mockBucketer)
            .build();

        Variation expectedVariation = optimizely.activate("etag1", genericUserId);
        assertNull(expectedVariation);

        // make sure we didn't even attempt to bucket the user
        verify(mockBucketer, never()).bucket(any(Experiment.class), anyString(), any(ProjectConfig.class));
    }

    //======== track tests ========//

    /**
     * Verify that the {@link Optimizely#track(String, String)} call correctly builds a V2 event and passes it
     * through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void trackEventEndToEndForced() throws Exception {
        EventType eventType = datafileVersion >= 4 ?
            validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY) :
            noAudienceProjectConfig.getEventTypes().get(0);

        Optimizely optimizely = optimizelyBuilder.build();
        optimizely.track(eventType.getKey(), testUserId);
        eventHandler.expectConversion(eventType.getKey(), testUserId);
    }

    /**
     * Verify that the {@link Optimizely#track(String, String)} call correctly builds a V2 event and passes it
     * through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void trackEventEndToEnd() throws Exception {
        EventType eventType = datafileVersion >= 4 ?
            validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY) :
            noAudienceProjectConfig.getEventTypes().get(0);

        Optimizely optimizely = optimizelyBuilder.build();
        optimizely.track(eventType.getKey(), testUserId);
        eventHandler.expectConversion(eventType.getKey(), testUserId);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} handles the case where an unknown event type
     * (i.e., not in the config) is passed through and a {@link NoOpErrorHandler} is used by default.
     */
    @Test
    public void trackEventWithUnknownEventKeyAndNoOpErrorHandler() throws Exception {
        EventType unknownEventType = createUnknownEventType();

        Optimizely optimizely = optimizelyBuilder.withErrorHandler(new NoOpErrorHandler()).build();

        logbackVerifier.expectMessage(Level.ERROR, "Event \"unknown_event_type\" is not in the datafile.");
        logbackVerifier.expectMessage(Level.INFO, "Not tracking event \"unknown_event_type\" for user \"userId\".");
        optimizely.track(unknownEventType.getKey(), testUserId);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} handles the case where an unknown event type
     * (i.e., not in the config) is passed through and a {@link RaiseExceptionErrorHandler} is provided.
     */
    @Test
    public void trackEventWithUnknownEventKeyAndRaiseExceptionErrorHandler() throws Exception {
        thrown.expect(UnknownEventTypeException.class);

        EventType unknownEventType = createUnknownEventType();

        Optimizely optimizely = optimizelyBuilder.withErrorHandler(new RaiseExceptionErrorHandler()).build();

        // since we use a RaiseExceptionErrorHandler, we should throw an error
        optimizely.track(unknownEventType.getKey(), testUserId);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} passes through attributes.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void trackEventWithAttributes() throws Exception {
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = datafileVersion >= 4 ?
            validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY) :
            validProjectConfig.getEventTypes().get(0);

        Optimizely optimizely = optimizelyBuilder.build();

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");

        Map<String, String> attributes = ImmutableMap.of(attribute.getKey(), "attributeValue");
        optimizely.track(eventType.getKey(), genericUserId, attributes);
        eventHandler.expectConversion(eventType.getKey(), genericUserId, attributes);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} if passed null attributes  than it returns null attributes.
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void trackEventWithNullAttributes() throws Exception {
        EventType eventType = datafileVersion >= 4 ?
            validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY) :
            validProjectConfig.getEventTypes().get(0);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");

        Optimizely optimizely = optimizelyBuilder.build();
        optimizely.track(eventType.getKey(), genericUserId, null);
        eventHandler.expectConversion(eventType.getKey(), genericUserId);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} gracefully handles null attribute values.
     */
    @Test
    public void trackEventWithNullAttributeValues() throws Exception {
        EventType eventType = datafileVersion >= 4 ?
            validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY) :
            validProjectConfig.getEventTypes().get(0);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");

        Optimizely optimizely = optimizelyBuilder.build();

        Map<String, String> attributes = Collections.singletonMap("test", null);
        optimizely.track(eventType.getKey(), genericUserId, attributes);
        eventHandler.expectConversion(eventType.getKey(), genericUserId);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} handles the case where an unknown attribute
     * (i.e., not in the config) is passed through.
     * <p>
     * In this case, the track event call should not remove the unknown attribute from the given map but should go on and track the event successfully.
     *
     * TODO: Is this a dupe?? Also not sure the intent of the test since the attributes are stripped by the EventFactory
     */
    @Test
    @SuppressWarnings("unchecked")
    public void trackEventWithUnknownAttribute() throws Exception {
        EventType eventType = datafileVersion >= 4 ?
            validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY) :
            validProjectConfig.getEventTypes().get(0);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");

        Optimizely optimizely = optimizelyBuilder.build();
        optimizely.track(eventType.getKey(), genericUserId, ImmutableMap.of("unknownAttribute", "attributeValue"));
        eventHandler.expectConversion(eventType.getKey(), genericUserId);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map, Map)} passes event features to
     * {@link UserEventFactory#createConversionEvent(ProjectConfig, String, String, String, Map, Map)}
     */
    @Test
    public void trackEventWithEventTags() throws Exception {
        EventType eventType = datafileVersion >= 4 ?
            validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY) :
            validProjectConfig.getEventTypes().get(0);

        Map<String, Object> eventTags = new HashMap<>();
        eventTags.put("int_param", 123);
        eventTags.put("string_param", "123");
        eventTags.put("boolean_param", false);
        eventTags.put("float_param", 12.3f);

        Optimizely optimizely = optimizelyBuilder.build();
        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() + "\" for user \""
            + genericUserId + "\".");

        // call track
        optimizely.track(eventType.getKey(), genericUserId, Collections.emptyMap(), eventTags);
        eventHandler.expectConversion(eventType.getKey(), genericUserId, Collections.emptyMap(), eventTags);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map, Map)} called with null event tags will return null eventTag
     * when calling {@link UserEventFactory#createConversionEvent(ProjectConfig, String, String, String, Map, Map)}
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void trackEventWithNullEventTags() throws Exception {
        EventType eventType = datafileVersion >= 4 ?
            validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY) :
            validProjectConfig.getEventTypes().get(0);

        Optimizely optimizely = optimizelyBuilder.build();

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");

        // call track
        optimizely.track(eventType.getKey(), genericUserId, Collections.emptyMap(), null);
        eventHandler.expectConversion(eventType.getKey(), genericUserId, Collections.emptyMap(), null);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map, Map)} called with null User ID will return and will not track
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void trackEventWithNullOrEmptyUserID() throws Exception {
        EventType eventType = datafileVersion >= 4 ?
            validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY) :
            validProjectConfig.getEventTypes().get(0);

        // call track with null userId
        Optimizely optimizely = optimizelyBuilder.build();
        optimizely.track(eventType.getKey(), null);

        logbackVerifier.expectMessage(Level.ERROR, "The user ID parameter must be nonnull.");
        logbackVerifier.expectMessage(Level.INFO, "Not tracking event \"" + eventType.getKey() + "\".");
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map, Map)} called with null event name will return and will not track
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void trackEventWithNullOrEmptyEventKey() throws Exception {
        // call track with null event key
        Optimizely optimizely = optimizelyBuilder.build();
        optimizely.track(null, genericUserId);

        logbackVerifier.expectMessage(Level.ERROR, "Event Key is null or empty when non-null and non-empty String was expected.");
        logbackVerifier.expectMessage(Level.INFO, "Not tracking event for user \"" + genericUserId + "\".");
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map)} dispatches an event always and logs appropriate message
     */
    @Test
    public void trackEventWithNoValidExperiments() throws Exception {
        EventType eventType = datafileVersion >= 4 ?
            validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY) :
            validProjectConfig.getEventTypes().get(0);

        Attribute attribute = validProjectConfig.getAttributes().get(0);
        Map<String, String> attributes = Collections.singletonMap(attribute.getKey(), "value");

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");

        Optimizely optimizely = optimizelyBuilder.build();
        optimizely.track(eventType.getKey(), genericUserId, attributes);
        eventHandler.expectConversion(eventType.getKey(), genericUserId, attributes);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map)}
     * dispatches events even if the event links only to launched experiments
     */
    @Test
    public void trackDoesNotSendEventWhenExperimentsAreLaunchedOnly() throws Exception {
        EventType eventType = datafileVersion >= 4 ?
            noAudienceProjectConfig.getEventNameMapping().get(EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY) :
            noAudienceProjectConfig.getEventNameMapping().get("launched_exp_event");

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");

        Optimizely optimizely = optimizelyBuilder.withConfig(noAudienceProjectConfig).build();
        optimizely.track(eventType.getKey(), genericUserId);
        eventHandler.expectConversion(eventType.getKey(), genericUserId);
    }

    /**
     * Verify that an event is dispatched even if a user doesn't satisfy audience conditions for an experiment.
     */
    @Test
    public void trackDoesNotSendEventWhenUserDoesNotSatisfyAudiences() throws Exception {
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(2);

        // the audience for the experiments is "NOT firefox" so this user shouldn't satisfy audience conditions
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "firefox");

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");

        Optimizely optimizely = optimizelyBuilder.build();
        optimizely.track(eventType.getKey(), genericUserId, attributeMap);
        eventHandler.expectConversion(eventType.getKey(), genericUserId, attributeMap);
    }

    /**
     * Verify that we don't attempt to track any events if the Optimizely instance is not valid
     */
    @Test
    public void trackWithInvalidDatafile() throws Exception {
        Optimizely optimizely = Optimizely.builder(invalidProjectConfigV5(), mockEventHandler)
            .withBucketing(mockBucketer)
            .build();
        optimizely.track("event_with_launched_and_running_experiments", genericUserId);

        // make sure we didn't even attempt to bucket the user or fire any conversion events
        verify(mockBucketer, never()).bucket(any(Experiment.class), anyString(), any(ProjectConfig.class));
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    //======== getVariation tests ========//

    /**
     * Verify that {@link Optimizely#getVariation(Experiment, String)} correctly makes the
     * {@link Bucketer#bucket(Experiment, String, ProjectConfig)} call and does NOT dispatch an event.
     */
    @Test
    public void getVariation() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        Optimizely optimizely = optimizelyBuilder.withBucketing(mockBucketer).build();

        when(mockBucketer.bucket(activatedExperiment, testUserId, validProjectConfig)).thenReturn(bucketedVariation);

        Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put("browser_type", "chrome");

        // activate the experiment
        Variation actualVariation = optimizely.getVariation(activatedExperiment.getKey(), testUserId,
            testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testUserId, validProjectConfig);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that we didn't attempt to dispatch an event
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} correctly makes the
     * {@link Bucketer#bucket(Experiment, String, ProjectConfig)} call and does NOT dispatch an event.
     */
    @Test
    public void getVariationWithExperimentKey() throws Exception {
        Experiment activatedExperiment = noAudienceProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        Optimizely optimizely = optimizelyBuilder
            .withBucketing(mockBucketer)
            .withConfig(noAudienceProjectConfig)
            .build();

        when(mockBucketer.bucket(activatedExperiment, testUserId, noAudienceProjectConfig)).thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.getVariation(activatedExperiment.getKey(), testUserId);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testUserId, noAudienceProjectConfig);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that we didn't attempt to dispatch an event
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} returns null variation when null or empty
     * experimentKey is sent
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getVariationWithNullExperimentKey() throws Exception {
        Optimizely optimizely = optimizelyBuilder.withConfig(noAudienceProjectConfig).build();

        String nullExperimentKey = null;
        // activate the experiment
        Variation nullVariation = optimizely.getVariation(nullExperimentKey, testUserId);

        assertNull(nullVariation);
        logbackVerifier.expectMessage(Level.ERROR, "The experimentKey parameter must be nonnull.");

    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} handles the case where an unknown experiment
     * (i.e., not in the config) is passed through and a {@link NoOpErrorHandler} is used by default.
     */
    @Test
    public void getVariationWithUnknownExperimentKeyAndNoOpErrorHandler() throws Exception {
        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = optimizelyBuilder
            .withErrorHandler(new NoOpErrorHandler())
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Experiment \"unknown_experiment\" is not in the datafile.");

        // since we use a NoOpErrorHandler, we should fail and return null
        Variation actualVariation = optimizely.getVariation(unknownExperiment.getKey(), testUserId);

        // verify that null is returned, as no project config was available
        assertNull(actualVariation);
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String, Map)} returns a valid variation for a user who
     * falls into the experiment.
     */
    @Test
    public void getVariationWithAudiences() throws Exception {
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = experiment.getVariations().get(0);

        when(mockBucketer.bucket(experiment, testUserId, validProjectConfig)).thenReturn(bucketedVariation);

        Optimizely optimizely = optimizelyBuilder.withBucketing(mockBucketer).build();

        Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put("browser_type", "chrome");

        Variation actualVariation = optimizely.getVariation(experiment.getKey(), testUserId, testUserAttributes);

        verify(mockBucketer).bucket(experiment, testUserId, validProjectConfig);
        assertThat(actualVariation, is(bucketedVariation));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} doesn't return a variation when
     * given an experiment with audiences but no attributes.
     */
    @Test
    public void getVariationWithAudiencesNoAttributes() throws Exception {
        Experiment experiment;
        if (datafileVersion >= 4) {
            experiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        } else {
            experiment = validProjectConfig.getExperiments().get(0);
        }

        Optimizely optimizely = optimizelyBuilder.build();

        Variation actualVariation = optimizely.getVariation(experiment.getKey(), testUserId);
        /**
         * This test now passes because the audience is evaludated even if there is no
         * attributes passed in.  In version 2,3 of the datafile, the audience is a not condition
         * which evaluates to true if it is absent.
         */
        if (datafileVersion >= 4) {
            assertNull(actualVariation);
        } else {
            assertNotNull(actualVariation);
        }
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} returns a variation when given an experiment
     * with no audiences and no user attributes.
     */
    @Test
    public void getVariationNoAudiences() throws Exception {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = experiment.getVariations().get(0);

        when(mockBucketer.bucket(experiment, testUserId, noAudienceProjectConfig)).thenReturn(bucketedVariation);

        Optimizely optimizely = optimizelyBuilder
            .withConfig(noAudienceProjectConfig)
            .withBucketing(mockBucketer)
            .build();

        Variation actualVariation = optimizely.getVariation(experiment.getKey(), testUserId);

        verify(mockBucketer).bucket(experiment, testUserId, noAudienceProjectConfig);
        assertThat(actualVariation, is(bucketedVariation));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} handles the case where an unknown experiment
     * (i.e., not in the config) is passed through and a {@link RaiseExceptionErrorHandler} is provided.
     */
    @Test
    public void getVariationWithUnknownExperimentKeyAndRaiseExceptionErrorHandler() throws Exception {
        thrown.expect(UnknownExperimentException.class);

        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = optimizelyBuilder
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        // since we use a RaiseExceptionErrorHandler, we should throw an error
        optimizely.getVariation(unknownExperiment.getKey(), testUserId);
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} return a variation when provided an
     * empty string.
     */
    @Test
    public void getVariationWithEmptyUserId() throws Exception {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);

        Optimizely optimizely = optimizelyBuilder
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        assertNotNull(optimizely.getVariation(experiment.getKey(), ""));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String, Map)} returns a variation when given matching
     * user attributes.
     */
    @Test
    public void getVariationForGroupExperimentWithMatchingAttributes() throws Exception {
        Experiment experiment = validProjectConfig.getGroups()
            .get(0)
            .getExperiments()
            .get(0);
        Variation variation = experiment.getVariations().get(0);

        Map<String, String> attributes = new HashMap<>();
        if (datafileVersion >= 4) {
            attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            attributes.put("browser_type", "chrome");
        }

        when(mockBucketer.bucket(experiment, "user", validProjectConfig)).thenReturn(variation);

        Optimizely optimizely = optimizelyBuilder.withBucketing(mockBucketer).build();

        assertThat(optimizely.getVariation(experiment.getKey(), "user", attributes),
            is(variation));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String, Map)} doesn't return a variation when given
     * non-matching user attributes.
     */
    @Test
    public void getVariationForGroupExperimentWithNonMatchingAttributes() throws Exception {
        Experiment experiment = validProjectConfig.getGroups()
            .get(0)
            .getExperiments()
            .get(0);

        Optimizely optimizely = optimizelyBuilder.build();

        assertNull(optimizely.getVariation(experiment.getKey(), "user",
            Collections.singletonMap("browser_type", "firefox")));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} gives precedence to experiment status over forced
     * variation bucketing.
     */
    @Test
    public void getVariationExperimentStatusPrecedesForcedVariation() throws Exception {
        Experiment experiment;
        if (datafileVersion >= 4) {
            experiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_PAUSED_EXPERIMENT_KEY);
        } else {
            experiment = validProjectConfig.getExperiments().get(1);
        }

        Optimizely optimizely = optimizelyBuilder.build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"" + experiment.getKey() + "\" is not running.");
        // testUser3 has a corresponding forced variation, but experiment status should be checked first
        assertNull(optimizely.getVariation(experiment.getKey(), "testUser3"));
    }

    /**
     * Verify that we don't attempt to track any events if the Optimizely instance is not valid
     */
    @Test
    public void getVariationWithInvalidDatafile() throws Exception {
        Optimizely optimizely = Optimizely.builder(invalidProjectConfigV5(), mockEventHandler)
            .withBucketing(mockBucketer)
            .build();
        Variation variation = optimizely.getVariation("etag1", genericUserId);

        assertNull(variation);

        // make sure we didn't even attempt to bucket the user
        verify(mockBucketer, never()).bucket(any(Experiment.class), anyString(), any(ProjectConfig.class));
    }

    //======== Notification listeners ========//

    boolean isListenerCalled = false;

    /**
     * Helper method to return decisionListener
     **/
    private NotificationHandler<DecisionNotification> getDecisionListener(
        final String testType,
        final String testUserId,
        final Map<String, ?> testUserAttributes,
        final Map<String, ?> testDecisionInfo)
    {
        return decisionNotification -> {
            assertEquals(decisionNotification.getType(), testType);
            assertEquals(decisionNotification.getUserId(), testUserId);
            assertEquals(decisionNotification.getAttributes(), testUserAttributes);
            for (Map.Entry<String, ?> entry : decisionNotification.getAttributes().entrySet()) {
                assertEquals(testUserAttributes.get(entry.getKey()), entry.getValue());
            }
            for (Map.Entry<String, ?> entry : decisionNotification.getDecisionInfo().entrySet()) {
                assertEquals(testDecisionInfo.get(entry.getKey()), entry.getValue());
            }
            isListenerCalled = true;
        };
    }

    //======Activate Notification TESTS======//

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION")
    public void activateEndToEndWithDecisionListener() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));
        isListenerCalled = false;
        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        Map<String, String> testUserAttributes = new HashMap<>();
        String userId = "Gred";

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(EXPERIMENT_KEY, activatedExperiment.getKey());
        testDecisionInfoMap.put(VARIATION_KEY, "Gred");

        int notificationId = optimizely.notificationCenter.<DecisionNotification>getNotificationManager(DecisionNotification.class)
            .addHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE_TEST.toString(),
                userId,
                testUserAttributes,
                testDecisionInfoMap));

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), userId, null);
        assertThat(actualVariation.getKey(), is("Gred"));

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), userId);

        // Verify that listener being called
        assertTrue(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));

    }

    /**
     * Verify that if user is null than listener will not get called.
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION")
    public void activateUserNullWithListener() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));
        isListenerCalled = false;
        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(EXPERIMENT_KEY, activatedExperiment.getKey());
        testDecisionInfoMap.put(VARIATION_KEY, null);

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.AB_TEST.toString(),
                null,
                Collections.<String, Object>emptyMap(),
                testDecisionInfoMap));

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), null, Collections.<String, Object>emptyMap());
        assertNull(actualVariation);

        // Verify that listener will not get called
        assertFalse(isListenerCalled);

        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));

    }

    /**
     * Verify that a user not in any of an experiment's audiences isn't assigned to a variation.
     */
    @Test
    public void activateUserNotInAudienceWithListener() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));
        isListenerCalled = false;
        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        Map<String, String> testUserAttributes = new HashMap<>();

        testUserAttributes.put("invalid", "invalid");

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(EXPERIMENT_KEY, activatedExperiment.getKey());
        testDecisionInfoMap.put(VARIATION_KEY, null);

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE_TEST.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), genericUserId, testUserAttributes);
        assertNull(actualVariation);

        // Verify that listener being called
        assertTrue(isListenerCalled);

        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));

    }

    //======GetEnabledFeatures Notification TESTS======//

    /**
     * Verify that the {@link Optimizely#getEnabledFeatures(String, Map)}
     * notification listener of getEnabledFeatures is called with multiple FeatureEnabled
     */
    @Test
    public void getEnabledFeaturesWithListenerMultipleFeatureEnabled() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        isListenerCalled = false;

        Optimizely optimizely = optimizelyBuilder.build();

        int notificationId = optimizely.addDecisionNotificationHandler(
            decisionNotification -> {
                isListenerCalled = true;
                assertEquals(decisionNotification.getType(), NotificationCenter.DecisionNotificationType.FEATURE.toString());
            });

        List<String> featureFlags = optimizely.getEnabledFeatures(testUserId, Collections.emptyMap());
        assertEquals(2, featureFlags.size());

        // Why is there only a single impression when there are 2 enabled features?
        eventHandler.expectImpression("1786133852", "1619235542", testUserId);

        // Verify that listener being called
        assertTrue(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify {@link Optimizely#getEnabledFeatures(String, Map)} calls into
     * {@link DecisionService#getVariationForFeature} for each featureFlag sending
     * userId and emptyMap and Mocked {@link Optimizely#isFeatureEnabled(String, String, Map)}
     * to return feature disabled so {@link Optimizely#getEnabledFeatures(String, Map)} will
     * return empty List of FeatureFlags and no notification listener will get called.
     */
    @Test
    public void getEnabledFeaturesWithNoFeatureEnabled() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        isListenerCalled = false;
        Optimizely optimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();

        FeatureDecision featureDecision = new FeatureDecision(null, null, FeatureDecision.DecisionSource.ROLLOUT);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            any(FeatureFlag.class),
            anyString(),
            anyMapOf(String.class, String.class),
            any(ProjectConfig.class)
        );
        int notificationId = optimizely.addDecisionNotificationHandler( decisionNotification -> { });

        List<String> featureFlags = optimizely.getEnabledFeatures(genericUserId, Collections.emptyMap());
        assertTrue(featureFlags.isEmpty());

        // Verify that listener not being called
        assertFalse(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    //======IsFeatureEnabled Notification TESTS======//

    /**
     * Verify that the {@link Optimizely#isFeatureEnabled(String, String, Map<String, String>)}
     * notification listener of isFeatureEnabled is called when feature is in experiment and feature is true
     */
    @Test
    public void isFeatureEnabledWithListenerUserInExperimentFeatureOn() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        isListenerCalled = false;
        final String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        final Map<String, String> testSourceInfo = new HashMap<>();
        testSourceInfo.put(VARIATION_KEY, "George");
        testSourceInfo.put(EXPERIMENT_KEY, "multivariate_experiment");

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(FEATURE_KEY, validFeatureKey);
        testDecisionInfoMap.put(FEATURE_ENABLED, true);
        testDecisionInfoMap.put(SOURCE, FeatureDecision.DecisionSource.FEATURE_TEST.toString());
        testDecisionInfoMap.put(SOURCE_INFO, testSourceInfo);

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertTrue(optimizely.isFeatureEnabled(
            validFeatureKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
        ));

        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        eventHandler.expectImpression(activatedExperiment.getId(), "2099211198", genericUserId, testUserAttributes);

        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is enabled for user \"" + genericUserId + "\"? true"
        );

        // Verify that listener being called
        assertTrue(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#isFeatureEnabled(String, String, Map<String, String>)}
     * notification listener of isFeatureEnabled is called when feature is in experiment and feature is false
     */
    @Test
    public void isFeatureEnabledWithListenerUserInExperimentFeatureOff() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        isListenerCalled = false;

        final String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely optimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        final Map<String, String> testSourceInfo = new HashMap<>();
        testSourceInfo.put(VARIATION_KEY, "variation_toggled_off");
        testSourceInfo.put(EXPERIMENT_KEY, "multivariate_experiment");

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(FEATURE_KEY, validFeatureKey);
        testDecisionInfoMap.put(FEATURE_ENABLED, false);
        testDecisionInfoMap.put(SOURCE, FeatureDecision.DecisionSource.FEATURE_TEST.toString());
        testDecisionInfoMap.put(SOURCE_INFO, testSourceInfo);

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        Variation variation = new Variation("2", "variation_toggled_off", false, null);

        FeatureDecision featureDecision = new FeatureDecision(activatedExperiment, variation, FeatureDecision.DecisionSource.FEATURE_TEST);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            any(FeatureFlag.class),
            anyString(),
            anyMapOf(String.class, String.class),
            any(ProjectConfig.class)
        );

        assertFalse(optimizely.isFeatureEnabled(validFeatureKey, genericUserId, testUserAttributes));
        eventHandler.expectImpression(activatedExperiment.getId(), variation.getId(), genericUserId, testUserAttributes);

        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is enabled for user \"" + genericUserId + "\"? false"
        );

        // Verify that listener being called
        assertTrue(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#isFeatureEnabled(String, String, Map<String, String>)}
     * notification listener of isFeatureEnabled is called when feature is not in experiment and not in rollout
     * returns false
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void isFeatureEnabledWithListenerUserNotInExperimentAndNotInRollOut() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        isListenerCalled = false;
        final String validFeatureKey = "boolean_feature";

        Optimizely optimizely = optimizelyBuilder.build();
        final Map<String, String> testUserAttributes = new HashMap<>();


        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(FEATURE_KEY, validFeatureKey);
        testDecisionInfoMap.put(FEATURE_ENABLED, false);
        testDecisionInfoMap.put(SOURCE, FeatureDecision.DecisionSource.ROLLOUT.toString());
        testDecisionInfoMap.put(SOURCE_INFO, new HashMap<>());

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertFalse(optimizely.isFeatureEnabled(validFeatureKey, genericUserId, null));

        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is enabled for user \"" + genericUserId + "\"? false"
        );

        // Verify that listener being called
        assertTrue(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#isFeatureEnabled(String, String, Map<String, String>)}
     * notification listener of isFeatureEnabled is called when feature is in rollout and featureEnabled is true
     */
    @Test
    public void isFeatureEnabledWithListenerUserInRollOut() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        isListenerCalled = false;
        final String validFeatureKey = "integer_single_variable_feature";

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        testUserAttributes.put(testBucketingIdKey, testBucketingId);

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(VARIATION_KEY, null);
        testDecisionInfoMap.put(FEATURE_KEY, validFeatureKey);
        testDecisionInfoMap.put(FEATURE_ENABLED, true);
        testDecisionInfoMap.put(SOURCE, FeatureDecision.DecisionSource.ROLLOUT.toString());
        testDecisionInfoMap.put(SOURCE_INFO, new HashMap<>());

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertTrue(optimizely.isFeatureEnabled(validFeatureKey, genericUserId, testUserAttributes));

        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is enabled for user \"" + genericUserId + "\"? true"
        );

        // Verify that listener being called
        assertTrue(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    //======GetFeatureVariable Notification TESTS======//

    /**
     * Verify that the {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * notification listener of getFeatureVariableString is called when feature is in experiment and feature is true
     */
    @Test
    public void getFeatureVariableWithListenerUserInExperimentFeatureOn() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));
        isListenerCalled = false;
        final String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;
        String expectedValue = "F";

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        final Map<String, String> testSourceInfo = new HashMap<>();
        testSourceInfo.put(EXPERIMENT_KEY, "multivariate_experiment");
        testSourceInfo.put(VARIATION_KEY, "Fred");

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(FEATURE_KEY, validFeatureKey);
        testDecisionInfoMap.put(FEATURE_ENABLED, true);
        testDecisionInfoMap.put(VARIABLE_KEY, validVariableKey);
        testDecisionInfoMap.put(VARIABLE_TYPE, FeatureVariable.VariableType.STRING.toString());
        testDecisionInfoMap.put(VARIABLE_VALUE, expectedValue);
        testDecisionInfoMap.put(SOURCE, FeatureDecision.DecisionSource.FEATURE_TEST.toString());
        testDecisionInfoMap.put(SOURCE_INFO, testSourceInfo);

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE_VARIABLE.toString(),
                testUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals(optimizely.getFeatureVariableString(
            validFeatureKey,
            validVariableKey,
            testUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            expectedValue);

        // Verify that listener being called
        assertTrue(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * notification listener of getFeatureVariableString is called when feature is in experiment and feature enabled is false
     * than default value will get returned and passing null attribute will send empty map instead of null
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableWithListenerUserInExperimentFeatureOff() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));
        isListenerCalled = false;
        final String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;
        String expectedValue = "H";
        String userID = "Gred";

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, String> testUserAttributes = new HashMap<>();

        final Map<String, String> testSourceInfo = new HashMap<>();
        testSourceInfo.put(EXPERIMENT_KEY, "multivariate_experiment");
        testSourceInfo.put(VARIATION_KEY, "Gred");

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(FEATURE_KEY, validFeatureKey);
        testDecisionInfoMap.put(FEATURE_ENABLED, false);
        testDecisionInfoMap.put(VARIABLE_KEY, validVariableKey);
        testDecisionInfoMap.put(VARIABLE_TYPE, FeatureVariable.VariableType.STRING.toString());
        testDecisionInfoMap.put(VARIABLE_VALUE, expectedValue);
        testDecisionInfoMap.put(SOURCE, FeatureDecision.DecisionSource.FEATURE_TEST.toString());
        testDecisionInfoMap.put(SOURCE_INFO, testSourceInfo);

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE_VARIABLE.toString(),
                userID,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals(optimizely.getFeatureVariableString(
            validFeatureKey,
            validVariableKey,
            userID,
            null),
            expectedValue);

        // Verify that listener being called
        assertTrue(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * notification listener of getFeatureVariableString is called when feature is in rollout and feature enabled is true
     */
    @Test
    public void getFeatureVariableWithListenerUserInRollOutFeatureOn() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        isListenerCalled = false;
        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_STRING_KEY;
        String validVariableKey = VARIABLE_STRING_VARIABLE_KEY;
        String expectedValue = "lumos";

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();

        testDecisionInfoMap.put(EXPERIMENT_KEY, null);
        testDecisionInfoMap.put(FEATURE_KEY, validFeatureKey);
        testDecisionInfoMap.put(FEATURE_ENABLED, true);
        testDecisionInfoMap.put(VARIABLE_KEY, validVariableKey);
        testDecisionInfoMap.put(VARIABLE_TYPE, FeatureVariable.VariableType.STRING.toString());
        testDecisionInfoMap.put(VARIABLE_VALUE, expectedValue);
        testDecisionInfoMap.put(SOURCE, FeatureDecision.DecisionSource.ROLLOUT.toString());
        testDecisionInfoMap.put(SOURCE_INFO, Collections.EMPTY_MAP);

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE_VARIABLE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals(optimizely.getFeatureVariableString(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            expectedValue);

        // Verify that listener being called
        assertTrue(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableBoolean(String, String, String, Map)}
     * notification listener of getFeatureVariableBoolean is called when feature is not in rollout and feature enabled is false
     */
    @Test
    public void getFeatureVariableWithListenerUserNotInRollOutFeatureOff() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        isListenerCalled = false;
        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY;
        String validVariableKey = VARIABLE_BOOLEAN_VARIABLE_KEY;
        Boolean expectedValue = true;

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();

        testDecisionInfoMap.put(EXPERIMENT_KEY, null);
        testDecisionInfoMap.put(FEATURE_KEY, validFeatureKey);
        testDecisionInfoMap.put(FEATURE_ENABLED, false);
        testDecisionInfoMap.put(VARIABLE_KEY, validVariableKey);
        testDecisionInfoMap.put(VARIABLE_TYPE, FeatureVariable.VariableType.BOOLEAN.toString());
        testDecisionInfoMap.put(VARIABLE_VALUE, expectedValue);
        testDecisionInfoMap.put(SOURCE, FeatureDecision.DecisionSource.ROLLOUT.toString());
        testDecisionInfoMap.put(SOURCE_INFO, Collections.EMPTY_MAP);

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE_VARIABLE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals(optimizely.getFeatureVariableBoolean(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            expectedValue);

        // Verify that listener being called
        assertTrue(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableInteger(String, String, String, Map)}
     * notification listener of getFeatureVariableInteger is called when feature is in rollout and feature enabled is true
     */
    @Test
    public void getFeatureVariableIntegerWithListenerUserInRollOutFeatureOn() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        isListenerCalled = false;
        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_INTEGER_KEY;
        String validVariableKey = VARIABLE_INTEGER_VARIABLE_KEY;
        int expectedValue = 7;

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(EXPERIMENT_KEY, null);
        testDecisionInfoMap.put(FEATURE_KEY, validFeatureKey);
        testDecisionInfoMap.put(FEATURE_ENABLED, true);
        testDecisionInfoMap.put(VARIABLE_KEY, validVariableKey);
        testDecisionInfoMap.put(VARIABLE_TYPE, FeatureVariable.VariableType.INTEGER.toString());
        testDecisionInfoMap.put(VARIABLE_VALUE, expectedValue);
        testDecisionInfoMap.put(SOURCE, FeatureDecision.DecisionSource.ROLLOUT.toString());
        testDecisionInfoMap.put(SOURCE_INFO, Collections.EMPTY_MAP);

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE_VARIABLE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals((long) optimizely.getFeatureVariableInteger(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            (long) expectedValue);

        // Verify that listener being called
        assertTrue(isListenerCalled);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableDouble(String, String, String, Map)}
     * notification listener of getFeatureVariableDouble is called when feature is in experiment and feature enabled is true
     */
    @SuppressFBWarnings("CNT_ROUGH_CONSTANT_VALUE")
    @Test
    public void getFeatureVariableDoubleWithListenerUserInExperimentFeatureOn() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        isListenerCalled = false;
        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_DOUBLE_KEY;
        String validVariableKey = VARIABLE_DOUBLE_VARIABLE_KEY;

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_SLYTHERIN_VALUE);

        final Map<String, String> testSourceInfo = new HashMap<>();
        testSourceInfo.put(EXPERIMENT_KEY, "double_single_variable_feature_experiment");
        testSourceInfo.put(VARIATION_KEY, "pi_variation");

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(FEATURE_KEY, validFeatureKey);
        testDecisionInfoMap.put(FEATURE_ENABLED, true);
        testDecisionInfoMap.put(VARIABLE_KEY, validVariableKey);
        testDecisionInfoMap.put(VARIABLE_TYPE, FeatureVariable.VariableType.DOUBLE.toString());
        testDecisionInfoMap.put(VARIABLE_VALUE, 3.14);
        testDecisionInfoMap.put(SOURCE, FeatureDecision.DecisionSource.FEATURE_TEST.toString());
        testDecisionInfoMap.put(SOURCE_INFO, testSourceInfo);

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.FEATURE_VARIABLE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals(optimizely.getFeatureVariableDouble(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_SLYTHERIN_VALUE)),
            Math.PI, 2);

        // Verify that listener being called
        assertTrue(isListenerCalled);

        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#activate(String, String, Map<String, String>)} call
     * correctly builds an endpoint url and request params
     * and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateWithListener() throws Exception {
        final Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        final Variation bucketedVariation = activatedExperiment.getVariations().get(1);

        final Map<String, String> testUserAttributes = new HashMap<>();
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            testUserAttributes.put("browser_type", "chrome");
        }

        NotificationHandler<ActivateNotification> activateNotification = message -> {
            assertEquals(activatedExperiment.getKey(), message.getExperiment().getKey());
            assertEquals(bucketedVariation.getKey(), message.getVariation().getKey());
            assertEquals(testUserId, message.getUserId());
            assertEquals(testUserAttributes, message.getAttributes());

            assertEquals(RequestMethod.POST, message.getEvent().getRequestMethod());
        };

        Optimizely optimizely = optimizelyBuilder.build();
        int notificationId = optimizely.addNotificationHandler(ActivateNotification.class, activateNotification);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);
        assertThat(actualVariation, is(bucketedVariation));

        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, testUserAttributes);

        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void activateWithListenerNullAttributes() throws Exception {
        final Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        final Variation bucketedVariation = activatedExperiment.getVariations().get(1);

        NotificationHandler<ActivateNotification> activateNotification = message -> {
            assertEquals(activatedExperiment.getKey(), message.getExperiment().getKey());
            assertEquals(bucketedVariation.getKey(), message.getVariation().getKey());
            assertEquals(testUserId, message.getUserId());
            assertNull(message.getAttributes());

            assertEquals(RequestMethod.POST, message.getEvent().getRequestMethod());
        };

        Optimizely optimizely = optimizelyBuilder.build();
        int notificationId = optimizely.addNotificationHandler(ActivateNotification.class, activateNotification);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, null);
        assertThat(actualVariation, is(bucketedVariation));
        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId);

        optimizely.notificationCenter.removeNotificationListener(notificationId);
    }

    /**
     * Verify that {@link com.optimizely.ab.notification.NotificationCenter#addNotificationListener(
     *com.optimizely.ab.notification.NotificationCenter.NotificationType,
     * com.optimizely.ab.notification.NotificationListener)} properly used
     * and the listener is
     * added and notified when an experiment is activated.
     *
     * Feels redundant with the above tests
     */
    @SuppressWarnings("unchecked")
    @Test
    public void addNotificationListenerFromNotificationCenter() throws Exception {
        Experiment activatedExperiment;
        EventType eventType;
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_BASIC_EXPERIMENT_KEY);
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            eventType = validProjectConfig.getEventTypes().get(0);
        }
        Variation bucketedVariation = activatedExperiment.getVariations().get(1);
        Map<String, String> attributes = Collections.emptyMap();

        Optimizely optimizely = optimizelyBuilder.build();

        // Add listener
        ActivateNotificationListener listener = mock(ActivateNotificationListener.class);
        optimizely.addNotificationHandler(ActivateNotification.class, listener);

        // Check if listener is notified when experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, testUserId, attributes);
        verify(listener, times(1))
            .onActivate(eq(activatedExperiment), eq(testUserId), eq(attributes), eq(bucketedVariation), any(LogEvent.class));

        assertEquals(actualVariation.getKey(), bucketedVariation.getKey());
        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, attributes);

        // Check if listener is notified after an event is tracked
        String eventKey = eventType.getKey();

        NotificationHandler<TrackNotification> trackNotification = mock(NotificationHandler.class);
        optimizely.addTrackNotificationHandler(trackNotification);

        optimizely.track(eventKey, testUserId, attributes);
        verify(trackNotification, times(1)).handle(any(TrackNotification.class));
        eventHandler.expectConversion(eventType.getKey(), testUserId);
    }

    /**
     * Verify that {@link com.optimizely.ab.notification.NotificationCenter} properly
     * calls and the listener is removed and no longer notified when an experiment is activated.
     *
     * TODO move this to NotificationCenter.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void removeNotificationListenerNotificationCenter() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);

        Optimizely optimizely = optimizelyBuilder.build();

        Map<String, String> attributes = new HashMap<>();
        if (datafileVersion >= 4) {
            attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            attributes.put("browser_type", "chrome");
        }

        // Add and remove listener
        ActivateNotificationListener activateNotification = mock(ActivateNotificationListener.class);
        int notificationId = optimizely.addNotificationHandler(ActivateNotification.class, activateNotification);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));

        NotificationHandler<TrackNotification> trackNotification = mock(NotificationHandler.class);
        notificationId = optimizely.addTrackNotificationHandler(trackNotification);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));

        // Check if listener is notified after an experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, testUserId, attributes);
        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, attributes);

        verify(activateNotification, never())
            .onActivate(eq(activatedExperiment), eq(testUserId), eq(attributes), eq(actualVariation), any(LogEvent.class));

        // Check if listener is notified after an event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        optimizely.track(eventKey, testUserId, attributes);
        eventHandler.expectConversion(eventKey, testUserId, attributes);

        verify(trackNotification, never()).handle(any(TrackNotification.class));
    }

    /**
     * Verify that {@link com.optimizely.ab.notification.NotificationCenter}
     * clearAllListerners removes all listeners
     * and no longer notified when an experiment is activated.
     *
     * TODO Should be part of NotificationCenter tests.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void clearNotificationListenersNotificationCenter() throws Exception {
        Experiment activatedExperiment;
        Map<String, String> attributes = new HashMap<>();
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
            attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            attributes.put("browser_type", "chrome");
        }

        Optimizely optimizely = optimizelyBuilder.build();

        ActivateNotificationListener activateNotification = mock(ActivateNotificationListener.class);
        NotificationHandler<TrackNotification> trackNotification = mock(NotificationHandler.class);

        optimizely.addNotificationHandler(ActivateNotification.class, activateNotification);
        optimizely.addTrackNotificationHandler(trackNotification);

        optimizely.notificationCenter.clearAllNotificationListeners();

        // Check if listener is notified after an experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, testUserId, attributes);
        eventHandler.expectImpression(activatedExperiment.getId(), actualVariation.getId(), testUserId, attributes);

        // Check if listener is notified after a feature variable is accessed
        verify(activateNotification, never())
            .onActivate(eq(activatedExperiment), eq(testUserId), eq(attributes), eq(actualVariation), any(LogEvent.class));

        // Check if listener is notified after a event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        optimizely.track(eventKey, testUserId, attributes);
        eventHandler.expectConversion(eventKey, testUserId, attributes);

        verify(trackNotification, never()).handle(any(TrackNotification.class));
    }

    /**
     * Add notificaiton listener for track {@link com.optimizely.ab.notification.NotificationCenter}.  Verify called and
     * remove.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void trackEventWithListenerAttributes() throws Exception {
        final Attribute attribute = validProjectConfig.getAttributes().get(0);
        final EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, String> attributes = ImmutableMap.of(attribute.getKey(), "attributeValue");

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + testUserId + "\".");

        NotificationHandler<TrackNotification> trackNotification = message -> {
            assertEquals(eventType.getKey(), message.getEventKey());
            assertEquals(testUserId, message.getUserId());
            assertEquals(attributes, message.getAttributes());
            assertTrue(message.getEventTags().isEmpty());
        };

        int notificationId = optimizely.addTrackNotificationHandler(trackNotification);

        // call track
        optimizely.track(eventType.getKey(), testUserId, attributes);
        eventHandler.expectConversion(eventType.getKey(), testUserId, attributes);

        optimizely.notificationCenter.removeNotificationListener(notificationId);
    }

    /**
     * Track with listener and verify that {@link Optimizely#track(String, String)} returns null attributes.
     * TODO I think these are the same tests, but now with an event handler... :/ perhaps we combine.
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void trackEventWithListenerNullAttributes() throws Exception {
        final EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

        Optimizely optimizely = optimizelyBuilder.build();

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + testUserId + "\".");

        NotificationHandler<TrackNotification> trackNotification = message -> {
            assertEquals(eventType.getKey(), message.getEventKey());
            assertEquals(testUserId, message.getUserId());
            assertNull(message.getAttributes());
            assertTrue(message.getEventTags().isEmpty());
        };

        int notificationId = optimizely.addTrackNotificationHandler(trackNotification);

        // call track
        optimizely.track(eventType.getKey(), testUserId, null);
        eventHandler.expectConversion(eventType.getKey(), testUserId);

        optimizely.notificationCenter.removeNotificationListener(notificationId);
    }

    //======== Feature Accessor Tests ========//

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns null and logs a message
     * when it is called with a feature key that has no corresponding feature in the datafile.
     */
    @Test
    public void getFeatureVariableValueForTypeReturnsNullWhenFeatureNotFound() throws Exception {
        String invalidFeatureKey = "nonexistent feature key";
        String invalidVariableKey = "nonexistent variable key";
        Map<String, String> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        Optimizely optimizely = optimizelyBuilder.build();

        String value = optimizely.getFeatureVariableValueForType(
            invalidFeatureKey,
            invalidVariableKey,
            genericUserId,
            Collections.<String, String>emptyMap(),
            FeatureVariable.VariableType.STRING);
        assertNull(value);

        value = optimizely.getFeatureVariableString(invalidFeatureKey, invalidVariableKey, genericUserId, attributes);
        assertNull(value);

        logbackVerifier.expectMessage(Level.INFO,
            "No feature flag was found for key \"" + invalidFeatureKey + "\".",
            2);
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns null and logs a message
     * when the feature key is valid, but no variable could be found for the variable key in the feature.
     */
    @Test
    public void getFeatureVariableValueForTypeReturnsNullWhenVariableNotFoundInValidFeature() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String invalidVariableKey = "nonexistent variable key";
        Optimizely optimizely = optimizelyBuilder.build();

        String value = optimizely.getFeatureVariableValueForType(
            FEATURE_MULTI_VARIATE_FEATURE_KEY,
            invalidVariableKey,
            genericUserId,
            Collections.<String, String>emptyMap(),
            FeatureVariable.VariableType.STRING);
        assertNull(value);

        logbackVerifier.expectMessage(Level.INFO,
            "No feature variable was found for key \"" + invalidVariableKey + "\" in feature flag \"" +
                validFeatureKey + "\".");
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns null when the variable's type does not match the type with which it was attempted to be accessed.
     */
    @Test
    public void getFeatureVariableValueReturnsNullWhenVariableTypeDoesNotMatch() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;
        Optimizely optimizely = optimizelyBuilder.build();

        String value = optimizely.getFeatureVariableValueForType(
            FEATURE_MULTI_VARIATE_FEATURE_KEY,
            validVariableKey,
            genericUserId,
            Collections.<String, String>emptyMap(),
            FeatureVariable.VariableType.INTEGER
        );
        assertNull(value);

        logbackVerifier.expectMessage(
            Level.INFO,
            "The feature variable \"" + validVariableKey +
                "\" is actually of type \"" + FeatureVariable.VariableType.STRING.toString() +
                "\" type. You tried to access it as type \"" + FeatureVariable.VariableType.INTEGER.toString() +
                "\". Please use the appropriate feature variable accessor."
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns the String default value of a feature variable
     * when the feature is not attached to an experiment or a rollout.
     */
    @Test
    public void getFeatureVariableValueForTypeReturnsDefaultValueWhenFeatureIsNotAttachedToExperimentOrRollout() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY;
        String validVariableKey = VARIABLE_BOOLEAN_VARIABLE_KEY;
        Boolean defaultValue = Boolean.parseBoolean(VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE);
        Map<String, String> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        Optimizely optimizely = optimizelyBuilder.build();

        Boolean value = optimizely.getFeatureVariableValueForType(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            attributes,
            FeatureVariable.VariableType.BOOLEAN);
        assertEquals(defaultValue, value);

        logbackVerifier.expectMessage(
            Level.INFO,
            "The feature flag \"" + validFeatureKey + "\" is not used in any experiments."
        );
        logbackVerifier.expectMessage(
            Level.INFO,
            "The feature flag \"" + validFeatureKey + "\" is not used in a rollout."
        );
        logbackVerifier.expectMessage(
            Level.INFO,
            "User \"" + genericUserId + "\" was not bucketed into any variation for feature flag \"" +
                validFeatureKey + "\". The default value \"" +
                defaultValue + "\" for \"" +
                validVariableKey + "\" is being returned."
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns the String default value for a feature variable
     * when the feature is attached to an experiment and no rollout, but the user is excluded from the experiment.
     */
    @Test
    public void getFeatureVariableValueReturnsDefaultValueWhenFeatureIsAttachedToOneExperimentButFailsTargeting() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_SINGLE_VARIABLE_DOUBLE_KEY;
        String validVariableKey = VARIABLE_DOUBLE_VARIABLE_KEY;
        Double expectedValue = Double.parseDouble(VARIABLE_DOUBLE_DEFAULT_VALUE);
        FeatureFlag featureFlag = FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE;
        Experiment experiment = validProjectConfig.getExperimentIdMapping().get(featureFlag.getExperimentIds().get(0));

        Optimizely optimizely = optimizelyBuilder.build();

        Double valueWithImproperAttributes = optimizely.getFeatureVariableValueForType(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, "Ravenclaw"),
            FeatureVariable.VariableType.DOUBLE
        );
        assertEquals(expectedValue, valueWithImproperAttributes);

        logbackVerifier.expectMessage(
            Level.INFO,
            "User \"" + genericUserId + "\" does not meet conditions to be in experiment \"" +
                experiment.getKey() + "\"."
        );
        logbackVerifier.expectMessage(
            Level.INFO,
            "The feature flag \"" + validFeatureKey + "\" is not used in a rollout."
        );
        logbackVerifier.expectMessage(
            Level.INFO,
            "User \"" + genericUserId +
                "\" was not bucketed into any variation for feature flag \"" + validFeatureKey +
                "\". The default value \"" + expectedValue +
                "\" for \"" + validVariableKey + "\" is being returned."
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * is called when the variation is not null and feature enabled is false
     * returns the default variable value
     */
    @Test
    public void getFeatureVariableValueReturnsDefaultValueWhenFeatureEnabledIsFalse() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;
        String expectedValue = VARIABLE_FIRST_LETTER_DEFAULT_VALUE;
        Experiment multivariateExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);

        Optimizely optimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();

        FeatureDecision featureDecision = new FeatureDecision(multivariateExperiment, VARIATION_MULTIVARIATE_EXPERIMENT_GRED, FeatureDecision.DecisionSource.FEATURE_TEST);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            FEATURE_FLAG_MULTI_VARIATE_FEATURE,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE),
            validProjectConfig
        );

        String value = optimizely.getFeatureVariableValueForType(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE),
            FeatureVariable.VariableType.STRING
        );

        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey + "\" for variation \"Gred\" was not enabled. " +
                "The default value is being returned."
        );

        assertEquals(expectedValue, value);
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * is called when feature is in experiment and feature enabled is true
     * returns variable value
     */
    @Test
    public void getFeatureVariableUserInExperimentFeatureOn() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;
        String expectedValue = "F";

        Optimizely optimizely = optimizelyBuilder.build();

        assertEquals(optimizely.getFeatureVariableString(
            validFeatureKey,
            validVariableKey,
            testUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            expectedValue);
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * is called when feature is in experiment and feature enabled is false
     * than default value will gets returned
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableUserInExperimentFeatureOff() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;
        String expectedValue = "H";
        String userID = "Gred";

        Optimizely optimizely = optimizelyBuilder.build();

        assertEquals(optimizely.getFeatureVariableString(
            validFeatureKey,
            validVariableKey,
            userID,
            null),
            expectedValue);
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * is called when feature is in rollout and feature enabled is true
     * returns variable value
     */
    @Test
    public void getFeatureVariableUserInRollOutFeatureOn() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_STRING_KEY;
        String validVariableKey = VARIABLE_STRING_VARIABLE_KEY;
        String expectedValue = "lumos";

        Optimizely optimizely = optimizelyBuilder.build();

        assertEquals(optimizely.getFeatureVariableString(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            expectedValue);
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableBoolean(String, String, String, Map)}
     * is called when feature is not in rollout and feature enabled is false
     * returns default value
     */
    @Test
    public void getFeatureVariableUserNotInRollOutFeatureOff() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY;
        String validVariableKey = VARIABLE_BOOLEAN_VARIABLE_KEY;
        Boolean expectedValue = true;

        Optimizely optimizely = optimizelyBuilder.build();

        assertEquals(optimizely.getFeatureVariableBoolean(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            expectedValue);
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableInteger(String, String, String, Map)}
     * is called when feature is in rollout and feature enabled is true
     * return rollout variable value
     */
    @Test
    public void getFeatureVariableIntegerUserInRollOutFeatureOn() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_INTEGER_KEY;
        String validVariableKey = VARIABLE_INTEGER_VARIABLE_KEY;
        int expectedValue = 7;

        Optimizely optimizely = optimizelyBuilder.build();

        assertEquals((long) optimizely.getFeatureVariableInteger(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            (long) expectedValue);
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableDouble(String, String, String, Map)}
     * is called when feature is in experiment and feature enabled is true
     * returns variable value
     */
    @Test
    public void getFeatureVariableDoubleUserInExperimentFeatureOn() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_DOUBLE_KEY;
        String validVariableKey = VARIABLE_DOUBLE_VARIABLE_KEY;

        Optimizely optimizely = optimizelyBuilder.build();

        assertEquals(optimizely.getFeatureVariableDouble(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_SLYTHERIN_VALUE)),
            Math.PI, 2);
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns the default value for the feature variable
     * when there is no variable usage present for the variation the user is bucketed into.
     */
    @Test
    public void getFeatureVariableValueReturnsDefaultValueWhenNoVariationUsageIsPresent() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_SINGLE_VARIABLE_INTEGER_KEY;
        String validVariableKey = VARIABLE_INTEGER_VARIABLE_KEY;
        FeatureVariable variable = FEATURE_FLAG_SINGLE_VARIABLE_INTEGER.getVariableKeyToFeatureVariableMap().get(validVariableKey);
        Integer expectedValue = Integer.parseInt(variable.getDefaultValue());

        Optimizely optimizely = optimizelyBuilder.build();

        Integer value = optimizely.getFeatureVariableValueForType(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.<String, String>emptyMap(),
            FeatureVariable.VariableType.INTEGER
        );

        assertEquals(expectedValue, value);
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return False
     * when the APIs are called with a null value for the feature key parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void isFeatureEnabledReturnsFalseWhenFeatureKeyIsNull() throws Exception {
        Optimizely spyOptimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();
        assertFalse(spyOptimizely.isFeatureEnabled(null, genericUserId));

        logbackVerifier.expectMessage(Level.WARN, "The featureKey parameter must be nonnull.");

        verify(mockDecisionService, never()).getVariationForFeature(
            any(FeatureFlag.class),
            any(String.class),
            anyMapOf(String.class, String.class),
            any(ProjectConfig.class)
        );
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return False
     * when the APIs are called with a null value for the user ID parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void isFeatureEnabledReturnsFalseWhenUserIdIsNull() throws Exception {
        Optimizely optimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();
        assertFalse(optimizely.isFeatureEnabled(FEATURE_MULTI_VARIATE_FEATURE_KEY, null));

        logbackVerifier.expectMessage(Level.WARN, "The userId parameter must be nonnull.");

        verify(mockDecisionService, never()).getVariationForFeature(
            any(FeatureFlag.class),
            any(String.class),
            anyMapOf(String.class, String.class),
            any(ProjectConfig.class)
        );
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return False
     * when the APIs are called with a feature key that is not in the datafile.
     */
    @Test
    public void isFeatureEnabledReturnsFalseWhenFeatureFlagKeyIsInvalid() throws Exception {

        String invalidFeatureKey = "nonexistent feature key";

        Optimizely spyOptimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();
        assertFalse(spyOptimizely.isFeatureEnabled(invalidFeatureKey, genericUserId));

        logbackVerifier.expectMessage(Level.INFO, "No feature flag was found for key \"" + invalidFeatureKey + "\".");

        verify(mockDecisionService, never()).getVariation(
            any(Experiment.class),
            anyString(),
            anyMapOf(String.class, String.class),
            any(ProjectConfig.class)
        );
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return False
     * when the user is not bucketed into any variation for the feature.
     */
    @Test
    public void isFeatureEnabledReturnsFalseWhenUserIsNotBucketedIntoAnyVariation() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely optimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();

        FeatureDecision featureDecision = new FeatureDecision(null, null, null);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            any(FeatureFlag.class),
            anyString(),
            anyMapOf(String.class, String.class),
            any(ProjectConfig.class)
        );

        assertFalse(optimizely.isFeatureEnabled(validFeatureKey, genericUserId));

        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is enabled for user \"" + genericUserId + "\"? false"
        );

        verify(mockDecisionService).getVariationForFeature(
            eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(validProjectConfig)
        );
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return True when the user is bucketed into a variation for the feature.
     * An impression event should not be dispatched since the user was not bucketed into an Experiment.
     */
    @Test
    public void isFeatureEnabledReturnsTrueButDoesNotSendWhenUserIsBucketedIntoVariationWithoutExperiment() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        Optimizely optimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();

        // Should be an experiment from the rollout associated with the feature, but for this test
        // it doesn't matter. Just use any valid experiment.
        Experiment experiment = validProjectConfig.getRolloutIdMapping().get(ROLLOUT_2_ID).getExperiments().get(0);
        Variation variation = new Variation("variationId", "variationKey", true, null);
        FeatureDecision featureDecision = new FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.ROLLOUT);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(validProjectConfig)
        );

        assertTrue(optimizely.isFeatureEnabled(validFeatureKey, genericUserId));

        logbackVerifier.expectMessage(
            Level.INFO,
            "The user \"" + genericUserId +
                "\" is not included in an experiment for feature \"" + validFeatureKey + "\"."
        );
        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is enabled for user \"" + genericUserId + "\"? true"
        );

        verify(mockDecisionService).getVariationForFeature(
            eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(validProjectConfig)
        );
    }

    /**
     * Verify that the {@link Optimizely#activate(String, String, Map<String, String>)} call
     * uses forced variation to force the user into the third variation in which FeatureEnabled is set to
     * false so feature enabled will return false
     */
    @Test
    public void isFeatureEnabledWithExperimentKeyForcedOffFeatureEnabledFalse() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));
        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        Variation forcedVariation = activatedExperiment.getVariations().get(2);

        Optimizely optimizely = optimizelyBuilder.build();

        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, forcedVariation.getKey());
        assertFalse(optimizely.isFeatureEnabled(FEATURE_FLAG_MULTI_VARIATE_FEATURE.getKey(), testUserId));

        eventHandler.expectImpression(activatedExperiment.getId(), forcedVariation.getId(), testUserId);
    }

    /**
     * Verify that the {@link Optimizely#activate(String, String, Map<String, String>)} call
     * uses forced variation to force the user into the second variation in which FeatureEnabled is not set
     * feature enabled will return false by default
     */
    @Test
    public void isFeatureEnabledWithExperimentKeyForcedWithNoFeatureEnabledSet() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));
        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_KEY);
        Variation forcedVariation = activatedExperiment.getVariations().get(1);

        Optimizely optimizely = optimizelyBuilder.build();

        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, forcedVariation.getKey());
        assertFalse(optimizely.isFeatureEnabled(FEATURE_SINGLE_VARIABLE_DOUBLE_KEY, testUserId));

        eventHandler.expectImpression(activatedExperiment.getId(), forcedVariation.getId(), testUserId);
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} sending FeatureEnabled true and they both
     * return True when the user is bucketed into a variation for the feature.
     * An impression event should not be dispatched since the user was not bucketed into an Experiment.
     */
    @Test
    public void isFeatureEnabledTrueWhenFeatureEnabledOfVariationIsTrue() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        Optimizely optimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();

        // Should be an experiment from the rollout associated with the feature, but for this test
        // it doesn't matter. Just use any valid experiment.
        Experiment experiment = validProjectConfig.getRolloutIdMapping().get(ROLLOUT_2_ID).getExperiments().get(0);
        Variation variation = new Variation("variationId", "variationKey", true, null);
        FeatureDecision featureDecision = new FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.ROLLOUT);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(validProjectConfig)
        );

        assertTrue(optimizely.isFeatureEnabled(validFeatureKey, genericUserId));

    }


    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} sending FeatureEnabled false because of which and they both
     * return false even when the user is bucketed into a variation for the feature.
     * An impression event should not be dispatched since the user was not bucketed into an Experiment.
     */
    @Test
    public void isFeatureEnabledFalseWhenFeatureEnabledOfVariationIsFalse() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        Optimizely spyOptimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();

        // Should be an experiment from the rollout associated with the feature, but for this test
        // it doesn't matter. Just use any valid experiment.
        Experiment experiment = validProjectConfig.getRolloutIdMapping().get(ROLLOUT_2_ID).getExperiments().get(0);
        Variation variation = new Variation("variationId", "variationKey", false, null);
        FeatureDecision featureDecision = new FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.ROLLOUT);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(validProjectConfig)
        );

        assertFalse(spyOptimizely.isFeatureEnabled(FEATURE_MULTI_VARIATE_FEATURE_KEY, genericUserId));

    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return False
     * when the user is bucketed an feature test variation that is turned off.
     */
    @Test
    public void isFeatureEnabledReturnsFalseAndDispatchesWhenUserIsBucketedIntoAnExperimentVariationToggleOff() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely spyOptimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();

        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        Variation variation = new Variation("2", "variation_toggled_off", false, null);

        FeatureDecision featureDecision = new FeatureDecision(activatedExperiment, variation, FeatureDecision.DecisionSource.FEATURE_TEST);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            any(FeatureFlag.class),
            anyString(),
            anyMapOf(String.class, String.class),
            any(ProjectConfig.class)
        );

        assertFalse(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId));
        eventHandler.expectImpression(activatedExperiment.getId(), variation.getId(), genericUserId);

        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is enabled for user \"" + genericUserId + "\"? false"
        );
    }

    /**
     * Integration Test
     * Verify {@link Optimizely#isFeatureEnabled(String, String, Map)}
     * returns True
     * when the user is bucketed into a variation for the feature.
     * The user is also bucketed into an experiment, so we verify that an event is dispatched.
     */
    @Test
    public void isFeatureEnabledReturnsTrueAndDispatchesEventWhenUserIsBucketedIntoAnExperiment() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);

        Optimizely optimizely = optimizelyBuilder.build();

        Map<String, Object> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        assertTrue(optimizely.isFeatureEnabled(validFeatureKey, genericUserId, attributes));

        eventHandler.expectImpression(activatedExperiment.getId(), "2099211198", genericUserId, attributes);

        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is enabled for user \"" + genericUserId + "\"? true"
        );
    }

    /**
     * Verify that we don't attempt to activate the user when the Optimizely instance is not valid
     */
    @Test
    public void isFeatureEnabledWithInvalidDatafile() throws Exception {
        Optimizely optimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();
        Boolean isEnabled = optimizely.isFeatureEnabled("no_variable_feature", genericUserId);
        assertFalse(isEnabled);

        // make sure we didn't even attempt to bucket the user
        verify(mockDecisionService, never()).getVariationForFeature(
            any(FeatureFlag.class),
            anyString(),
            anyMap(),
            any(ProjectConfig.class)
        );
    }

    /**
     * Verify {@link Optimizely#getEnabledFeatures(String, Map)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} for each featureFlag
     * return List of FeatureFlags that are enabled
     */
    @Test
    public void getEnabledFeatureWithValidUserId() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        Optimizely optimizely = optimizelyBuilder.build();
        List<String> featureFlags = optimizely.getEnabledFeatures(genericUserId, Collections.emptyMap());
        assertFalse(featureFlags.isEmpty());

        eventHandler.expectImpression("1786133852", "1619235542", genericUserId);
    }

    /**
     * Verify {@link Optimizely#getEnabledFeatures(String, Map)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} for each featureFlag sending
     * userId as empty string
     * return empty List of FeatureFlags without checking further.
     */
    @Test
    public void getEnabledFeatureWithEmptyUserId() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        Optimizely optimizely = optimizelyBuilder.build();
        List<String> featureFlags = optimizely.getEnabledFeatures("", Collections.emptyMap());
        assertFalse(featureFlags.isEmpty());

        eventHandler.expectImpression("4138322202", "1394671166", "");
    }

    /**
     * Verify {@link Optimizely#getEnabledFeatures(String, Map)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} for each featureFlag sending
     * userId as null
     * Exception of IllegalArgumentException will be thrown
     * return
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getEnabledFeatureWithNullUserID() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));
        String userID = null;
        Optimizely optimizely = optimizelyBuilder.build();
        List<String> featureFlags = optimizely.getEnabledFeatures(userID, Collections.emptyMap());
        assertTrue(featureFlags.isEmpty());

        logbackVerifier.expectMessage(
            Level.ERROR,
            "The user ID parameter must be nonnull."
        );
    }

    /**
     * Verify {@link Optimizely#getEnabledFeatures(String, Map)} calls into
     * {@link DecisionService#getVariationForFeature} to return feature
     * disabled so {@link Optimizely#getEnabledFeatures(String, Map)} will
     * return empty List of FeatureFlags.
     */
    @Test
    public void getEnabledFeatureWithMockDecisionService() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        Optimizely optimizely = optimizelyBuilder.withDecisionService(mockDecisionService).build();

        FeatureDecision featureDecision = new FeatureDecision(null, null, FeatureDecision.DecisionSource.ROLLOUT);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            any(FeatureFlag.class),
            anyString(),
            anyMapOf(String.class, String.class),
            any(ProjectConfig.class)
        );

        List<String> featureFlags = optimizely.getEnabledFeatures(genericUserId,
            Collections.<String, String>emptyMap());
        assertTrue(featureFlags.isEmpty());
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableString(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * and returns null
     * when called with a null value for the feature Key parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableStringReturnsNullWhenFeatureKeyIsNull() throws Exception {
        String variableKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        assertNull(spyOptimizely.getFeatureVariableString(
            null,
            variableKey,
            genericUserId
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The featureKey parameter must be nonnull."
        );
        verify(spyOptimizely, times(1)).getFeatureVariableString(
            isNull(String.class),
            any(String.class),
            any(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * calls through to {@link Optimizely#getFeatureVariableString(String, String, String)}
     * and returns null
     * when called with a null value for the variableKey parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableStringReturnsNullWhenVariableKeyIsNull() throws Exception {
        String featureKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        assertNull(spyOptimizely.getFeatureVariableString(featureKey, null, genericUserId));

        logbackVerifier.expectMessage(Level.WARN, "The variableKey parameter must be nonnull.");
        verify(spyOptimizely, times(1)).getFeatureVariableString(
            any(String.class),
            isNull(String.class),
            any(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * calls through to {@link Optimizely#getFeatureVariableString(String, String, String)}
     * and returns null
     * when called with a null value for the userID parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableStringReturnsNullWhenUserIdIsNull() throws Exception {
        String featureKey = "";
        String variableKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        assertNull(spyOptimizely.getFeatureVariableString(featureKey, variableKey, null));

        logbackVerifier.expectMessage(Level.WARN, "The userId parameter must be nonnull.");
        verify(spyOptimizely, times(1)).getFeatureVariableString(
            any(String.class),
            any(String.class),
            isNull(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableString(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableString(String, String, String, Map<String, String>)}
     * and returns null
     * when {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns null
     */
    @Test
    public void getFeatureVariableStringReturnsNullFromInternal() throws Exception {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        doReturn(null).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.STRING)
        );

        assertNull(spyOptimizely.getFeatureVariableString(featureKey, variableKey, genericUserId));

        verify(spyOptimizely).getFeatureVariableString(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableString(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * and both return the value returned from
     * {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}.
     */
    @Test
    public void getFeatureVariableStringReturnsWhatInternalReturns() throws Exception {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        String valueNoAttributes = "valueNoAttributes";
        String valueWithAttributes = "valueWithAttributes";
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        doReturn(valueNoAttributes).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.STRING)
        );

        doReturn(valueWithAttributes).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(attributes),
            eq(FeatureVariable.VariableType.STRING)
        );

        assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableString(
            featureKey,
            variableKey,
            genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableString(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );

        assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableString(
            featureKey,
            variableKey,
            genericUserId,
            attributes
        ));
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableBoolean(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableBoolean(String, String, String, Map)}
     * and returns null
     * when called with a null value for the feature Key parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableBooleanReturnsNullWhenFeatureKeyIsNull() throws Exception {
        String variableKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        assertNull(spyOptimizely.getFeatureVariableBoolean(
            null,
            variableKey,
            genericUserId
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The featureKey parameter must be nonnull."
        );

        verify(spyOptimizely, times(1)).getFeatureVariableBoolean(
            isNull(String.class),
            any(String.class),
            any(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableBoolean(String, String, String, Map)}
     * calls through to {@link Optimizely#getFeatureVariableBoolean(String, String, String)}
     * and returns null
     * when called with a null value for the variableKey parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableBooleanReturnsNullWhenVariableKeyIsNull() throws Exception {
        String featureKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        assertNull(spyOptimizely.getFeatureVariableBoolean(
            featureKey,
            null,
            genericUserId
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The variableKey parameter must be nonnull."
        );
        verify(spyOptimizely, times(1)).getFeatureVariableBoolean(
            any(String.class),
            isNull(String.class),
            any(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableBoolean(String, String, String, Map)}
     * calls through to {@link Optimizely#getFeatureVariableBoolean(String, String, String)}
     * and returns null
     * when called with a null value for the userID parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableBooleanReturnsNullWhenUserIdIsNull() throws Exception {
        String featureKey = "";
        String variableKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        assertNull(spyOptimizely.getFeatureVariableBoolean(
            featureKey,
            variableKey,
            null
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The userId parameter must be nonnull."
        );
        verify(spyOptimizely, times(1)).getFeatureVariableBoolean(
            any(String.class),
            any(String.class),
            isNull(String.class),
            anyMapOf(String.class, String.class)
        );
    }


    /**
     * Verify {@link Optimizely#getFeatureVariableBoolean(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableBoolean(String, String, String, Map<String, String>)}
     * and returns null
     * when {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns null
     */
    @Test
    public void getFeatureVariableBooleanReturnsNullFromInternal() throws Exception {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        doReturn(null).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.BOOLEAN)
        );

        assertNull(spyOptimizely.getFeatureVariableBoolean(
            featureKey,
            variableKey,
            genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableBoolean(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableBoolean(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableBoolean(String, String, String, Map)}
     * and both return a Boolean representation of the value returned from
     * {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}.
     */
    @Test
    public void getFeatureVariableBooleanReturnsWhatInternalReturns() throws Exception {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        Boolean valueNoAttributes = false;
        Boolean valueWithAttributes = true;
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        doReturn(valueNoAttributes).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.BOOLEAN)
        );

        doReturn(valueWithAttributes).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(attributes),
            eq(FeatureVariable.VariableType.BOOLEAN)
        );

        assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableBoolean(
            featureKey,
            variableKey,
            genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableBoolean(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );

        assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableBoolean(
            featureKey,
            variableKey,
            genericUserId,
            attributes
        ));
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableDouble(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableDouble(String, String, String, Map<String, String>)}
     * and returns null
     * when {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns null
     */
    @Test
    public void getFeatureVariableDoubleReturnsNullFromInternal() throws Exception {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        doReturn(null).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.DOUBLE)
        );

        assertNull(spyOptimizely.getFeatureVariableDouble(
            featureKey,
            variableKey,
            genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableDouble(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableDouble(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableDouble(String, String, String, Map)}
     * and both return the parsed Double from the value returned from
     * {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}.
     */
    @Test
    public void getFeatureVariableDoubleReturnsWhatInternalReturns() throws Exception {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        Double valueNoAttributes = 0.1;
        Double valueWithAttributes = 0.2;
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        doReturn(valueNoAttributes).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.DOUBLE)
        );

        doReturn(valueWithAttributes).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(attributes),
            eq(FeatureVariable.VariableType.DOUBLE)
        );

        assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableDouble(
            featureKey,
            variableKey,
            genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableDouble(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );

        assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableDouble(
            featureKey,
            variableKey,
            genericUserId,
            attributes
        ));
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableInteger(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableInteger(String, String, String, Map<String, String>)}
     * and returns null
     * when {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns null
     */
    @Test
    public void getFeatureVariableIntegerReturnsNullFromInternal() throws Exception {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        doReturn(null).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.INTEGER)
        );

        assertNull(spyOptimizely.getFeatureVariableInteger(
            featureKey,
            variableKey,
            genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableInteger(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableDouble(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableDouble(String, String, String, Map)}
     * and returns null
     * when called with a null value for the feature Key parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableDoubleReturnsNullWhenFeatureKeyIsNull() throws Exception {
        String variableKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        assertNull(spyOptimizely.getFeatureVariableDouble(null, variableKey, genericUserId));

        logbackVerifier.expectMessage(Level.WARN, "The featureKey parameter must be nonnull.");
        verify(spyOptimizely, times(1)).getFeatureVariableDouble(
            isNull(String.class),
            any(String.class),
            any(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableDouble(String, String, String, Map)}
     * calls through to {@link Optimizely#getFeatureVariableDouble(String, String, String)}
     * and returns null
     * when called with a null value for the variableKey parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableDoubleReturnsNullWhenVariableKeyIsNull() throws Exception {
        String featureKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        assertNull(spyOptimizely.getFeatureVariableDouble(featureKey, null, genericUserId));
        logbackVerifier.expectMessage(Level.WARN, "The variableKey parameter must be nonnull.");

        verify(spyOptimizely, times(1)).getFeatureVariableDouble(
            any(String.class),
            isNull(String.class),
            any(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableDouble(String, String, String, Map)}
     * calls through to {@link Optimizely#getFeatureVariableDouble(String, String, String)}
     * and returns null
     * when called with a null value for the userID parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableDoubleReturnsNullWhenUserIdIsNull() throws Exception {
        String featureKey = "";
        String variableKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        assertNull(spyOptimizely.getFeatureVariableDouble(featureKey, variableKey, null));
        logbackVerifier.expectMessage(Level.WARN, "The userId parameter must be nonnull.");

        verify(spyOptimizely, times(1)).getFeatureVariableDouble(
            any(String.class),
            any(String.class),
            isNull(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify that {@link Optimizely#getFeatureVariableDouble(String, String, String)}
     * and {@link Optimizely#getFeatureVariableDouble(String, String, String, Map)}
     * do not throw errors when they are unable to parse the value into an Double.
     */
    @Test
    public void getFeatureVariableDoubleCatchesExceptionFromParsing() throws Exception {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        String unParsableValue = "not_a_double";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        doReturn(unParsableValue).when(spyOptimizely).getFeatureVariableValueForType(
            anyString(),
            anyString(),
            anyString(),
            anyMapOf(String.class, String.class),
            eq(FeatureVariable.VariableType.DOUBLE)
        );

        assertNull(spyOptimizely.getFeatureVariableDouble(featureKey, variableKey, genericUserId));
    }

    /**
     * Verify that {@link Optimizely#convertStringToType(String, FeatureVariable.VariableType)}
     * do not throw errors when they are unable to parse the value into an Double.
     *
     * @throws NumberFormatException
     */
    @Test
    public void convertStringToTypeDoubleCatchesExceptionFromParsing() throws NumberFormatException {
        String unParsableValue = "not_a_double";

        Optimizely optimizely = optimizelyBuilder.build();
        assertNull(optimizely.convertStringToType(unParsableValue, FeatureVariable.VariableType.DOUBLE));

        logbackVerifier.expectMessage(
            Level.ERROR,
            "NumberFormatException while trying to parse \"" + unParsableValue +
                "\" as Double."
        );
    }

    /**
     * Verify that {@link Optimizely#convertStringToType(String, FeatureVariable.VariableType)}
     * do not throw errors when they are unable to parse the value into an Integer.
     *
     * @throws NumberFormatException
     */
    @Test
    public void convertStringToTypeIntegerCatchesExceptionFromParsing() throws NumberFormatException {
        String unParsableValue = "not_a_integer";

        Optimizely optimizely = optimizelyBuilder.build();
        assertNull(optimizely.convertStringToType(unParsableValue, FeatureVariable.VariableType.INTEGER));

        logbackVerifier.expectMessage(
            Level.ERROR,
            "NumberFormatException while trying to parse \"" + unParsableValue +
                "\" as Integer."
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableInteger(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableInteger(String, String, String, Map)}
     * and returns null
     * when called with a null value for the feature Key parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableIntegerReturnsNullWhenFeatureKeyIsNull() throws Exception {
        String variableKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());
        assertNull(spyOptimizely.getFeatureVariableInteger(null, variableKey, genericUserId));
        logbackVerifier.expectMessage(Level.WARN, "The featureKey parameter must be nonnull.");

        verify(spyOptimizely, times(1)).getFeatureVariableInteger(
            isNull(String.class),
            any(String.class),
            any(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableInteger(String, String, String, Map)}
     * calls through to {@link Optimizely#getFeatureVariableInteger(String, String, String)}
     * and returns null
     * when called with a null value for the variableKey parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableIntegerReturnsNullWhenVariableKeyIsNull() throws Exception {
        String featureKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());
        assertNull(spyOptimizely.getFeatureVariableInteger(featureKey, null, genericUserId));
        logbackVerifier.expectMessage(Level.WARN, "The variableKey parameter must be nonnull.");

        verify(spyOptimizely, times(1)).getFeatureVariableInteger(
            any(String.class),
            isNull(String.class),
            any(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableInteger(String, String, String, Map)}
     * calls through to {@link Optimizely#getFeatureVariableInteger(String, String, String)}
     * and returns null
     * when called with a null value for the userID parameter.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableIntegerReturnsNullWhenUserIdIsNull() throws Exception {
        String featureKey = "";
        String variableKey = "";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());
        assertNull(spyOptimizely.getFeatureVariableInteger(featureKey, variableKey, null));
        logbackVerifier.expectMessage(Level.WARN, "The userId parameter must be nonnull.");

        verify(spyOptimizely, times(1)).getFeatureVariableInteger(
            any(String.class),
            any(String.class),
            isNull(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableInteger(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableInteger(String, String, String, Map)}
     * and both return the parsed Integer value from the value returned from
     * {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}.
     */
    @Test
    public void getFeatureVariableIntegerReturnsWhatInternalReturns() throws Exception {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        Integer valueNoAttributes = 1;
        Integer valueWithAttributes = 2;
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        doReturn(valueNoAttributes).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.INTEGER)
        );

        doReturn(valueWithAttributes).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(attributes),
            eq(FeatureVariable.VariableType.INTEGER)
        );

        assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableInteger(
            featureKey,
            variableKey,
            genericUserId
        ));

        verify(spyOptimizely).getFeatureVariableInteger(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );

        assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableInteger(
            featureKey,
            variableKey,
            genericUserId,
            attributes
        ));
    }

    /**
     * Verify that {@link Optimizely#getFeatureVariableInteger(String, String, String)}
     * and {@link Optimizely#getFeatureVariableInteger(String, String, String, Map)}
     * do not throw errors when they are unable to parse the value into an Integer.
     */
    @Test
    public void getFeatureVariableIntegerCatchesExceptionFromParsing() throws Exception {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        String unParsableValue = "not_an_integer";

        Optimizely spyOptimizely = spy(optimizelyBuilder.build());

        doReturn(unParsableValue).when(spyOptimizely).getFeatureVariableValueForType(
            anyString(),
            anyString(),
            anyString(),
            anyMapOf(String.class, String.class),
            eq(FeatureVariable.VariableType.INTEGER)
        );

        assertNull(spyOptimizely.getFeatureVariableInteger(featureKey, variableKey, genericUserId));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} returns a variation when given an experiment
     * with no audiences and no user attributes and verify that listener is getting called.
     */
    @Test
    public void getVariationBucketingIdAttribute() throws Exception {
        Experiment experiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = experiment.getVariations().get(1);
        Map<String, String> testUserAttributes = Collections.singletonMap("browser_type", "chrome");

        Optimizely optimizely = optimizelyBuilder.build();

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(EXPERIMENT_KEY, experiment.getKey());
        testDecisionInfoMap.put(VARIATION_KEY, bucketedVariation.getKey());

        int notificationId = optimizely.addDecisionNotificationHandler(
            getDecisionListener(NotificationCenter.DecisionNotificationType.AB_TEST.toString(),
                testUserId,
                testUserAttributes,
                testDecisionInfoMap));

        Variation actualVariation = optimizely.getVariation(experiment.getKey(), testUserId, testUserAttributes);
        assertThat(actualVariation, is(bucketedVariation));
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    //======== isValid calls  ========//

    /**
     * Verify that {@link Optimizely#isValid()} returns false when the Optimizely instance is not valid
     */
    @Test
    public void isValidReturnsFalseWhenClientIsInvalid() throws Exception {
        Optimizely optimizely = Optimizely.builder(invalidProjectConfigV5(), mockEventHandler).build();

        assertFalse(optimizely.isValid());
    }

    /**
     * Verify that {@link Optimizely#isValid()} returns false when the Optimizely instance is not valid
     */
    @Test
    public void isValidReturnsTrueWhenClientIsValid() throws Exception {
        Optimizely optimizely = optimizelyBuilder.build();
        assertTrue(optimizely.isValid());
    }

    //======== Test Notification APIs ========//

    @Test
    public void testGetNotificationCenter() {
        Optimizely optimizely = optimizelyBuilder.withConfigManager(() -> null).build();
        assertEquals(optimizely.notificationCenter, optimizely.getNotificationCenter());
    }

    @Test
    public void testAddTrackNotificationHandler() {
        Optimizely optimizely = optimizelyBuilder.withConfigManager(() -> null).build();
        NotificationManager<TrackNotification> manager = optimizely.getNotificationCenter()
            .getNotificationManager(TrackNotification.class);

        int notificationId = optimizely.addTrackNotificationHandler(message -> {});
        assertTrue(manager.remove(notificationId));
    }

    @Test
    public void testAddDecisionNotificationHandler() {
        Optimizely optimizely = optimizelyBuilder.withConfigManager(() -> null).build();
        NotificationManager<DecisionNotification> manager = optimizely.getNotificationCenter()
            .getNotificationManager(DecisionNotification.class);

        int notificationId = optimizely.addDecisionNotificationHandler(message -> {});
        assertTrue(manager.remove(notificationId));
    }

    @Test
    public void testAddUpdateConfigNotificationHandler() {
        Optimizely optimizely = optimizelyBuilder.withConfigManager(() -> null).build();
        NotificationManager<UpdateConfigNotification> manager = optimizely.getNotificationCenter()
            .getNotificationManager(UpdateConfigNotification.class);

        int notificationId = optimizely.addUpdateConfigNotificationHandler(message -> {});
        assertTrue(manager.remove(notificationId));
    }

    @Test
    public void testAddLogEventNotificationHandler() {
        Optimizely optimizely = optimizelyBuilder.withConfigManager(() -> null).build();
        NotificationManager<LogEvent> manager = optimizely.getNotificationCenter()
            .getNotificationManager(LogEvent.class);

        int notificationId = optimizely.addLogEventNotificationHandler(message -> {});
        assertTrue(manager.remove(notificationId));
    }

    //======== Helper methods ========//

    private Experiment createUnknownExperiment() {
        return new Experiment("0987", "unknown_experiment", "Running", "1",
            Collections.<String>emptyList(),
            null,
            Collections.singletonList(
                new Variation("8765", "unknown_variation", Collections.<FeatureVariableUsageInstance>emptyList())),
            Collections.<String, String>emptyMap(),
            Collections.singletonList(new TrafficAllocation("8765", 4999)));
    }

    private EventType createUnknownEventType() {
        List<String> experimentIds = asList(
            "223"
        );
        return new EventType("8765", "unknown_event_type", experimentIds);
    }

    /* Invalid Experiment */
    @Test
    @SuppressFBWarnings("NP")
    public void setForcedVariationNullExperimentKey() {
        Optimizely optimizely = optimizelyBuilder.build();
        assertFalse(optimizely.setForcedVariation(null, "testUser1", "vtag1"));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void getForcedVariationNullExperimentKey() {
        Optimizely optimizely = optimizelyBuilder.build();
        assertNull(optimizely.getForcedVariation(null, "testUser1"));
    }


    @Test
    public void setForcedVariationWrongExperimentKey() {
        Optimizely optimizely = optimizelyBuilder.build();
        assertFalse(optimizely.setForcedVariation("wrongKey", "testUser1", "vtag1"));

    }

    @Test
    public void getForcedVariationWrongExperimentKey() {
        Optimizely optimizely = optimizelyBuilder.build();
        assertNull(optimizely.getForcedVariation("wrongKey", "testUser1"));
    }

    @Test
    public void setForcedVariationEmptyExperimentKey() {
        Optimizely optimizely = optimizelyBuilder.build();
        assertFalse(optimizely.setForcedVariation("", "testUser1", "vtag1"));

    }

    @Test
    public void getForcedVariationEmptyExperimentKey() {
        Optimizely optimizely = optimizelyBuilder.build();
        assertNull(optimizely.getForcedVariation("", "testUser1"));
    }
}
