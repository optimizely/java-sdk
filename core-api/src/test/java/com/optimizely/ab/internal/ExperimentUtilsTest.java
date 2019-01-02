/**
 *
 *    Copyright 2017,2019, Optimizely and contributors
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

import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4;
import static com.optimizely.ab.config.ValidProjectConfigV4.ATTRIBUTE_NATIONALITY_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_WITH_MISSING_VALUE_VALUE;
import static com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY;
import static com.optimizely.ab.internal.ExperimentUtils.isExperimentActive;
import static com.optimizely.ab.internal.ExperimentUtils.isUserInExperiment;
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
     * then {@link ExperimentUtils#isUserInExperiment(ProjectConfig, Experiment, Map)} should return true;
     */
    @Test
    public void isUserInExperimentReturnsTrueIfExperimentHasNoAudiences() {
        Experiment experiment = noAudienceProjectConfig.getExperiments().get(0);

        assertTrue(isUserInExperiment(noAudienceProjectConfig, experiment, Collections.<String, String>emptyMap()));
        logbackVerifier.expectMessage(Level.DEBUG, String.format("There is no Audience associated with experiment %s", experiment.getKey()));
    }

    /**
     * If the {@link Experiment} contains at least one {@link Audience}, but attributes is empty,
     * then {@link ExperimentUtils#isUserInExperiment(ProjectConfig, Experiment, Map)} should return false.
     */
    @Test
    public void isUserInExperimentEvaluatesEvenIfExperimentHasAudiencesButUserHasNoAttributes() {
        Experiment experiment = projectConfig.getExperiments().get(0);
        Audience audience = projectConfig.getAudience(experiment.getAudienceIds().get(0));
        Boolean result = isUserInExperiment(projectConfig, experiment, Collections.<String, String>emptyMap());
        assertTrue(result);
        logbackVerifier.expectMessage(Level.DEBUG, String.format("Starting to evaluate audience %s with conditions: \"%s\"", audience.getName(), audience.getConditions()));
        logbackVerifier.expectMessage(Level.DEBUG, String.format("User attributes: {}"));
        logbackVerifier.expectMessage(Level.ERROR, String.format("Cannot evaluate targeting condition since the value for attribute is an incompatible type"));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audience %s evaluated as %s", audience.getName(), result));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audiences for experiment %s collectively evaluated as %s", experiment.getKey(), result));
    }

    /**
     * If the {@link Experiment} contains at least one {@link Audience}, but attributes is empty,
     * then {@link ExperimentUtils#isUserInExperiment(ProjectConfig, Experiment, Map)} should return false.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void isUserInExperimentEvaluatesEvenIfExperimentHasAudiencesButUserSendNullAttributes() throws Exception {
        Experiment experiment = projectConfig.getExperiments().get(0);
        Audience audience = projectConfig.getAudience(experiment.getAudienceIds().get(0));
        Boolean result = isUserInExperiment(projectConfig, experiment, null);
        assertTrue(result);
        logbackVerifier.expectMessage(Level.DEBUG, String.format("Starting to evaluate audience %s with conditions: \"%s\"", audience.getName(), audience.getConditions()));
        logbackVerifier.expectMessage(Level.DEBUG, String.format("User attributes: null"));
        logbackVerifier.expectMessage(Level.ERROR, String.format("Cannot evaluate targeting condition since the value for attribute is an incompatible type"));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audience %s evaluated as %s", audience.getName(), result));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audiences for experiment %s collectively evaluated as %s", experiment.getKey(), result));
    }
    /**
     * If the {@link Experiment} contains {@link TypedAudience}, and attributes is valid and true,
     * then {@link ExperimentUtils#isUserInExperiment(ProjectConfig, Experiment, Map)} should return true.
     */
    @Test
    public void isUserInExperimentEvaluatesExperimentHasTypedAudiences() {
        Experiment experiment = v4ProjectConfig.getExperiments().get(1);
        Audience audience = v4ProjectConfig.getAudience(experiment.getAudienceIds().get(0));
        Map<String, Boolean> attribute = Collections.singletonMap("booleanKey", true);
        Boolean result = isUserInExperiment(v4ProjectConfig, experiment, attribute);
        assertTrue(result);
        logbackVerifier.expectMessage(Level.DEBUG, String.format("Evaluating audiences for experiment \"%s\": \"%s\"", experiment.getKey(), experiment.getAudienceConditions().toString()));
        logbackVerifier.expectMessage(Level.DEBUG, String.format("Starting to evaluate audience %s with conditions: \"%s\"", audience.getName(), audience.getConditions()));
        logbackVerifier.expectMessage(Level.DEBUG, String.format("User attributes: %s", attribute.toString()));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audience %s evaluated as %s", audience.getName(), result));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audiences for experiment %s collectively evaluated as %s", experiment.getKey(), result));
    }

    /**
     * If the attributes satisfies at least one {@link Condition} in an {@link Audience} of the {@link Experiment},
     * then {@link ExperimentUtils#isUserInExperiment(ProjectConfig, Experiment, Map)} should return true.
     */
    @Test
    public void isUserInExperimentReturnsTrueIfUserSatisfiesAnAudience() {
        Experiment experiment = projectConfig.getExperiments().get(0);
        Audience audience = projectConfig.getAudience(experiment.getAudienceIds().get(0));
        Map<String, String> attributes = Collections.singletonMap("browser_type", "chrome");
        Boolean result = isUserInExperiment(projectConfig, experiment, attributes);
        assertTrue(result);
        logbackVerifier.expectMessage(Level.DEBUG, String.format("Starting to evaluate audience %s with conditions: \"%s\"", audience.getName(), audience.getConditions()));
        logbackVerifier.expectMessage(Level.DEBUG, String.format("User attributes: %s", attributes.toString()));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audience %s evaluated as %s", audience.getName(), result));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audiences for experiment %s collectively evaluated as %s", experiment.getKey(), result));
    }

    /**
     * If the attributes satisfies no {@link Condition} of any {@link Audience} of the {@link Experiment},
     * then {@link ExperimentUtils#isUserInExperiment(ProjectConfig, Experiment, Map)} should return false.
     */
    @Test
    public void isUserInExperimentReturnsTrueIfUserDoesNotSatisfyAnyAudiences() {
        Experiment experiment = projectConfig.getExperiments().get(0);
        Audience audience = projectConfig.getAudience(experiment.getAudienceIds().get(0));
        Map<String, String> attributes = Collections.singletonMap("browser_type", "firefox");
        Boolean result = isUserInExperiment(projectConfig, experiment, attributes);
        assertFalse(result);

        logbackVerifier.expectMessage(Level.DEBUG, String.format("Starting to evaluate audience %s with conditions: \"%s\"", audience.getName(), audience.getConditions()));
        logbackVerifier.expectMessage(Level.DEBUG, String.format("User attributes: %s", attributes.toString()));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audience %s evaluated as %s", audience.getName(), result));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audiences for experiment %s collectively evaluated as %s", experiment.getKey(), result));

    }

    /**
     * If there are audiences with attributes on the experiment, but one of the attribute values is null,
     * they must explicitly pass in null in order for us to evaluate this. Otherwise we will say they do not match.
     */
    @Test
    public void isUserInExperimentHandlesNullValue() {
        Experiment experiment = v4ProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY);
        Map<String, String> satisfiesFirstCondition = Collections.singletonMap(ATTRIBUTE_NATIONALITY_KEY,
                AUDIENCE_WITH_MISSING_VALUE_VALUE);
        Map<String, String> nonMatchingMap = Collections.singletonMap(ATTRIBUTE_NATIONALITY_KEY, "American");

        assertTrue(isUserInExperiment(v4ProjectConfig, experiment, satisfiesFirstCondition));
        assertFalse(isUserInExperiment(v4ProjectConfig, experiment, nonMatchingMap));
    }

    /**
     * Audience will evaluate null when condition value is null and attribute value passed is also null
     */
    @Test
    public void isUserInExperimentHandlesNullValueAttributesWithNull() {
        Experiment experiment = v4ProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY);
        Audience audience = v4ProjectConfig.getAudience(experiment.getAudienceIds().get(0));
        Map<String, String> attributesWithNull = Collections.singletonMap(ATTRIBUTE_NATIONALITY_KEY, null);
        assertFalse(isUserInExperiment(v4ProjectConfig, experiment, attributesWithNull));

        logbackVerifier.expectMessage(Level.DEBUG, String.format("Starting to evaluate audience %s with conditions: \"%s\"", audience.getName(), audience.getConditions()));
        logbackVerifier.expectMessage(Level.DEBUG, String.format("User attributes: %s", attributesWithNull.toString()));
        logbackVerifier.expectMessage(Level.ERROR, String.format("Cannot evaluate targeting condition since the value for attribute is an incompatible type"));
        logbackVerifier.expectMessage(Level.WARN, String.format("Audience condition \"{name='nationality', type='custom_attribute', match='null', value=null}\" condition value \"null\" data type is inapplicable"));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audience %s evaluated as null", audience.getName()));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audiences for experiment %s collectively evaluated as null", experiment.getKey()));
    }

    /**
     * Audience will evaluate null when condition value is null
     */
    @Test
    public void isUserInExperimentHandlesNullConditionValue() {
        Experiment experiment = v4ProjectConfig.getExperimentKeyMapping().get(EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY);
        Audience audience = v4ProjectConfig.getAudience(experiment.getAudienceIds().get(0));
        Map<String, String> attributesEmpty = Collections.emptyMap();

        // It should explicitly be set to null otherwise we will return false on empty maps
        assertFalse(isUserInExperiment(v4ProjectConfig, experiment, attributesEmpty));

        logbackVerifier.expectMessage(Level.DEBUG, String.format("Starting to evaluate audience %s with conditions: \"%s\"", audience.getName(), audience.getConditions()));
        logbackVerifier.expectMessage(Level.DEBUG, String.format("User attributes: %s", attributesEmpty.toString()));
        logbackVerifier.expectMessage(Level.ERROR, String.format("Cannot evaluate targeting condition since the value for attribute is an incompatible type"));
        logbackVerifier.expectMessage(Level.WARN, String.format("Audience condition \"{name='nationality', type='custom_attribute', match='null', value=null}\" condition value \"null\" data type is inapplicable"));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audience %s evaluated as null", audience.getName()));
        logbackVerifier.expectMessage(Level.INFO, String.format("Audiences for experiment %s collectively evaluated as null", experiment.getKey()));
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
