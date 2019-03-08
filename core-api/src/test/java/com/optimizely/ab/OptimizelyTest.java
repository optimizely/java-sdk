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
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.FeatureVariable;
import com.optimizely.ab.config.FeatureVariableUsageInstance;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.error.RaiseExceptionErrorHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.EventFactory;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.internal.LogbackVerifier;
import com.optimizely.ab.internal.ControlAttribute;
import com.optimizely.ab.notification.ActivateNotificationListener;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.TrackNotificationListener;
import com.optimizely.ab.notification.DecisionNotificationListener;
import com.optimizely.ab.notification.DecisionInfoEnums;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.config.ProjectConfigTestUtils.*;
import static com.optimizely.ab.config.ValidProjectConfigV4.*;
import static com.optimizely.ab.event.LogEvent.RequestMethod;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
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

    @Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
            {
                2,
                validConfigJsonV2(),
                noAudienceProjectConfigJsonV2(),
                validProjectConfigV2(),
                noAudienceProjectConfigV2()
            },
            {
                3,
                validConfigJsonV3(),
                noAudienceProjectConfigJsonV3(),
                validProjectConfigV3(),
                noAudienceProjectConfigV3()
            },
            {
                4,
                validConfigJsonV4(),
                validConfigJsonV4(),
                validProjectConfigV4(),
                validProjectConfigV4()
            }
        });
    }

    @Rule
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Mock
    EventHandler mockEventHandler;
    @Mock
    Bucketer mockBucketer;
    @Mock
    DecisionService mockDecisionService;
    @Mock
    ErrorHandler mockErrorHandler;

    private static final String genericUserId = "genericUserId";
    private static final String testUserId = "userId";
    private static final String testBucketingId = "bucketingId";
    private static final String testBucketingIdKey = ControlAttribute.BUCKETING_ATTRIBUTE.toString();
    private static final Map<String, String> testParams = Collections.singletonMap("test", "params");
    private static final LogEvent logEventToDispatch = new LogEvent(RequestMethod.GET, "test_url", testParams, new EventBatch());

    private int datafileVersion;
    private String validDatafile;
    private String noAudienceDatafile;
    private ProjectConfig validProjectConfig;
    private ProjectConfig noAudienceProjectConfig;

    public OptimizelyTest(int datafileVersion,
                          String validDatafile,
                          String noAudienceDatafile,
                          ProjectConfig validProjectConfig,
                          ProjectConfig noAudienceProjectConfig) {
        this.datafileVersion = datafileVersion;
        this.validDatafile = validDatafile;
        this.noAudienceDatafile = noAudienceDatafile;
        this.validProjectConfig = validProjectConfig;
        this.noAudienceProjectConfig = noAudienceProjectConfig;
    }

    //======== activate tests ========//

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEnd() throws Exception {
        Experiment activatedExperiment;
        Map<String, String> testUserAttributes = new HashMap<String, String>();
        String bucketingKey = testBucketingIdKey;
        String userId = testUserId;
        String bucketingId = testBucketingId;
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            testUserAttributes.put("browser_type", "chrome");
        }
        testUserAttributes.put(bucketingKey, bucketingId);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();


        when(mockEventFactory.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, testUserId,
            testUserAttributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, bucketingId))
            .thenReturn(bucketedVariation);

        logbackVerifier.expectMessage(Level.DEBUG, String.format("No variation for experiment \"%s\" mapped to user \"%s\" in the forced variation map", activatedExperiment.getKey(), testUserId));

        logbackVerifier.expectMessage(Level.DEBUG, "BucketingId is valid: \"bucketingId\"");

        logbackVerifier.expectMessage(Level.INFO, "This decision will not be saved since the UserProfileService is null.");

        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching impression event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), userId, testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, bucketingId);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEndWithTypedAudienceInt() throws Exception {
        Experiment activatedExperiment;
        Map<String, Object> testUserAttributes = new HashMap<String, Object>();
        String bucketingKey = testBucketingIdKey;
        String userId = testUserId;
        String bucketingId = testBucketingId;
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY);
            testUserAttributes.put(ATTRIBUTE_INTEGER_KEY, 2); // should be gt 1.
        } else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            testUserAttributes.put("browser_type", "chrome");
        }
        testUserAttributes.put(bucketingKey, bucketingId);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();


        when(mockEventFactory.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, testUserId,
            testUserAttributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, bucketingId))
            .thenReturn(bucketedVariation);

        logbackVerifier.expectMessage(Level.DEBUG, String.format("No variation for experiment \"%s\" mapped to user \"%s\" in the forced variation map", activatedExperiment.getKey(), testUserId));

        logbackVerifier.expectMessage(Level.DEBUG, "BucketingId is valid: \"bucketingId\"");

        logbackVerifier.expectMessage(Level.INFO, "This decision will not be saved since the UserProfileService is null.");

        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching impression event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), userId, testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, bucketingId);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that activating using typed audiences works for numeric match exact using double and integer.
     */
    @Test
    public void activateEndToEndWithTypedAudienceIntExactDouble() throws Exception {
        Experiment activatedExperiment;
        Map<String, Object> testUserAttributes = new HashMap<String, Object>();
        String bucketingKey = testBucketingIdKey;
        String userId = testUserId;
        String bucketingId = testBucketingId;
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY);
            testUserAttributes.put(ATTRIBUTE_INTEGER_KEY, 1.0); // should be equal 1.
        } else {
            return; // only test on v4 datafiles.
        }
        testUserAttributes.put(bucketingKey, bucketingId);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();


        when(mockEventFactory.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, testUserId,
            testUserAttributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, bucketingId))
            .thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), userId, testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, bucketingId);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that activating using typed audiences works for numeric match exact using double and integer.
     */
    @Test
    public void activateEndToEndWithTypedAudienceIntExact() throws Exception {
        Experiment activatedExperiment;
        Map<String, Object> testUserAttributes = new HashMap<String, Object>();
        String bucketingKey = testBucketingIdKey;
        String userId = testUserId;
        String bucketingId = testBucketingId;
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY);
            testUserAttributes.put(ATTRIBUTE_INTEGER_KEY, 1); // should be equal 1.
        } else {
            return; // only test on v4 datafiles.
        }
        testUserAttributes.put(bucketingKey, bucketingId);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();


        when(mockEventFactory.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, testUserId,
            testUserAttributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, bucketingId))
            .thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), userId, testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, bucketingId);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEndWithTypedAudienceBool() throws Exception {
        Experiment activatedExperiment;
        Map<String, Object> testUserAttributes = new HashMap<String, Object>();
        String bucketingKey = testBucketingIdKey;
        String userId = testUserId;
        String bucketingId = testBucketingId;
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY);
            testUserAttributes.put(ATTRIBUTE_BOOLEAN_KEY, true); // should be eq true.
        } else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            testUserAttributes.put("browser_type", "chrome");
        }
        testUserAttributes.put(bucketingKey, bucketingId);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();


        when(mockEventFactory.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, testUserId,
            testUserAttributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, bucketingId))
            .thenReturn(bucketedVariation);

        logbackVerifier.expectMessage(Level.DEBUG, String.format("No variation for experiment \"%s\" mapped to user \"%s\" in the forced variation map", activatedExperiment.getKey(), testUserId));

        logbackVerifier.expectMessage(Level.DEBUG, "BucketingId is valid: \"bucketingId\"");

        logbackVerifier.expectMessage(Level.INFO, "This decision will not be saved since the UserProfileService is null.");

        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching impression event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), userId, testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, bucketingId);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEndWithTypedAudienceDouble() throws Exception {
        Experiment activatedExperiment;
        Map<String, Object> testUserAttributes = new HashMap<String, Object>();
        String bucketingKey = testBucketingIdKey;
        String userId = testUserId;
        String bucketingId = testBucketingId;
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY);
            testUserAttributes.put(ATTRIBUTE_DOUBLE_KEY, 99.9); // should be lt 100.
        } else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            testUserAttributes.put("browser_type", "chrome");
        }
        testUserAttributes.put(bucketingKey, bucketingId);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();


        when(mockEventFactory.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, testUserId,
            testUserAttributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, bucketingId))
            .thenReturn(bucketedVariation);

        logbackVerifier.expectMessage(Level.DEBUG, String.format("No variation for experiment \"%s\" mapped to user \"%s\" in the forced variation map", activatedExperiment.getKey(), testUserId));

        logbackVerifier.expectMessage(Level.DEBUG, "BucketingId is valid: \"bucketingId\"");

        logbackVerifier.expectMessage(Level.INFO, "This decision will not be saved since the UserProfileService is null.");

        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching impression event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), userId, testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, bucketingId);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEndWithTypedAudienceBoolWithAndAudienceConditions() throws Exception {
        Experiment activatedExperiment;
        Map<String, Object> testUserAttributes = new HashMap<String, Object>();
        String bucketingKey = testBucketingIdKey;
        String userId = testUserId;
        String bucketingId = testBucketingId;
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_KEY);
            testUserAttributes.put(ATTRIBUTE_BOOLEAN_KEY, true); // should be eq true.
        } else {
            return;
        }
        testUserAttributes.put(bucketingKey, bucketingId);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();


        when(mockEventFactory.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, testUserId,
            testUserAttributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, bucketingId))
            .thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), userId, testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verifyZeroInteractions(mockBucketer);
        assertNull(actualVariation);

        // verify that dispatchEvent was called with the correct LogEvent object
        //verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} call correctly builds an endpoint url and
     * request params and passes them through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void activateEndToEndWithTypedAudienceWithAnd() throws Exception {
        Experiment activatedExperiment;
        Map<String, Object> testUserAttributes = new HashMap<String, Object>();
        String bucketingKey = testBucketingIdKey;
        String userId = testUserId;
        String bucketingId = testBucketingId;
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_KEY);
            testUserAttributes.put(ATTRIBUTE_DOUBLE_KEY, 99.9); // should be lt 100.
            testUserAttributes.put(ATTRIBUTE_BOOLEAN_KEY, true); // should be eq true.
            testUserAttributes.put(ATTRIBUTE_INTEGER_KEY, 2); // should be gt 1.
        } else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            testUserAttributes.put("browser_type", "chrome");
        }
        testUserAttributes.put(bucketingKey, bucketingId);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();


        when(mockEventFactory.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, testUserId,
            testUserAttributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, bucketingId))
            .thenReturn(bucketedVariation);

        logbackVerifier.expectMessage(Level.DEBUG, String.format("No variation for experiment \"%s\" mapped to user \"%s\" in the forced variation map", activatedExperiment.getKey(), testUserId));

        logbackVerifier.expectMessage(Level.DEBUG, "BucketingId is valid: \"bucketingId\"");

        logbackVerifier.expectMessage(Level.INFO, "This decision will not be saved since the UserProfileService is null.");

        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching impression event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), userId, testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, bucketingId);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that the {@link Optimizely#activate(Experiment, String, Map)} DOES NOT dispatch an impression event
     * when the user isn't bucketed to a variation.
     */
    @Test
    public void activateForNullVariation() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");
        testUserAttributes.put(testBucketingIdKey,
            testBucketingId);

        when(mockBucketer.bucket(activatedExperiment, testBucketingId))
            .thenReturn(null);

        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
            activatedExperiment.getKey() + "\".");

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testBucketingId);
        assertNull(actualVariation);

        // verify that dispatchEvent was NOT called
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify the case were {@link Optimizely#activate(Experiment, String)} is called with an {@link Experiment}
     * that is not present in the current {@link ProjectConfig}. We should NOT throw an error in that case.
     * <p>
     * This may happen if an experiment is retrieved from the project config, the project config is updated and the
     * referenced experiment removed, then activate is called given the now removed experiment.
     * Could also happen if an experiment was manually created and passed through.
     */
    @Test
    public void activateWhenExperimentIsNotInProject() throws Exception {
        Experiment unknownExperiment = createUnknownExperiment();
        Variation bucketedVariation = unknownExperiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(validProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        when(mockBucketer.bucket(unknownExperiment, testUserId))
            .thenReturn(bucketedVariation);

        optimizely.activate(unknownExperiment, testUserId);
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
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, forcedVariation.getKey());

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            testUserAttributes.put("browser_type", "chrome");
        }

        testUserAttributes.put(testBucketingIdKey,
            testBucketingId);

        when(mockEventFactory.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(forcedVariation),
            eq(testUserId), eq(testUserAttributes)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, testBucketingId))
            .thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);

        assertThat(actualVariation, is(forcedVariation));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);

        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, null);

        assertEquals(optimizely.getForcedVariation(activatedExperiment.getKey(), testUserId), null);

    }

    /**
     * Verify that the {@link Optimizely#getVariation(String, String, Map<String, String>)} call
     * uses forced variation to force the user into the second variation.  The mock bucket returns
     * the first variation. Then remove the forced variation and confirm that the forced variation is null.
     */
    @Test
    public void getVariationWithExperimentKeyForced() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation forcedVariation = activatedExperiment.getVariations().get(1);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, forcedVariation.getKey());

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            testUserAttributes.put("browser_type", "chrome");
        }

        testUserAttributes.put(testBucketingIdKey,
            testBucketingId);

        when(mockEventFactory.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(forcedVariation),
            eq(testUserId), eq(testUserAttributes)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, testBucketingId))
            .thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.getVariation(activatedExperiment.getKey(), testUserId, testUserAttributes);

        assertThat(actualVariation, is(forcedVariation));

        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, null);

        assertEquals(optimizely.getForcedVariation(activatedExperiment.getKey(), testUserId), null);

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
        Variation forcedVariation = activatedExperiment.getVariations().get(1);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, forcedVariation.getKey());

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        if (datafileVersion < 4) {
            testUserAttributes.put("browser_type", "chrome");
        }

        when(mockEventFactory.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(forcedVariation),
            eq(testUserId), eq(testUserAttributes)))
            .thenReturn(logEventToDispatch);

        // activate the experiment
        assertTrue(optimizely.isFeatureEnabled(FEATURE_FLAG_MULTI_VARIATE_FEATURE.getKey(), testUserId));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);

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
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Variation userIdBucketVariation = activatedExperiment.getVariations().get(1);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            testUserAttributes.put("browser_type", "chrome");
        }

        testUserAttributes.put(testBucketingIdKey, testBucketingId);

        when(mockEventFactory.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
            eq(testUserId), eq(testUserAttributes)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, testUserId))
            .thenReturn(userIdBucketVariation);

        when(mockBucketer.bucket(activatedExperiment, testBucketingId))
            .thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testBucketingId);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} handles the case where an unknown experiment
     * (i.e., not in the config) is passed through and a {@link NoOpErrorHandler} is used by default.
     */
    @Test
    public void activateWithUnknownExperimentKeyAndNoOpErrorHandler() throws Exception {
        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Experiment \"unknown_experiment\" is not in the datafile.");
        logbackVerifier.expectMessage(Level.INFO,
            "Not activating user \"userId\" for experiment \"unknown_experiment\".");

        // since we use a NoOpErrorHandler, we should fail and return null
        Variation actualVariation = optimizely.activate(unknownExperiment.getKey(), testUserId);

        // verify that null is returned, as no project config was available
        assertNull(actualVariation);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} handles the case where an unknown experiment
     * (i.e., not in the config) is passed through and a {@link RaiseExceptionErrorHandler} is provided.
     */
    @Test
    public void activateWithUnknownExperimentKeyAndRaiseExceptionErrorHandler() throws Exception {
        thrown.expect(UnknownExperimentException.class);

        Experiment unknownExperiment = createUnknownExperiment();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        // since we use a RaiseExceptionErrorHandler, we should throw an error
        optimizely.activate(unknownExperiment.getKey(), testUserId);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} passes through attributes.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void activateWithAttributes() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Variation userIdBucketedVariation = activatedExperiment.getVariations().get(1);
        Attribute attributeString = validProjectConfig.getAttributes().get(0);

        // setup a mock event builder to return expected impression params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        when(mockEventFactory.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
            eq(testUserId), anyMapOf(String.class, String.class)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, testUserId))
            .thenReturn(userIdBucketedVariation);

        when(mockBucketer.bucket(activatedExperiment, testBucketingId))
            .thenReturn(bucketedVariation);

        Map<String, String> attr = new HashMap<String, String>();

        attr.put(attributeString.getKey(), "attributeValue");
        attr.put(testBucketingIdKey, testBucketingId);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId,
            attr);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testBucketingId);
        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockEventFactory).createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment),
            eq(bucketedVariation), eq(testUserId), attributeCaptor.capture());

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, hasEntry(attributeString.getKey(), "attributeValue"));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    public void activateWithTypedAttributes() throws Exception {
        if (datafileVersion < 4) {
            return;
        }

        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Variation userIdBucketedVariation = activatedExperiment.getVariations().get(1);
        Attribute attributeString = validProjectConfig.getAttributes().get(0);
        Attribute attributeBoolean = validProjectConfig.getAttributes().get(3);
        Attribute attributeInteger = validProjectConfig.getAttributes().get(4);
        Attribute attributeDouble = validProjectConfig.getAttributes().get(5);

        // setup a mock event builder to return expected impression params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        when(mockEventFactory.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
            eq(testUserId), anyMapOf(String.class, Object.class)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, testUserId))
            .thenReturn(userIdBucketedVariation);

        when(mockBucketer.bucket(activatedExperiment, testBucketingId))
            .thenReturn(bucketedVariation);

        Map<String, Object> attr = new HashMap<>();

        attr.put(attributeString.getKey(), "attributeValue");
        attr.put(attributeBoolean.getKey(), true);
        attr.put(attributeInteger.getKey(), 3);
        attr.put(attributeDouble.getKey(), 3.123);
        attr.put(testBucketingIdKey, testBucketingId);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId,
            attr);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testBucketingId);
        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockEventFactory).createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment),
            eq(bucketedVariation), eq(testUserId), attributeCaptor.capture());

        Map<String, ?> actualValue = attributeCaptor.getValue();

        assertThat((Map<String, ? extends String>) actualValue, hasEntry(attributeString.getKey(), "attributeValue"));
        assertThat((Map<String, ? extends Boolean>) actualValue, hasEntry(attributeBoolean.getKey(), true));
        assertThat((Map<String, ? extends Integer>) actualValue, hasEntry(attributeInteger.getKey(), 3));
        assertThat((Map<String, ? extends Double>) actualValue, hasEntry(attributeDouble.getKey(), 3.123));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map<String, String>)} handles the case
     * where an unknown attribute (i.e., not in the config) is passed through.
     * <p>
     * In this case, the activate call should not remove the unknown attribute from the given map.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void activateWithUnknownAttribute() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        // setup a mock event builder to return mock params and endpoint
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            testUserAttributes.put("browser_type", "chrome");
        }
        testUserAttributes.put("unknownAttribute", "dimValue");

        when(mockEventFactory.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
            eq(testUserId), anyMapOf(String.class, String.class)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, testUserId))
            .thenReturn(bucketedVariation);

        logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
            activatedExperiment.getKey() + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching impression event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // Use an immutable map to also check that we're not attempting to change the provided attribute map
        Variation actualVariation =
            optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);

        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventFactory).createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment),
            eq(bucketedVariation), eq(testUserId), attributeCaptor.capture());

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, hasKey("unknownAttribute"));

        // verify that dispatchEvent was called with the correct LogEvent object.
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} if passed null attributes than it returns null attributes.
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void activateWithNullAttributes() throws Exception {
        Experiment activatedExperiment = noAudienceProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        // setup a mock event builder to return expected impression params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();
        Map<String, String> attributes = null;
        when(mockEventFactory.createImpressionEvent(eq(noAudienceProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
            eq(testUserId), eq(attributes)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, testUserId))
            .thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, attributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testUserId);
        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockEventFactory).createImpressionEvent(eq(noAudienceProjectConfig), eq(activatedExperiment),
            eq(bucketedVariation), eq(testUserId), attributeCaptor.capture());

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertNull(actualValue);

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String, Map)} gracefully handles null attribute values.
     */
    @Test
    public void activateWithNullAttributeValues() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = validProjectConfig.getAttributes().get(0);

        // setup a mock event builder to return expected impression params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        when(mockEventFactory.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
            eq(testUserId), anyMapOf(String.class, String.class)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, testUserId))
            .thenReturn(bucketedVariation);

        // activate the experiment
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(attribute.getKey(), null);
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, attributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testUserId);
        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockEventFactory).createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment),
            eq(bucketedVariation), eq(testUserId), attributeCaptor.capture());

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, hasEntry(attribute.getKey(), null));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
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

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"" + inactiveExperiment.getKey() +
            "\" is not running.");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
            inactiveExperiment.getKey() + "\".");

        Variation variation = optimizely.activate(inactiveExperiment.getKey(), testUserId);

        // verify that null is returned, as the experiment isn't running
        assertNull(variation);
    }

    /**
     * Verify that a user who falls in an experiment's audience is assigned a variation.
     */
    @Test
    public void activateUserInAudience() throws Exception {
        Experiment experimentToCheck = validProjectConfig.getExperiments().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        Variation actualVariation = optimizely.activate(experimentToCheck.getKey(), testUserId, testUserAttributes);
        assertNotNull(actualVariation);
    }

    /**
     * Verify that if user ID sent is null will return null variation.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void activateUserIDIsNull() throws Exception {
        Experiment experimentToCheck = validProjectConfig.getExperiments().get(0);
        String nullUserID = null;
        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        Variation nullVariation = optimizely.activate(experimentToCheck.getKey(), nullUserID, testUserAttributes);
        assertNull(nullVariation);

        logbackVerifier.expectMessage(
            Level.ERROR,
            "The user ID parameter must be nonnull."
        );
    }

    /**
     * Verify that if user ID sent is null will return null variation.
     * In activate override function where experiment object is passed
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void activateWithExperimentUserIDIsNull() throws Exception {
        Experiment experimentToCheck = validProjectConfig.getExperiments().get(0);
        String nullUserID = null;
        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        Variation nullVariation = optimizely.activate(experimentToCheck, nullUserID, testUserAttributes);
        assertNull(nullVariation);

        logbackVerifier.expectMessage(
            Level.ERROR,
            "The user ID parameter must be nonnull."
        );
    }

    /**
     * Verify that if Experiment key sent is null will return null variation.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void activateExperimentKeyIsNull() throws Exception {
        String nullExperimentKey = null;
        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        Variation nullVariation = optimizely.activate(nullExperimentKey, testUserId, testUserAttributes);
        assertNull(nullVariation);

        logbackVerifier.expectMessage(
            Level.ERROR,
            "The experimentKey parameter must be nonnull."
        );
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

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "firefox");

        logbackVerifier.expectMessage(Level.INFO,
            "User \"userId\" does not meet conditions to be in experiment \"" +
                experimentToCheck.getKey() + "\".");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
            experimentToCheck.getKey() + "\".");

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

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withErrorHandler(mockErrorHandler)
            .build();

        assertNotNull(optimizely.activate(experimentToCheck.getKey(), testUserId));
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

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .build();

        /**
         * TBD: This should be fixed.  The v4 datafile does not contain the same condition so
         * results are different.  We have made a change around 9/7/18 where we evaluate audience
         * regardless if you pass in a attribute list or not.  In this case there is a not("broswer_type = "firefox")
         * This causes the user to be bucketed now because they don't have browser_type set to firefox.
         */
        if (datafileVersion == 4) {
            assertNull(optimizely.activate(experiment.getKey(), testUserId));
        } else {
            assertNotNull(optimizely.activate(experiment.getKey(), testUserId));
        }
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} return a variation when provided an empty string.
     */
    @Test
    public void activateWithEmptyUserId() throws Exception {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        String experimentKey = experiment.getKey();

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        assertNotNull(optimizely.activate(experimentKey, ""));
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
        Variation variation = experiment.getVariations().get(0);

        Map<String, String> attributes = new HashMap<String, String>();
        if (datafileVersion == 4) {
            attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            attributes.put("browser_type", "chrome");
        }

        when(mockBucketer.bucket(experiment, "user")).thenReturn(variation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withBucketing(mockBucketer)
            .build();

        assertThat(optimizely.activate(experiment.getKey(), "user", attributes),
            is(variation));
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

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

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

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.INFO, "User \"" + whitelistedUserId + "\" is forced in variation \"" +
            expectedVariation.getKey() + "\".");
        // no attributes provided for a experiment that has an audience
        assertTrue(experiment.getUserIdToVariationKeyMap().containsKey(whitelistedUserId));
        assertThat(optimizely.activate(experiment.getKey(), whitelistedUserId), is(expectedVariation));
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

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.INFO, "Experiment \"" + experiment.getKey() + "\" is not running.");
        logbackVerifier.expectMessage(Level.INFO, "Not activating user \"" + whitelistedUserId +
            "\" for experiment \"" + experiment.getKey() + "\".");
        // testUser3 has a corresponding forced variation, but experiment status should be checked first
        assertTrue(experiment.getUserIdToVariationKeyMap().containsKey(whitelistedUserId));
        assertNull(optimizely.activate(experiment.getKey(), whitelistedUserId));
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} handles exceptions thrown by
     * {@link EventHandler#dispatchEvent(LogEvent)} gracefully.
     */
    @Test
    public void activateDispatchEventThrowsException() throws Exception {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);

        doThrow(new Exception("Test Exception")).when(mockEventHandler).dispatchEvent(any(LogEvent.class));

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withConfig(noAudienceProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Unexpected exception in event dispatcher");
        optimizely.activate(experiment.getKey(), testUserId);
    }

    /**
     * Verify that {@link Optimizely#activate(String, String)} doesn't dispatch an event for an experiment with a
     * "Launched" status.
     */
    @Test
    public void activateLaunchedExperimentDoesNotDispatchEvent() throws Exception {
        Experiment launchedExperiment;
        if (datafileVersion == 4) {
            launchedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_LAUNCHED_EXPERIMENT_KEY);
        } else {
            launchedExperiment = noAudienceProjectConfig.getExperiments().get(2);
        }

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(noAudienceProjectConfig)
            .build();

        Variation expectedVariation = launchedExperiment.getVariations().get(0);

        when(mockBucketer.bucket(launchedExperiment, testUserId))
            .thenReturn(launchedExperiment.getVariations().get(0));

        logbackVerifier.expectMessage(Level.INFO,
            "Experiment has \"Launched\" status so not dispatching event during activation.");
        Variation variation = optimizely.activate(launchedExperiment.getKey(), testUserId);

        assertNotNull(variation);
        assertThat(variation.getKey(), is(expectedVariation.getKey()));

        // verify that we did NOT dispatch an event
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that we don't attempt to activate the user when the Optimizely instance is not valid
     */
    @Test
    public void activateWithInvalidDatafile() throws Exception {
        Optimizely optimizely = Optimizely.builder(invalidProjectConfigV5(), mockEventHandler)
            .withBucketing(mockBucketer)
            .build();
        Variation expectedVariation = optimizely.activate("etag1", genericUserId);
        assertNull(expectedVariation);

        // make sure we didn't even attempt to bucket the user
        verify(mockBucketer, never()).bucket(any(Experiment.class), anyString());
    }

    //======== track tests ========//

    /**
     * Verify that the {@link Optimizely#track(String, String)} call correctly builds a V2 event and passes it
     * through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void trackEventEndToEndForced() throws Exception {
        EventType eventType;
        String datafile;
        ProjectConfig config;
        if (datafileVersion >= 4) {
            config = spy(validProjectConfig);
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
            datafile = validDatafile;
        } else {
            config = spy(noAudienceProjectConfig);
            eventType = noAudienceProjectConfig.getEventTypes().get(0);
            datafile = noAudienceDatafile;
        }

        EventFactory eventFactory = new EventFactory();
        DecisionService spyDecisionService = spy(new DecisionService(mockBucketer,
            mockErrorHandler,
            config,
            null));

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withDecisionService(spyDecisionService)
            .withEventBuilder(eventFactory)
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        // call track
        optimizely.track(eventType.getKey(), testUserId);

        // verify that dispatchEvent was called
        verify(mockEventHandler).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that the {@link Optimizely#track(String, String)} call correctly builds a V2 event and passes it
     * through {@link EventHandler#dispatchEvent(LogEvent)}.
     */
    @Test
    public void trackEventEndToEnd() throws Exception {
        EventType eventType;
        String datafile;
        ProjectConfig config;
        if (datafileVersion >= 4) {
            config = spy(validProjectConfig);
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
            datafile = validDatafile;
        } else {
            config = spy(noAudienceProjectConfig);
            eventType = noAudienceProjectConfig.getEventTypes().get(0);
            datafile = noAudienceDatafile;
        }

        EventFactory eventFactory = new EventFactory();
        DecisionService spyDecisionService = spy(new DecisionService(mockBucketer,
            mockErrorHandler,
            config,
            null));

        Optimizely optimizely = Optimizely.builder(datafile, mockEventHandler)
            .withDecisionService(spyDecisionService)
            .withEventBuilder(eventFactory)
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();


        // call track
        optimizely.track(eventType.getKey(), testUserId);

        // verify that dispatchEvent was called
        verify(mockEventHandler).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} handles the case where an unknown event type
     * (i.e., not in the config) is passed through and a {@link NoOpErrorHandler} is used by default.
     */
    @Test
    public void trackEventWithUnknownEventKeyAndNoOpErrorHandler() throws Exception {
        EventType unknownEventType = createUnknownEventType();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(new NoOpErrorHandler())
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Event \"unknown_event_type\" is not in the datafile.");
        logbackVerifier.expectMessage(Level.INFO, "Not tracking event \"unknown_event_type\" for user \"userId\".");
        optimizely.track(unknownEventType.getKey(), testUserId);

        // verify that we did NOT dispatch an event
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} handles the case where an unknown event type
     * (i.e., not in the config) is passed through and a {@link RaiseExceptionErrorHandler} is provided.
     */
    @Test
    public void trackEventWithUnknownEventKeyAndRaiseExceptionErrorHandler() throws Exception {
        thrown.expect(UnknownEventTypeException.class);

        EventType unknownEventType = createUnknownEventType();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

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
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> attributes = ImmutableMap.of(attribute.getKey(), "attributeValue");

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                anyMapOf(String.class, String.class),
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // call track
        optimizely.track(eventType.getKey(), genericUserId, attributes);

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventFactory).createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                attributeCaptor.capture(),
                eq(Collections.<String, Object>emptyMap()));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, hasEntry(attribute.getKey(), "attributeValue"));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} if passed null attributes  than it returns null attributes.
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void trackEventWithNullAttributes() throws Exception {
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

    Map<String, String> attributes = null;

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(attributes),
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // call track
        optimizely.track(eventType.getKey(), genericUserId, attributes);

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventFactory).createConversionEvent(
          eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                attributeCaptor.capture(),
                eq(Collections.<String, Object>emptyMap()));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertEquals(actualValue, null);

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} gracefully handles null attribute values.
     */
    @Test
    public void trackEventWithNullAttributeValues() throws Exception {
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.singletonMap("test", null)),
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // call track
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("test", null);
        optimizely.track(eventType.getKey(), genericUserId, attributes);

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventFactory).createConversionEvent(
             eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                attributeCaptor.capture(),
                eq(Collections.<String, Object>emptyMap()));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} handles the case where an unknown attribute
     * (i.e., not in the config) is passed through.
     * <p>
     * In this case, the track event call should not remove the unknown attribute from the given map but should go on and track the event successfully.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void trackEventWithUnknownAttribute() throws Exception {
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(new RaiseExceptionErrorHandler())
            .build();

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(ImmutableMap.of("unknownAttribute", "attributeValue")),
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // call track
        optimizely.track(eventType.getKey(), genericUserId, ImmutableMap.of("unknownAttribute", "attributeValue"));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventFactory).createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                attributeCaptor.capture(),
                eq(Collections.<String, Object>emptyMap()));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, hasKey("unknownAttribute"));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map, Map)} passes event features to
     * {@link EventFactory#createConversionEvent(ProjectConfig, String, String, String, Map, Map)}
     */
    @Test
    public void trackEventWithEventTags() throws Exception {
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, Object> eventTags = new HashMap<String, Object>();
        eventTags.put("int_param", 123);
        eventTags.put("string_param", "123");
        eventTags.put("boolean_param", false);
        eventTags.put("float_param", 12.3f);

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                anyMapOf(String.class, String.class),
                eq(eventTags)))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() + "\" for user \""
            + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // call track
        optimizely.track(eventType.getKey(), genericUserId, Collections.<String, String>emptyMap(), eventTags);

        // setup the event map captor (so we can verify its content)
        ArgumentCaptor<Map> eventTagCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventFactory).createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eventTagCaptor.capture());

        Map<String, ?> actualValue = eventTagCaptor.getValue();
        assertThat(actualValue, hasEntry("int_param", eventTags.get("int_param")));
        assertThat(actualValue, hasEntry("string_param", eventTags.get("string_param")));
        assertThat(actualValue, hasEntry("boolean_param", eventTags.get("boolean_param")));
        assertThat(actualValue, hasEntry("float_param", eventTags.get("float_param")));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map, Map)} called with null event tags will return null eventTag
     * when calling {@link EventFactory#createConversionEvent(ProjectConfig, String, String, String, Map, Map)}
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void trackEventWithNullEventTags() throws Exception {
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }

        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();
        Map<String, ?> eventTags = null;

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eq(eventTags)))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        // call track
        optimizely.track(eventType.getKey(), genericUserId, Collections.<String, String>emptyMap(), null);

        // verify that the event builder was called with the expected attributes
        verify(mockEventFactory).createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eq(eventTags));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }


    /**
     * Verify that {@link Optimizely#track(String, String, Map, Map)} called with null User ID will return and will not track
     */
    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void trackEventWithNullOrEmptyUserID() throws Exception {
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }
        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eq(Collections.<String, String>emptyMap())))
                .thenReturn(logEventToDispatch);

        String userID = null;
        // call track with null event key
        optimizely.track(eventType.getKey(), userID, Collections.<String, String>emptyMap(), Collections.<String, Object>emptyMap());
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
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = validProjectConfig.getEventTypes().get(0);
        }
        String nullEventKey = null;
        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eq(Collections.<String, String>emptyMap())))
                .thenReturn(logEventToDispatch);

        // call track with null event key
        optimizely.track(nullEventKey, genericUserId, Collections.<String, String>emptyMap(), Collections.<String, Object>emptyMap());
        logbackVerifier.expectMessage(Level.ERROR, "Event Key is null or empty when non-null and non-empty String was expected.");
        logbackVerifier.expectMessage(Level.INFO, "Not tracking event for user \"" + genericUserId + "\".");

    }
        /**
         * Verify that {@link Optimizely#track(String, String, Map)} dispatches an event always and logs appropriate message
         */
    @Test
    public void trackEventWithNoValidExperiments() throws Exception {
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = validProjectConfig.getEventNameMapping().get("clicked_purchase");
        }

    // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
                .withEventBuilder(mockEventFactory)
                .withConfig(validProjectConfig)
                .build();

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("browser_type", "firefox");

        when(mockEventFactory.createConversionEvent(
            eq(validProjectConfig),
            eq(genericUserId),
            eq(eventType.getId()),
            eq(eventType.getKey()),
            eq(attributes),
            eq(Collections.<String, Object>emptyMap())))
            .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        optimizely.track(eventType.getKey(), genericUserId, attributes);
        verify(mockEventHandler).dispatchEvent(eq(logEventToDispatch));
    }

    /**
     * Verify that {@link Optimizely#track(String, String)} handles exceptions thrown by
     * {@link EventHandler#dispatchEvent(LogEvent)} gracefully.
     */
    @Test
    public void trackDispatchEventThrowsException() throws Exception {
        EventType eventType = noAudienceProjectConfig.getEventTypes().get(0);

        doThrow(new Exception("Test Exception")).when(mockEventHandler).dispatchEvent(any(LogEvent.class));

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withConfig(noAudienceProjectConfig)
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Unexpected exception in event dispatcher");
        optimizely.track(eventType.getKey(), testUserId);
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map)}
     * dispatches events even if the event links only to launched experiments
     */
    @Test
    public void trackDoesNotSendEventWhenExperimentsAreLaunchedOnly() throws Exception {
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY);
        } else {
            eventType = noAudienceProjectConfig.getEventNameMapping().get("launched_exp_event");
        }

        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely client = Optimizely.builder(noAudienceDatafile, mockEventHandler)
                .withEventBuilder(mockEventFactory)
                .withConfig(noAudienceProjectConfig)
                .build();

        when(mockEventFactory.createConversionEvent(
            eq(noAudienceProjectConfig),
            eq(genericUserId),
            eq(eventType.getId()),
            eq(eventType.getKey()),
            eq(Collections.<String, String>emptyMap()),
            eq(Collections.<String, Object>emptyMap())))
            .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        client.track(eventType.getKey(), genericUserId);
        verify(mockEventHandler).dispatchEvent(eq(logEventToDispatch));
    }

    /**
     * Verify that {@link Optimizely#track(String, String, Map)}
     * dispatches log events when the tracked event links to both launched and running experiments.
     */
    @Test
    public void trackDispatchesWhenEventHasLaunchedAndRunningExperiments() throws Exception {
        EventFactory mockEventFactory = mock(EventFactory.class);
        EventType eventType;
        if (datafileVersion >= 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
        } else {
            eventType = noAudienceProjectConfig.getEventNameMapping().get("event_with_launched_and_running_experiments");
        }
        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : validProjectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, genericUserId))
                .thenReturn(experiment.getVariations().get(0));
        }

        Optimizely client = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(noAudienceProjectConfig)
            .withBucketing(mockBucketAlgorithm)
            .withEventBuilder(mockEventFactory)
            .build();

        when(mockEventFactory.createConversionEvent(
                eq(noAudienceProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(Collections.<String, String>emptyMap()),
                eq(Collections.<String, Object>emptyMap())
        )).thenReturn(logEventToDispatch);

        // The event has 1 launched experiment and 1 running experiment.
        // It should send a track event with the running experiment
        client.track(eventType.getKey(), genericUserId, Collections.<String, String>emptyMap());
        verify(client.eventHandler).dispatchEvent(eq(logEventToDispatch));
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

        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely client = Optimizely.builder(validDatafile, mockEventHandler)
           .withEventBuilder(mockEventFactory)
                .withConfig(validProjectConfig)
                .build();

        when(mockEventFactory.createConversionEvent(
            eq(validProjectConfig),
            eq(genericUserId),
            eq(eventType.getId()),
            eq(eventType.getKey()),
            eq(attributeMap),
            eq(Collections.<String, Object>emptyMap())))
            .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        client.track(eventType.getKey(), genericUserId, attributeMap);
        verify(mockEventHandler).dispatchEvent(eq(logEventToDispatch));
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
        verify(mockBucketer, never()).bucket(any(Experiment.class), anyString());
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    //======== getVariation tests ========//

    /**
     * Verify that {@link Optimizely#getVariation(Experiment, String)} correctly makes the
     * {@link Bucketer#bucket(Experiment, String)} call and does NOT dispatch an event.
     */
    @Test
    public void getVariation() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        when(mockBucketer.bucket(activatedExperiment, testUserId)).thenReturn(bucketedVariation);

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        // activate the experiment
        Variation actualVariation = optimizely.getVariation(activatedExperiment.getKey(), testUserId,
            testUserAttributes);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testUserId);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that we didn't attempt to dispatch an event
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} correctly makes the
     * {@link Bucketer#bucket(Experiment, String)} call and does NOT dispatch an event.
     */
    @Test
    public void getVariationWithExperimentKey() throws Exception {
        Experiment activatedExperiment = noAudienceProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        when(mockBucketer.bucket(activatedExperiment, testUserId)).thenReturn(bucketedVariation);

        // activate the experiment
        Variation actualVariation = optimizely.getVariation(activatedExperiment.getKey(), testUserId);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testUserId);
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
        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

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

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withErrorHandler(new NoOpErrorHandler())
            .build();

        logbackVerifier.expectMessage(Level.ERROR, "Experiment \"unknown_experiment\" is not in the datafile");

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

        when(mockBucketer.bucket(experiment, testUserId)).thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withBucketing(mockBucketer)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");

        Variation actualVariation = optimizely.getVariation(experiment.getKey(), testUserId, testUserAttributes);

        verify(mockBucketer).bucket(experiment, testUserId);
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

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withErrorHandler(mockErrorHandler)
            .build();

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

        when(mockBucketer.bucket(experiment, testUserId)).thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withConfig(noAudienceProjectConfig)
            .withBucketing(mockBucketer)
            .withErrorHandler(mockErrorHandler)
            .build();

        Variation actualVariation = optimizely.getVariation(experiment.getKey(), testUserId);

        verify(mockBucketer).bucket(experiment, testUserId);
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

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
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

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
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

        Map<String, String> attributes = new HashMap<String, String>();
        if (datafileVersion >= 4) {
            attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            attributes.put("browser_type", "chrome");
        }

        when(mockBucketer.bucket(experiment, "user")).thenReturn(variation);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withBucketing(mockBucketer)
            .build();

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

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

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

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

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
        verify(mockBucketer, never()).bucket(any(Experiment.class), anyString());
    }

    //======== Notification listeners ========//

    /**
     * Helper method to return decisionListener
     **/
    private DecisionNotificationListener getDecisionListener(final String testType,
                                                             final String testUserId,
                                                             final Map<String, ?> testUserAttributes,
                                                             final Map<String, ?> testDecisionInfo) {
        return new DecisionNotificationListener() {
            @Override
            public void onDecision(@Nonnull String type, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Map<String, ?> decisionInfo) {
                assertEquals(type, testType);
                assertEquals(userId, testUserId);
                for (Map.Entry<String, ?> entry : attributes.entrySet()) {
                    assertEquals(testUserAttributes.get(entry.getKey()), entry.getValue());
                }
                for (Map.Entry<String, ?> entry : decisionInfo.entrySet()) {
                    assertEquals(testDecisionInfo.get(entry.getKey()), entry.getValue());
                }
            }
        };
    }


    //======GetFeatureVariable Notification TESTS======//

    /**
     * Verify that the {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * notification listener of getFeatureVariableString is called when feature is in experiment and feature is true
     */
    @Test
    public void getFeatureVariableWithListenerUserInExperimentFeatureOn() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;
        String expectedValue = "F";

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE_EXPERIMENT_KEY.toString(), "multivariate_experiment");
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE_VARIATION_KEY.toString(), "Fred");
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_KEY.toString(), validFeatureKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_ENABLED.toString(), true);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_KEY.toString(), validVariableKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_TYPE.toString(), FeatureVariable.VariableType.STRING);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_VALUE.toString(), expectedValue);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE.toString(), FeatureDecision.DecisionSource.EXPERIMENT);

        int notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.OnDecision,
            getDecisionListener(NotificationCenter.OnDecisionNotificationType.GET_FEATURE_VARIABLE.toString(),
                testUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals(optimizely.getFeatureVariableString(
            validFeatureKey,
            validVariableKey,
            testUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            expectedValue);

        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * notification listener of getFeatureVariableString is called when feature is in experiment and feature enabled is false
     * than default value will get returned
     */
    @Test
    public void getFeatureVariableWithListenerUserInExperimentFeatureOff() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;
        String expectedValue = "H";
        String userID = "Gred";

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE_EXPERIMENT_KEY.toString(), "multivariate_experiment");
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE_VARIATION_KEY.toString(), "Gred");
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_KEY.toString(), validFeatureKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_ENABLED.toString(), false);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_KEY.toString(), validVariableKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_TYPE.toString(), FeatureVariable.VariableType.STRING);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_VALUE.toString(), expectedValue);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE.toString(), FeatureDecision.DecisionSource.EXPERIMENT);

        int notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.OnDecision,
            getDecisionListener(NotificationCenter.OnDecisionNotificationType.GET_FEATURE_VARIABLE.toString(),
                userID,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals(optimizely.getFeatureVariableString(
            validFeatureKey,
            validVariableKey,
            userID,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            expectedValue);

        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * notification listener of getFeatureVariableString is called when feature is in rollout and feature enabled is true
     */
    @Test
    public void getFeatureVariableWithListenerUserInRollOutFeatureOn() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_STRING_KEY;
        String validVariableKey = VARIABLE_STRING_VARIABLE_KEY;
        String expectedValue = "lumos";

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();

        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE_EXPERIMENT_KEY.toString(), null);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_KEY.toString(), validFeatureKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_ENABLED.toString(), true);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_KEY.toString(), validVariableKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_TYPE.toString(), FeatureVariable.VariableType.STRING);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_VALUE.toString(), expectedValue);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE.toString(), FeatureDecision.DecisionSource.ROLLOUT);

        int notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.OnDecision,
            getDecisionListener(NotificationCenter.OnDecisionNotificationType.GET_FEATURE_VARIABLE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals(optimizely.getFeatureVariableString(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            expectedValue);

        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableBoolean(String, String, String, Map)}
     * notification listener of getFeatureVariableBoolean is called when feature is not in rollout and feature enabled is true
     */
    @Test
    public void getFeatureVariableWithListenerUserNotInRollOutFeatureOff() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY;
        String validVariableKey = VARIABLE_BOOLEAN_VARIABLE_KEY;
        Boolean expectedValue = true;

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();

        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE_EXPERIMENT_KEY.toString(), null);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_KEY.toString(), validFeatureKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_ENABLED.toString(), false);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_KEY.toString(), validVariableKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_TYPE.toString(), FeatureVariable.VariableType.BOOLEAN);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_VALUE.toString(), String.valueOf(expectedValue));
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE.toString(), FeatureDecision.DecisionSource.ROLLOUT);

        int notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.OnDecision,
            getDecisionListener(NotificationCenter.OnDecisionNotificationType.GET_FEATURE_VARIABLE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals(optimizely.getFeatureVariableBoolean(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            expectedValue);

        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableInteger(String, String, String, Map)}
     * notification listener of getFeatureVariableInteger is called when feature is in rollout and feature enabled is true
     */
    @Test
    public void getFeatureVariableIntegerWithListenerUserInRollOutFeatureOn() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_INTEGER_KEY;
        String validVariableKey = VARIABLE_INTEGER_VARIABLE_KEY;
        int expectedValue = 7;

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE_EXPERIMENT_KEY.toString(), null);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_KEY.toString(), validFeatureKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_ENABLED.toString(), true);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_KEY.toString(), validVariableKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_TYPE.toString(), FeatureVariable.VariableType.INTEGER);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_VALUE.toString(), String.valueOf(expectedValue));
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE.toString(), FeatureDecision.DecisionSource.ROLLOUT);

        int notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.OnDecision,
            getDecisionListener(NotificationCenter.OnDecisionNotificationType.GET_FEATURE_VARIABLE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals((long) optimizely.getFeatureVariableInteger(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)),
            (long) expectedValue);

        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
    }

    /**
     * Verify that the {@link Optimizely#getFeatureVariableDouble(String, String, String, Map)}
     * notification listener of getFeatureVariableDouble is called when feature is in experiment and feature enabled is true
     */
    @Test
    public void getFeatureVariableDoubleWithListenerUserInExperimentFeatureOn() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        final String validFeatureKey = FEATURE_SINGLE_VARIABLE_DOUBLE_KEY;
        String validVariableKey = VARIABLE_DOUBLE_VARIABLE_KEY;

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        final Map<String, String> testUserAttributes = new HashMap<>();
        testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_SLYTHERIN_VALUE);

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE_EXPERIMENT_KEY.toString(), "double_single_variable_feature_experiment");
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE_VARIATION_KEY.toString(), "pi_variation");
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_KEY.toString(), validFeatureKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.FEATURE_ENABLED.toString(), true);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_KEY.toString(), validVariableKey);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_TYPE.toString(), FeatureVariable.VariableType.DOUBLE);
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.VARIABLE_VALUE.toString(), "3.14");
        testDecisionInfoMap.put(DecisionInfoEnums.GetFeatureVariableDecisionInfo.SOURCE.toString(), FeatureDecision.DecisionSource.EXPERIMENT);

        int notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.OnDecision,
            getDecisionListener(NotificationCenter.OnDecisionNotificationType.GET_FEATURE_VARIABLE.toString(),
                genericUserId,
                testUserAttributes,
                testDecisionInfoMap));

        assertEquals(optimizely.getFeatureVariableDouble(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_SLYTHERIN_VALUE)),
            Math.PI, 2);

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
        final Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        final Map<String, String> testUserAttributes = new HashMap<String, String>();
        if (datafileVersion >= 4) {
            testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            testUserAttributes.put("browser_type", "chrome");
        }

        testUserAttributes.put(testBucketingIdKey, testBucketingId);

        ActivateNotificationListener activateNotification = new ActivateNotificationListener() {
            @Override
            public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {
                assertEquals(experiment.getKey(), activatedExperiment.getKey());
                assertEquals(bucketedVariation.getKey(), variation.getKey());
                assertEquals(userId, testUserId);
                for (Map.Entry<String, ?> entry : attributes.entrySet()) {
                    assertEquals(testUserAttributes.get(entry.getKey()), entry.getValue());
                }

                assertEquals(event.getRequestMethod(), RequestMethod.GET);
            }

        };

        int notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, activateNotification);

        when(mockEventFactory.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
            eq(testUserId), eq(testUserAttributes)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, testBucketingId))
            .thenReturn(bucketedVariation);


        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, testUserAttributes);

        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));
        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testBucketingId);
        assertThat(actualVariation, is(bucketedVariation));

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    @Test
    @SuppressFBWarnings(
        value = "NP_NONNULL_PARAM_VIOLATION",
        justification = "testing nullness contract violation")
    public void activateWithListenerNullAttributes() throws Exception {
        final Experiment activatedExperiment = noAudienceProjectConfig.getExperiments().get(0);
        final Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        // setup a mock event builder to return expected impression params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(noAudienceProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();
        Map<String, String> attributes = null;

        when(mockEventFactory.createImpressionEvent(eq(noAudienceProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
            eq(testUserId), eq(attributes)))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, testUserId))
            .thenReturn(bucketedVariation);

        ActivateNotificationListener activateNotification = new ActivateNotificationListener() {
            @Override
            public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {
                assertEquals(experiment.getKey(), activatedExperiment.getKey());
                assertEquals(bucketedVariation.getKey(), variation.getKey());
                assertEquals(userId, testUserId);
                assertNull(attributes);

                assertEquals(event.getRequestMethod(), RequestMethod.GET);
            }

        };

        int notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, activateNotification);

        // activate the experiment
        Variation actualVariation = optimizely.activate(activatedExperiment.getKey(), testUserId, attributes);

        optimizely.notificationCenter.removeNotificationListener(notificationId);

        // verify that the bucketing algorithm was called correctly
        verify(mockBucketer).bucket(activatedExperiment, testUserId);
        assertThat(actualVariation, is(bucketedVariation));

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockEventFactory).createImpressionEvent(eq(noAudienceProjectConfig), eq(activatedExperiment),
            eq(bucketedVariation), eq(testUserId), attributeCaptor.capture());

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertNull(actualValue);

        // verify that dispatchEvent was called with the correct LogEvent object
        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Verify that {@link com.optimizely.ab.notification.NotificationCenter#addNotificationListener(
     *com.optimizely.ab.notification.NotificationCenter.NotificationType,
     * com.optimizely.ab.notification.NotificationListener)} properly used
     * and the listener is
     * added and notified when an experiment is activated.
     */
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
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withDecisionService(mockDecisionService)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> attributes = Collections.emptyMap();

        when(mockEventFactory.createImpressionEvent(validProjectConfig, activatedExperiment,
            bucketedVariation, genericUserId, attributes))
            .thenReturn(logEventToDispatch);

        when(mockDecisionService.getVariation(
            eq(activatedExperiment),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())))
            .thenReturn(bucketedVariation);

        // Add listener
        ActivateNotificationListener listener = mock(ActivateNotificationListener.class);
        optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, listener);

        // Check if listener is notified when experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, genericUserId, attributes);
        verify(listener, times(1))
            .onActivate(activatedExperiment, genericUserId, attributes, bucketedVariation, logEventToDispatch);

        assertEquals(actualVariation.getKey(), bucketedVariation.getKey());
        // Check if listener is notified after an event is tracked
        String eventKey = eventType.getKey();

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventKey),
                eq(attributes),
                anyMapOf(String.class, Object.class)))
                .thenReturn(logEventToDispatch);

        TrackNotificationListener trackNotification = mock(TrackNotificationListener.class);

        optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, trackNotification);

        optimizely.track(eventKey, genericUserId, attributes);
        verify(trackNotification, times(1))
            .onTrack(eventKey, genericUserId, attributes, Collections.EMPTY_MAP, logEventToDispatch);
    }

    /**
     * Verify that {@link com.optimizely.ab.notification.NotificationCenter} properly
     * calls and the listener is removed and no longer notified when an experiment is activated.
     */
    @Test
    public void removeNotificationListenerNotificationCenter() throws Exception {
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        Map<String, String> attributes = new HashMap<String, String>();

        if (datafileVersion >= 4) {
            attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            attributes.put("browser_type", "chrome");
        }

        when(mockBucketer.bucket(activatedExperiment, genericUserId))
            .thenReturn(bucketedVariation);

        when(mockEventFactory.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, genericUserId,
            attributes))
            .thenReturn(logEventToDispatch);

        // Add and remove listener
        ActivateNotificationListener activateNotification = mock(ActivateNotificationListener.class);
        int notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, activateNotification);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));

        TrackNotificationListener trackNotification = mock(TrackNotificationListener.class);
        notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, trackNotification);
        assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId));

        // Check if listener is notified after an experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, genericUserId, attributes);
        verify(activateNotification, never())
            .onActivate(activatedExperiment, genericUserId, attributes, actualVariation, logEventToDispatch);

        // Check if listener is notified after a feature variable is accessed
        verify(activateNotification, never())
            .onActivate(activatedExperiment, genericUserId, attributes, actualVariation, logEventToDispatch);

        // Check if listener is notified after an event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventKey),
                eq(attributes),
                anyMapOf(String.class, Object.class)))
                .thenReturn(logEventToDispatch);

        optimizely.track(eventKey, genericUserId, attributes);
        verify(trackNotification, never())
            .onTrack(eventKey, genericUserId, attributes, Collections.EMPTY_MAP, logEventToDispatch);
    }

    /**
     * Verify that {@link com.optimizely.ab.notification.NotificationCenter}
     * clearAllListerners removes all listeners
     * and no longer notified when an experiment is activated.
     */
    @Test
    public void clearNotificationListenersNotificationCenter() throws Exception {
        Experiment activatedExperiment;
        Map<String, String> attributes = new HashMap<String, String>();
        if (datafileVersion >= 4) {
            activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
            attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        } else {
            activatedExperiment = validProjectConfig.getExperiments().get(0);
            attributes.put("browser_type", "chrome");
        }
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        when(mockEventFactory.createImpressionEvent(validProjectConfig, activatedExperiment,
            bucketedVariation, genericUserId, attributes))
            .thenReturn(logEventToDispatch);

        when(mockBucketer.bucket(activatedExperiment, genericUserId))
            .thenReturn(bucketedVariation);

        // set up argument captor for the attributes map to compare map equality
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        when(mockEventFactory.createImpressionEvent(
            eq(validProjectConfig),
            eq(activatedExperiment),
            eq(bucketedVariation),
            eq(genericUserId),
            attributeCaptor.capture()
        )).thenReturn(logEventToDispatch);

        ActivateNotificationListener activateNotification = mock(ActivateNotificationListener.class);
        TrackNotificationListener trackNotification = mock(TrackNotificationListener.class);

        optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, activateNotification);
        optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, trackNotification);

        optimizely.notificationCenter.clearAllNotificationListeners();

        // Check if listener is notified after an experiment is activated
        Variation actualVariation = optimizely.activate(activatedExperiment, genericUserId, attributes);

        // check that the argument that was captured by the mockEventBuilder attribute captor,
        // was equal to the attributes passed in to activate
        assertEquals(attributes, attributeCaptor.getValue());
        verify(activateNotification, never())
            .onActivate(activatedExperiment, genericUserId, attributes, actualVariation, logEventToDispatch);

        // Check if listener is notified after a feature variable is accessed
        verify(activateNotification, never())
            .onActivate(activatedExperiment, genericUserId, attributes, actualVariation, logEventToDispatch);

        // Check if listener is notified after a event is tracked
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String eventKey = eventType.getKey();

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(OptimizelyTest.genericUserId),
                eq(eventType.getId()),
                eq(eventKey),
                eq(attributes),
                anyMapOf(String.class, Object.class)))
                .thenReturn(logEventToDispatch);

        optimizely.track(eventKey, genericUserId, attributes);
        verify(trackNotification, never())
            .onTrack(eventKey, genericUserId, attributes, Collections.EMPTY_MAP, logEventToDispatch);
    }

    /**
     * Add notificaiton listener for track {@link com.optimizely.ab.notification.NotificationCenter}.  Verify called and
     * remove.
     *
     * @throws Exception
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

        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        final Map<String, String> attributes = ImmutableMap.of(attribute.getKey(), "attributeValue");

        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                anyMapOf(String.class, String.class),
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        TrackNotificationListener trackNotification = new TrackNotificationListener() {
            @Override
            public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, ?> _attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {
                assertEquals(eventType.getKey(), eventKey);
                assertEquals(genericUserId, userId);
                assertEquals(attributes, _attributes);
                assertTrue(eventTags.isEmpty());
            }
        };

        int notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, trackNotification);

        // call track
        optimizely.track(eventType.getKey(), genericUserId, attributes);

        optimizely.notificationCenter.removeNotificationListener(notificationId);

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventFactory).createConversionEvent(
            eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                attributeCaptor.capture(),
                eq(Collections.<String, Object>emptyMap()));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertThat(actualValue, hasEntry(attribute.getKey(), "attributeValue"));

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    /**
     * Track with listener and verify that {@link Optimizely#track(String, String)} returns null attributes.
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

        // setup a mock event builder to return expected conversion params
        EventFactory mockEventFactory = mock(EventFactory.class);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withEventBuilder(mockEventFactory)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

     Map<String, String> attributes = null;
        when(mockEventFactory.createConversionEvent(
                eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                eq(attributes),
                eq(Collections.<String, Object>emptyMap())))
                .thenReturn(logEventToDispatch);

        logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.getKey() +
            "\" for user \"" + genericUserId + "\".");
        logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
            testParams + " and payload \"{}\"");

        TrackNotificationListener trackNotification = new TrackNotificationListener() {
            @Override
            public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {
                assertEquals(eventType.getKey(), eventKey);
                assertEquals(genericUserId, userId);
                assertNull(attributes.isEmpty());
                assertTrue(eventTags.isEmpty());
            }
        };

        int notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, trackNotification);

        // call track
        optimizely.track(eventType.getKey(), genericUserId, attributes);

        optimizely.notificationCenter.removeNotificationListener(notificationId);

        // setup the attribute map captor (so we can verify its content)
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);

        // verify that the event builder was called with the expected attributes
        verify(mockEventFactory).createConversionEvent(
            eq(validProjectConfig),
                eq(genericUserId),
                eq(eventType.getId()),
                eq(eventType.getKey()),
                attributeCaptor.capture(),
                eq(Collections.<String, Object>emptyMap()));

        Map<String, String> actualValue = attributeCaptor.getValue();
        assertNull(actualValue);

        verify(mockEventHandler).dispatchEvent(logEventToDispatch);
    }

    //======== Feature Accessor Tests ========//

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns null and logs a message
     * when it is called with a feature key that has no corresponding feature in the datafile.
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueForTypeReturnsNullWhenFeatureNotFound() throws ConfigParseException {

        String invalidFeatureKey = "nonexistent feature key";
        String invalidVariableKey = "nonexistent variable key";
        Map<String, String> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build();

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
            times(2));

        verify(mockDecisionService, never()).getVariation(
            any(Experiment.class),
            anyString(),
            anyMapOf(String.class, String.class));
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns null and logs a message
     * when the feature key is valid, but no variable could be found for the variable key in the feature.
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueForTypeReturnsNullWhenVariableNotFoundInValidFeature() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String invalidVariableKey = "nonexistent variable key";

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build();

        String value = optimizely.getFeatureVariableValueForType(
            validFeatureKey,
            invalidVariableKey,
            genericUserId,
            Collections.<String, String>emptyMap(),
            FeatureVariable.VariableType.STRING);
        assertNull(value);

        logbackVerifier.expectMessage(Level.INFO,
            "No feature variable was found for key \"" + invalidVariableKey + "\" in feature flag \"" +
                validFeatureKey + "\".");

        verify(mockDecisionService, never()).getVariation(
            any(Experiment.class),
            anyString(),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns null when the variable's type does not match the type with which it was attempted to be accessed.
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueReturnsNullWhenVariableTypeDoesNotMatch() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build();

        String value = optimizely.getFeatureVariableValueForType(
            validFeatureKey,
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
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueForTypeReturnsDefaultValueWhenFeatureIsNotAttachedToExperimentOrRollout() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY;
        String validVariableKey = VARIABLE_BOOLEAN_VARIABLE_KEY;
        String defaultValue = VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE;
        Map<String, String> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        String value = optimizely.getFeatureVariableValueForType(
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
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueReturnsDefaultValueWhenFeatureIsAttachedToOneExperimentButFailsTargeting() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_SINGLE_VARIABLE_DOUBLE_KEY;
        String validVariableKey = VARIABLE_DOUBLE_VARIABLE_KEY;
        String expectedValue = VARIABLE_DOUBLE_DEFAULT_VALUE;
        FeatureFlag featureFlag = FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE;
        Experiment experiment = validProjectConfig.getExperimentIdMapping().get(featureFlag.getExperimentIds().get(0));

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        String valueWithImproperAttributes = optimizely.getFeatureVariableValueForType(
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
     * returns the variable value of the variation the user is bucketed into
     * if the variation is not null and the variable has a usage within the variation but featureEnabled is false than returns default value.
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueReturnsVariationValueWhenUserGetsBucketedToVariation() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;
        String validVariableKey = VARIABLE_FIRST_LETTER_KEY;
        String expectedValue = VARIABLE_FIRST_LETTER_DEFAULT_VALUE;
        Experiment multivariateExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build();

        FeatureDecision featureDecision = new FeatureDecision(multivariateExperiment, VARIATION_MULTIVARIATE_EXPERIMENT_GRED, FeatureDecision.DecisionSource.EXPERIMENT);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            FEATURE_FLAG_MULTI_VARIATE_FEATURE,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
        );

        String value = optimizely.getFeatureVariableValueForType(
            validFeatureKey,
            validVariableKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE),
            FeatureVariable.VariableType.STRING
        );

        assertEquals(expectedValue, value);
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}
     * returns the default value for the feature variable
     * when there is no variable usage present for the variation the user is bucketed into.
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableValueReturnsDefaultValueWhenNoVariationUsageIsPresent() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_SINGLE_VARIABLE_INTEGER_KEY;
        String validVariableKey = VARIABLE_INTEGER_VARIABLE_KEY;
        FeatureVariable variable = FEATURE_FLAG_SINGLE_VARIABLE_INTEGER.getVariableKeyToFeatureVariableMap().get(validVariableKey);
        String expectedValue = variable.getDefaultValue();

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        String value = optimizely.getFeatureVariableValueForType(
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
     *
     * @throws Exception
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void isFeatureEnabledReturnsFalseWhenFeatureKeyIsNull() throws Exception {
        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build());

        assertFalse(spyOptimizely.isFeatureEnabled(null, genericUserId));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The featureKey parameter must be nonnull."
        );

        verify(spyOptimizely, times(1)).isFeatureEnabled(
            isNull(String.class),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );
        verify(mockDecisionService, never()).getVariationForFeature(
            any(FeatureFlag.class),
            any(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return False
     * when the APIs are called with a null value for the user ID parameter.
     *
     * @throws Exception
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void isFeatureEnabledReturnsFalseWhenUserIdIsNull() throws Exception {
        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build());

        String featureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        assertFalse(spyOptimizely.isFeatureEnabled(featureKey, null));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The userId parameter must be nonnull."
        );

        verify(spyOptimizely, times(1)).isFeatureEnabled(
            eq(featureKey),
            isNull(String.class),
            eq(Collections.<String, String>emptyMap())
        );
        verify(mockDecisionService, never()).getVariationForFeature(
            any(FeatureFlag.class),
            any(String.class),
            anyMapOf(String.class, String.class)
        );
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return False
     * when the APIs are called with a feature key that is not in the datafile.
     *
     * @throws Exception
     */
    @Test
    public void isFeatureEnabledReturnsFalseWhenFeatureFlagKeyIsInvalid() throws Exception {

        String invalidFeatureKey = "nonexistent feature key";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build());

        assertFalse(spyOptimizely.isFeatureEnabled(invalidFeatureKey, genericUserId));

        logbackVerifier.expectMessage(
            Level.INFO,
            "No feature flag was found for key \"" + invalidFeatureKey + "\"."
        );
        verify(spyOptimizely, times(1)).isFeatureEnabled(
            eq(invalidFeatureKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );
        verify(mockDecisionService, never()).getVariation(
            any(Experiment.class),
            anyString(),
            anyMapOf(String.class, String.class));
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return False
     * when the user is not bucketed into any variation for the feature.
     *
     * @throws Exception
     */
    @Test
    public void isFeatureEnabledReturnsFalseWhenUserIsNotBucketedIntoAnyVariation() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build());

        FeatureDecision featureDecision = new FeatureDecision(null, null, null);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            any(FeatureFlag.class),
            anyString(),
            anyMapOf(String.class, String.class)
        );

        assertFalse(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId));

        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is not enabled for user \"" + genericUserId + "\"."
        );
        verify(spyOptimizely).isFeatureEnabled(
            eq(validFeatureKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );
        verify(mockDecisionService).getVariationForFeature(
            eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return True when the user is bucketed into a variation for the feature.
     * An impression event should not be dispatched since the user was not bucketed into an Experiment.
     *
     * @throws Exception
     */
    @Test
    public void isFeatureEnabledReturnsTrueButDoesNotSendWhenUserIsBucketedIntoVariationWithoutExperiment() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build());

        // Should be an experiment from the rollout associated with the feature, but for this test
        // it doesn't matter. Just use any valid experiment.
        Experiment experiment = validProjectConfig.getRolloutIdMapping().get(ROLLOUT_2_ID).getExperiments().get(0);
        Variation variation = new Variation("variationId", "variationKey", true, null);
        FeatureDecision featureDecision = new FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.ROLLOUT);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );

        assertTrue(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId));

        logbackVerifier.expectMessage(
            Level.INFO,
            "The user \"" + genericUserId +
                "\" is not included in an experiment for feature \"" + validFeatureKey + "\"."
        );
        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is enabled for user \"" + genericUserId + "\"."
        );
        verify(spyOptimizely).isFeatureEnabled(
            eq(validFeatureKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );
        verify(mockDecisionService).getVariationForFeature(
            eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );
        verify(mockEventHandler, never()).dispatchEvent(any(LogEvent.class));
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

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, forcedVariation.getKey());
        assertFalse(optimizely.isFeatureEnabled(FEATURE_FLAG_MULTI_VARIATE_FEATURE.getKey(), testUserId));
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

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .withConfig(validProjectConfig)
            .withErrorHandler(mockErrorHandler)
            .build();

        optimizely.setForcedVariation(activatedExperiment.getKey(), testUserId, forcedVariation.getKey());
        assertFalse(optimizely.isFeatureEnabled(FEATURE_SINGLE_VARIABLE_DOUBLE_KEY, testUserId));
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} sending FeatureEnabled true and they both
     * return True when the user is bucketed into a variation for the feature.
     * An impression event should not be dispatched since the user was not bucketed into an Experiment.
     *
     * @throws Exception
     */
    @Test
    public void isFeatureEnabledTrueWhenFeatureEnabledOfVariationIsTrue() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build());
        // Should be an experiment from the rollout associated with the feature, but for this test
        // it doesn't matter. Just use any valid experiment.
        Experiment experiment = validProjectConfig.getRolloutIdMapping().get(ROLLOUT_2_ID).getExperiments().get(0);
        Variation variation = new Variation("variationId", "variationKey", true, null);
        FeatureDecision featureDecision = new FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.ROLLOUT);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );

        assertTrue(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId));

    }


    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} sending FeatureEnabled false because of which and they both
     * return false even when the user is bucketed into a variation for the feature.
     * An impression event should not be dispatched since the user was not bucketed into an Experiment.
     *
     * @throws Exception
     */
    @Test
    public void isFeatureEnabledFalseWhenFeatureEnabledOfVariationIsFalse() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build());
        // Should be an experiment from the rollout associated with the feature, but for this test
        // it doesn't matter. Just use any valid experiment.
        Experiment experiment = validProjectConfig.getRolloutIdMapping().get(ROLLOUT_2_ID).getExperiments().get(0);
        Variation variation = new Variation("variationId", "variationKey", false, null);
        FeatureDecision featureDecision = new FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.ROLLOUT);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );

        assertFalse(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId));

    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} and they both
     * return False
     * when the user is bucketed an feature test variation that is turned off.
     *
     * @throws Exception
     */
    @Test
    public void isFeatureEnabledReturnsFalseAndDispatchesWhenUserIsBucketedIntoAnExperimentVariationToggleOff() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .withDecisionService(mockDecisionService)
            .build());

        Experiment activatedExperiment = validProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY);
        Variation variation = new Variation("2", "variation_toggled_off", false, null);

        FeatureDecision featureDecision = new FeatureDecision(activatedExperiment, variation, FeatureDecision.DecisionSource.EXPERIMENT);
        doReturn(featureDecision).when(mockDecisionService).getVariationForFeature(
            any(FeatureFlag.class),
            anyString(),
            anyMapOf(String.class, String.class)
        );

        assertFalse(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId));

        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is not enabled for user \"" + genericUserId + "\"."
        );
        verify(mockEventHandler, times(1)).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Integration Test
     * Verify {@link Optimizely#isFeatureEnabled(String, String, Map)}
     * returns True
     * when the user is bucketed into a variation for the feature.
     * The user is also bucketed into an experiment, so we verify that an event is dispatched.
     *
     * @throws Exception
     */
    @Test
    public void isFeatureEnabledReturnsTrueAndDispatchesEventWhenUserIsBucketedIntoAnExperiment() throws Exception {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY;

        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build();

        assertTrue(optimizely.isFeatureEnabled(
            validFeatureKey,
            genericUserId,
            Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
        ));

        logbackVerifier.expectMessage(
            Level.INFO,
            "Feature \"" + validFeatureKey +
                "\" is enabled for user \"" + genericUserId + "\"."
        );
        verify(mockEventHandler, times(1)).dispatchEvent(any(LogEvent.class));
    }

    /**
     * Verify that we don't attempt to activate the user when the Optimizely instance is not valid
     */
    @Test
    public void isFeatureEnabledWithInvalidDatafile() throws Exception {
        Optimizely optimizely = Optimizely.builder(invalidProjectConfigV5(), mockEventHandler)
            .withDecisionService(mockDecisionService)
            .build();
        Boolean isEnabled = optimizely.isFeatureEnabled("no_variable_feature", genericUserId);
        assertFalse(isEnabled);

        // make sure we didn't even attempt to bucket the user
        verify(mockDecisionService, never()).getVariationForFeature(any(FeatureFlag.class), anyString(), anyMap());
    }

    /**
     * Verify {@link Optimizely#getEnabledFeatures(String, Map)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} for each featureFlag
     * return List of FeatureFlags that are enabled
     */
    @Test
    public void getEnabledFeatureWithValidUserId() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());
        ArrayList<String> featureFlags = (ArrayList<String>) spyOptimizely.getEnabledFeatures(genericUserId,
            new HashMap<String, String>());
        assertFalse(featureFlags.isEmpty());

    }

    /**
     * Verify {@link Optimizely#getEnabledFeatures(String, Map)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} for each featureFlag sending
     * userId as empty string
     * return empty List of FeatureFlags without checking further.
     */
    @Test
    public void getEnabledFeatureWithEmptyUserId() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());
        ArrayList<String> featureFlags = (ArrayList<String>) spyOptimizely.getEnabledFeatures("",
            new HashMap<String, String>());
        assertFalse(featureFlags.isEmpty());

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
    public void getEnabledFeatureWithNullUserID() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));
        String userID = null;
        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());
        ArrayList<String> featureFlags = (ArrayList<String>) spyOptimizely.getEnabledFeatures(userID,
            new HashMap<String, String>());
        assertTrue(featureFlags.isEmpty());

        logbackVerifier.expectMessage(
            Level.ERROR,
            "The user ID parameter must be nonnull."
        );
    }

    /**
     * Verify {@link Optimizely#getEnabledFeatures(String, Map)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} for each featureFlag sending
     * userId and emptyMap and Mocked {@link Optimizely#isFeatureEnabled(String, String, Map)}
     * to return false so {@link Optimizely#getEnabledFeatures(String, Map)} will
     * return empty List of FeatureFlags.
     */
    @Test
    public void getEnabledFeatureWithMockIsFeatureEnabledToReturnFalse() throws ConfigParseException {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());
        doReturn(false).when(spyOptimizely).isFeatureEnabled(
            any(String.class),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap())
        );
        ArrayList<String> featureFlags = (ArrayList<String>) spyOptimizely.getEnabledFeatures(genericUserId,
            Collections.<String, String>emptyMap());
        assertTrue(featureFlags.isEmpty());
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableString(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * and returns null
     * when called with a null value for the feature Key parameter.
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableStringReturnsNullWhenFeatureKeyIsNull() throws ConfigParseException {
        String variableKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

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
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableStringReturnsNullWhenVariableKeyIsNull() throws ConfigParseException {
        String featureKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

        assertNull(spyOptimizely.getFeatureVariableString(
            featureKey,
            null,
            genericUserId
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The variableKey parameter must be nonnull."
        );
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
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableStringReturnsNullWhenUserIdIsNull() throws ConfigParseException {
        String featureKey = "";
        String variableKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

        assertNull(spyOptimizely.getFeatureVariableString(
            featureKey,
            variableKey,
            null
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The userId parameter must be nonnull."
        );
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
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableStringReturnsNullFromInternal() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

        doReturn(null).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.STRING)
        );

        assertNull(spyOptimizely.getFeatureVariableString(
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
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableString(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableString(String, String, String, Map)}
     * and both return the value returned from
     * {@link Optimizely#getFeatureVariableValueForType(String, String, String, Map, FeatureVariable.VariableType)}.
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableStringReturnsWhatInternalReturns() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        String valueNoAttributes = "valueNoAttributes";
        String valueWithAttributes = "valueWithAttributes";
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());


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
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableBooleanReturnsNullWhenFeatureKeyIsNull() throws ConfigParseException {
        String variableKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

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
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableBooleanReturnsNullWhenVariableKeyIsNull() throws ConfigParseException {
        String featureKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

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
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableBooleanReturnsNullWhenUserIdIsNull() throws ConfigParseException {
        String featureKey = "";
        String variableKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

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
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableBooleanReturnsNullFromInternal() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

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
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableBooleanReturnsWhatInternalReturns() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        Boolean valueNoAttributes = false;
        Boolean valueWithAttributes = true;
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());


        doReturn(valueNoAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.BOOLEAN)
        );

        doReturn(valueWithAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
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
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableDoubleReturnsNullFromInternal() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

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
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableDoubleReturnsWhatInternalReturns() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        Double valueNoAttributes = 0.1;
        Double valueWithAttributes = 0.2;
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());


        doReturn(valueNoAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.DOUBLE)
        );

        doReturn(valueWithAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
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
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableIntegerReturnsNullFromInternal() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

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
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableDoubleReturnsNullWhenFeatureKeyIsNull() throws ConfigParseException {
        String variableKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

        assertNull(spyOptimizely.getFeatureVariableDouble(
            null,
            variableKey,
            genericUserId
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The featureKey parameter must be nonnull."
        );
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
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableDoubleReturnsNullWhenVariableKeyIsNull() throws ConfigParseException {
        String featureKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

        assertNull(spyOptimizely.getFeatureVariableDouble(
            featureKey,
            null,
            genericUserId
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The variableKey parameter must be nonnull."
        );
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
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableDoubleReturnsNullWhenUserIdIsNull() throws ConfigParseException {
        String featureKey = "";
        String variableKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

        assertNull(spyOptimizely.getFeatureVariableDouble(
            featureKey,
            variableKey,
            null
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The userId parameter must be nonnull."
        );
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
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableDoubleCatchesExceptionFromParsing() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        String unParsableValue = "not_a_double";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

        doReturn(unParsableValue).when(spyOptimizely).getFeatureVariableValueForType(
            anyString(),
            anyString(),
            anyString(),
            anyMapOf(String.class, String.class),
            eq(FeatureVariable.VariableType.DOUBLE)
        );

        assertNull(spyOptimizely.getFeatureVariableDouble(
            featureKey,
            variableKey,
            genericUserId
        ));

        logbackVerifier.expectMessage(
            Level.ERROR,
            "NumberFormatException while trying to parse \"" + unParsableValue +
                "\" as Double. "
        );
    }

    /**
     * Verify {@link Optimizely#getFeatureVariableInteger(String, String, String)}
     * calls through to {@link Optimizely#getFeatureVariableInteger(String, String, String, Map)}
     * and returns null
     * when called with a null value for the feature Key parameter.
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableIntegerReturnsNullWhenFeatureKeyIsNull() throws ConfigParseException {
        String variableKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

        assertNull(spyOptimizely.getFeatureVariableInteger(
            null,
            variableKey,
            genericUserId
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The featureKey parameter must be nonnull."
        );
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
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableIntegerReturnsNullWhenVariableKeyIsNull() throws ConfigParseException {
        String featureKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

        assertNull(spyOptimizely.getFeatureVariableInteger(
            featureKey,
            null,
            genericUserId
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The variableKey parameter must be nonnull."
        );
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
     *
     * @throws ConfigParseException
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getFeatureVariableIntegerReturnsNullWhenUserIdIsNull() throws ConfigParseException {
        String featureKey = "";
        String variableKey = "";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

        assertNull(spyOptimizely.getFeatureVariableInteger(
            featureKey,
            variableKey,
            null
        ));

        logbackVerifier.expectMessage(
            Level.WARN,
            "The userId parameter must be nonnull."
        );
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
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableIntegerReturnsWhatInternalReturns() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        Integer valueNoAttributes = 1;
        Integer valueWithAttributes = 2;
        Map<String, String> attributes = Collections.singletonMap("key", "value");

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());


        doReturn(valueNoAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
            eq(featureKey),
            eq(variableKey),
            eq(genericUserId),
            eq(Collections.<String, String>emptyMap()),
            eq(FeatureVariable.VariableType.INTEGER)
        );

        doReturn(valueWithAttributes.toString()).when(spyOptimizely).getFeatureVariableValueForType(
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
     *
     * @throws ConfigParseException
     */
    @Test
    public void getFeatureVariableIntegerCatchesExceptionFromParsing() throws ConfigParseException {
        String featureKey = "featureKey";
        String variableKey = "variableKey";
        String unParsableValue = "not_an_integer";

        Optimizely spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler)
            .withConfig(validProjectConfig)
            .build());

        doReturn(unParsableValue).when(spyOptimizely).getFeatureVariableValueForType(
            anyString(),
            anyString(),
            anyString(),
            anyMapOf(String.class, String.class),
            eq(FeatureVariable.VariableType.INTEGER)
        );

        assertNull(spyOptimizely.getFeatureVariableInteger(
            featureKey,
            variableKey,
            genericUserId
        ));

        logbackVerifier.expectMessage(
            Level.ERROR,
            "NumberFormatException while trying to parse \"" + unParsableValue +
                "\" as Integer. "
        );
    }

    /**
     * Verify that {@link Optimizely#getVariation(String, String)} returns a variation when given an experiment
     * with no audiences and no user attributes and verify that listener is getting called.
     */
    @Test
    public void getVariationBucketingIdAttribute() throws Exception {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = experiment.getVariations().get(0);
        String bucketingKey = testBucketingIdKey;
        String bucketingId = "blah";
        String userId = testUserId;
        Map<String, String> testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");
        testUserAttributes.put(bucketingKey, bucketingId);


        when(mockBucketer.bucket(experiment, bucketingId)).thenReturn(bucketedVariation);

        Optimizely optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler)
            .withConfig(noAudienceProjectConfig)
            .withBucketing(mockBucketer)
            .withErrorHandler(mockErrorHandler)
            .build();

        Variation actualVariation = optimizely.getVariation(experiment.getKey(), userId, testUserAttributes);

        verify(mockBucketer).bucket(experiment, bucketingId);

        assertThat(actualVariation, is(bucketedVariation));
    }

    //======== isValid calls  ========//

    /**
     * Verify that {@link Optimizely#isValid()} returns false when the Optimizely instance is not valid
     */
    @Test
    public void isValidReturnsFalseWhenClientIsInvalid() throws Exception {
        Optimizely optimizely = Optimizely.builder(invalidProjectConfigV5(), mockEventHandler)
            .withBucketing(mockBucketer)
            .build();

        assertFalse(optimizely.isValid());
    }

    /**
     * Verify that {@link Optimizely#isValid()} returns false when the Optimizely instance is not valid
     */
    @Test
    public void isValidReturnsTrueWhenClientIsValid() throws Exception {
        Optimizely optimizely = Optimizely.builder(validDatafile, mockEventHandler)
            .withBucketing(mockBucketer)
            .build();

        assertTrue(optimizely.isValid());
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
}
