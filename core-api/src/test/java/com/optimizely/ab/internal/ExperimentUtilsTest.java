/**
 *
 *    Copyright 2017, 2019-2020, Optimizely and contributors
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
package com.optimizely.ab.internal;

import ch.qos.logback.classic.Level;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Experiment.ExperimentStatus;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.TypedAudience;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.noAudienceProjectConfigV2;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV4;
import static com.optimizely.ab.config.ValidProjectConfigV4.ATTRIBUTE_NATIONALITY_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_WITH_MISSING_VALUE_VALUE;
import static com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY;
import static com.optimizely.ab.internal.ExperimentUtils.isExperimentActive;
import static com.optimizely.ab.internal.ExperimentUtils.doesUserMeetAudienceConditions;
import static com.optimizely.ab.internal.LoggingConstants.AudienceFor.EXPERIMENT;
import static com.optimizely.ab.internal.LoggingConstants.AudienceFor.RULE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the Experiment Utils methods.
 */
public class ExperimentUtilsTest {

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    private static ProjectConfig projectConfig;
    private static ProjectConfig noAudienceProjectConfig;
    private static ProjectConfig v4ProjectConfig;

    @BeforeClass
    public static void setUp() throws IOException {
        projectConfig = validProjectConfigV2();
        noAudienceProjectConfig = noAudienceProjectConfigV2();
        v4ProjectConfig = validProjectConfigV4();
    }

    /**
     * If the {@link Experiment#status} is {@link ExperimentStatus#RUNNING},
     * then {@link ExperimentUtils#isExperimentActive(Experiment)} should return true.
     */
    @Test
    public void isExperimentActiveReturnsTrueWhenTheExperimentIsRunning() {
        Experiment mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.RUNNING);

        assertTrue(isExperimentActive(mockExperiment));
    }

    /**
     * If the {@link Experiment#status} is {@link ExperimentStatus#LAUNCHED},
     * then {@link ExperimentUtils#isExperimentActive(Experiment)} should return true.
     */
    @Test
    public void isExperimentActiveReturnsTrueWhenTheExperimentIsLaunched() {
        Experiment mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.LAUNCHED);

        assertTrue(isExperimentActive(mockExperiment));
    }

    /**
     * If the {@link Experiment#status} is {@link ExperimentStatus#PAUSED},
     * then {@link ExperimentUtils#isExperimentActive(Experiment)} should return false.
     */
    @Test
    public void isExperimentActiveReturnsFalseWhenTheExperimentIsPaused() {
        Experiment mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.PAUSED);

        assertFalse(isExperimentActive(mockExperiment));
    }

    /**
     * If the {@link Experiment#status} is {@link ExperimentStatus#ARCHIVED},
     * then {@link ExperimentUtils#isExperimentActive(Experiment)} should return false.
     */
    @Test
    public void isExperimentActiveReturnsFalseWhenTheExperimentIsArchived() {
        Experiment mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.ARCHIVED);

        assertFalse(isExperimentActive(mockExperiment));
    }

    /**
     * If the {@link Experiment#status} is {@link ExperimentStatus#NOT_STARTED},
     * then {@link ExperimentUtils#isExperimentActive(Experiment)} should return false.
     */
    @Test
    public void isExperimentActiveReturnsFalseWhenTheExperimentIsNotStarted() {
        Experiment mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.NOT_STARTED);

        assertFalse(isExperimentActive(mockExperiment));
    }

    /**
     * If the {@link Experiment} does not have any {@link Audience}s,
     * then {@link ExperimentUtils#doesUserMeetAudienceConditions(ProjectConfig, Experiment, Map, String, String)} should return true;
     */
    @Test
    public void doesUserMeetAudienceConditionsReturnsTrueIfExperimentHasNoAudiences() {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);
        assertTrue(doesUserMeetAudienceConditions(noAudienceProjectConfig, experiment, Collections.<String, String>emptyMap(), RULE, "Everyone Else"));
    }

    /**
     * If the {@link Experiment} contains at least one {@link Audience}, but attributes is empty,
     * then {@link ExperimentUtils#doesUserMeetAudienceConditions(ProjectConfig, Experiment, Map, String, String)} should return false.
     */
    @Test
    public void doesUserMeetAudienceConditionsEvaluatesEvenIfExperimentHasAudiencesButUserHasNoAttributes() {
        Experiment experiment = projectConfig.getExperiments().get(0);
        Boolean result = doesUserMeetAudienceConditions(projectConfig, experiment, Collections.<String, String>emptyMap(), EXPERIMENT, experiment.getKey());
        assertTrue(result);
        logbackVerifier.expectMessage(Level.DEBUG,
            "Evaluating audiences for experiment \"etag1\": \"[100]\"");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Starting to evaluate audience \"100\" with conditions: [and, [or, [not, [or, {name='browser_type', type='custom_attribute', match='null', value='firefox'}]]]]");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Audience 100 evaluated to true");
        logbackVerifier.expectMessage(Level.INFO,
            "Audiences for experiment \"etag1\" collectively evaluated to true");
    }

    /**
     * If the {@link Experiment} contains at least one {@link Audience}, but attributes is empty,
     * then {@link ExperimentUtils#doesUserMeetAudienceConditions(ProjectConfig, Experiment, Map, String, String)} should return false.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void doesUserMeetAudienceConditionsEvaluatesEvenIfExperimentHasAudiencesButUserSendNullAttributes() throws Exception {
        Experiment experiment = projectConfig.getExperiments().get(0);
        Boolean result = doesUserMeetAudienceConditions(projectConfig, experiment, null, EXPERIMENT, experiment.getKey());

        assertTrue(result);
        logbackVerifier.expectMessage(Level.DEBUG,
            "Evaluating audiences for experiment \"etag1\": \"[100]\"");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Starting to evaluate audience \"100\" with conditions: [and, [or, [not, [or, {name='browser_type', type='custom_attribute', match='null', value='firefox'}]]]]");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Audience 100 evaluated to true");
        logbackVerifier.expectMessage(Level.INFO,
            "Audiences for experiment \"etag1\" collectively evaluated to true");
    }

    /**
     * If the {@link Experiment} contains {@link TypedAudience}, and attributes is valid and true,
     * then {@link ExperimentUtils#doesUserMeetAudienceConditions(ProjectConfig, Experiment, Map, String, String)} should return true.
     */
    @Test
    public void doesUserMeetAudienceConditionsEvaluatesExperimentHasTypedAudiences() {
        Experiment experiment = v4ProjectConfig.getExperiments().get(1);
        Map<String, Boolean> attribute = Collections.singletonMap("booleanKey", true);
        Boolean result = doesUserMeetAudienceConditions(v4ProjectConfig, experiment, attribute, EXPERIMENT, experiment.getKey());

        assertTrue(result);
        logbackVerifier.expectMessage(Level.DEBUG,
            "Evaluating audiences for experiment \"typed_audience_experiment\": \"[or, 3468206643, 3468206644, 3468206646, 3468206645]\"");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Starting to evaluate audience \"3468206643\" with conditions: [and, [or, [or, {name='booleanKey', type='custom_attribute', match='exact', value=true}]]]");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Audience 3468206643 evaluated to true");
        logbackVerifier.expectMessage(Level.INFO,
            "Audiences for experiment \"typed_audience_experiment\" collectively evaluated to true");
    }

    /**
     * If the attributes satisfies at least one {@link Condition} in an {@link Audience} of the {@link Experiment},
     * then {@link ExperimentUtils#doesUserMeetAudienceConditions(ProjectConfig, Experiment, Map, String, String)} should return true.
     */
    @Test
    public void doesUserMeetAudienceConditionsReturnsTrueIfUserSatisfiesAnAudience() {
        Experiment experiment = projectConfig.getExperiments().get(0);
        Map<String, String> attributes = Collections.singletonMap("browser_type", "chrome");
        Boolean result = doesUserMeetAudienceConditions(projectConfig, experiment, attributes, EXPERIMENT, experiment.getKey());

        assertTrue(result);
        logbackVerifier.expectMessage(Level.DEBUG,
            "Evaluating audiences for experiment \"etag1\": \"[100]\"");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Starting to evaluate audience \"100\" with conditions: [and, [or, [not, [or, {name='browser_type', type='custom_attribute', match='null', value='firefox'}]]]]");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Audience 100 evaluated to true");
        logbackVerifier.expectMessage(Level.INFO,
            "Audiences for experiment \"etag1\" collectively evaluated to true");
    }

    /**
     * If the attributes satisfies no {@link Condition} of any {@link Audience} of the {@link Experiment},
     * then {@link ExperimentUtils#doesUserMeetAudienceConditions(ProjectConfig, Experiment, Map, String, String)} should return false.
     */
    @Test
    public void doesUserMeetAudienceConditionsReturnsTrueIfUserDoesNotSatisfyAnyAudiences() {
        Experiment experiment = projectConfig.getExperiments().get(0);
        Map<String, String> attributes = Collections.singletonMap("browser_type", "firefox");
        Boolean result = doesUserMeetAudienceConditions(projectConfig, experiment, attributes, EXPERIMENT, experiment.getKey());

        assertFalse(result);
        logbackVerifier.expectMessage(Level.DEBUG,
            "Evaluating audiences for experiment \"etag1\": \"[100]\"");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Starting to evaluate audience \"100\" with conditions: [and, [or, [not, [or, {name='browser_type', type='custom_attribute', match='null', value='firefox'}]]]]");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Audience 100 evaluated to false");
        logbackVerifier.expectMessage(Level.INFO,
            "Audiences for experiment \"etag1\" collectively evaluated to false");

    }

    /**
     * If there are audiences with attributes on the experiment, but one of the attribute values is null,
     * they must explicitly pass in null in order for us to evaluate this. Otherwise we will say they do not match.
     */
    @Test
    public void doesUserMeetAudienceConditionsHandlesNullValue() {
        Experiment experiment = v4ProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY);
        Map<String, String> satisfiesFirstCondition = Collections.singletonMap(ATTRIBUTE_NATIONALITY_KEY,
            AUDIENCE_WITH_MISSING_VALUE_VALUE);
        Map<String, String> nonMatchingMap = Collections.singletonMap(ATTRIBUTE_NATIONALITY_KEY, "American");

        assertTrue(doesUserMeetAudienceConditions(v4ProjectConfig, experiment, satisfiesFirstCondition, EXPERIMENT, experiment.getKey()));
        assertFalse(doesUserMeetAudienceConditions(v4ProjectConfig, experiment, nonMatchingMap, EXPERIMENT, experiment.getKey()));
    }

    /**
     * Audience will evaluate null when condition value is null and attribute value passed is also null
     */
    @Test
    public void doesUserMeetAudienceConditionsHandlesNullValueAttributesWithNull() {
        Experiment experiment = v4ProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY);
        Map<String, String> attributesWithNull = Collections.singletonMap(ATTRIBUTE_NATIONALITY_KEY, null);

        assertFalse(doesUserMeetAudienceConditions(v4ProjectConfig, experiment, attributesWithNull, EXPERIMENT, experiment.getKey()));

        logbackVerifier.expectMessage(Level.DEBUG,
            "Starting to evaluate audience \"2196265320\" with conditions: [and, [or, [or, {name='nationality', type='custom_attribute', match='null', value='English'}, {name='nationality', type='custom_attribute', match='null', value=null}]]]");
        logbackVerifier.expectMessage(Level.WARN,
            "Audience condition \"{name='nationality', type='custom_attribute', match='null', value=null}\" has an unsupported condition value. You may need to upgrade to a newer release of the Optimizely SDK.");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Audience 2196265320 evaluated to null");
        logbackVerifier.expectMessage(Level.INFO,
            "Audiences for experiment \"experiment_with_malformed_audience\" collectively evaluated to null");
    }

    /**
     * Audience will evaluate null when condition value is null
     */
    @Test
    public void doesUserMeetAudienceConditionsHandlesNullConditionValue() {
        Experiment experiment = v4ProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY);
        Map<String, String> attributesEmpty = Collections.emptyMap();

        // It should explicitly be set to null otherwise we will return false on empty maps
        assertFalse(doesUserMeetAudienceConditions(v4ProjectConfig, experiment, attributesEmpty, EXPERIMENT, experiment.getKey()));

        logbackVerifier.expectMessage(Level.DEBUG,
            "Starting to evaluate audience \"2196265320\" with conditions: [and, [or, [or, {name='nationality', type='custom_attribute', match='null', value='English'}, {name='nationality', type='custom_attribute', match='null', value=null}]]]");
        logbackVerifier.expectMessage(Level.WARN,
            "Audience condition \"{name='nationality', type='custom_attribute', match='null', value=null}\" has an unsupported condition value. You may need to upgrade to a newer release of the Optimizely SDK.");
        logbackVerifier.expectMessage(Level.DEBUG,
            "Audience 2196265320 evaluated to null");
        logbackVerifier.expectMessage(Level.INFO,
            "Audiences for experiment \"experiment_with_malformed_audience\" collectively evaluated to null");
    }

    /**
     * Helper method to create an {@link Experiment} object with the provided status.
     *
     * @param status What the desired {@link Experiment#status} should be.
     * @return The newly created {@link Experiment}.
     */
    private Experiment makeMockExperimentWithStatus(ExperimentStatus status) {
        return new Experiment("12345",
            "mockExperimentKey",
            status.toString(),
            "layerId",
            Collections.<String>emptyList(),
            null,
            Collections.<Variation>emptyList(),
            Collections.<String, String>emptyMap(),
            Collections.<TrafficAllocation>emptyList()
        );
    }
}
