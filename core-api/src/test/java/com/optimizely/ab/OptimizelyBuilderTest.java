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
package com.optimizely.ab;

import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.*;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.event.EventHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link Optimizely#builder(String, EventHandler)}.
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class OptimizelyBuilderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private EventHandler mockEventHandler;

    @Mock
    private ErrorHandler mockErrorHandler;

    @Mock
    ProjectConfigManager mockProjectConfigManager;

    @Test
    public void withEventHandler() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .build();

        assertThat(optimizelyClient.eventHandler, is(mockEventHandler));
    }

    @Test
    public void projectConfigV2() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .build();

        DatafileProjectConfigTestUtils.verifyProjectConfig(optimizelyClient.getProjectConfig(), validProjectConfigV2());
    }

    @Test
    public void projectConfigV3() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV3(), mockEventHandler)
            .build();

        DatafileProjectConfigTestUtils.verifyProjectConfig(optimizelyClient.getProjectConfig(), validProjectConfigV3());
    }

    @Test
    public void withErrorHandler() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .withErrorHandler(mockErrorHandler)
            .build();

        assertThat(optimizelyClient.errorHandler, is(mockErrorHandler));
    }

    @Test
    public void withDefaultErrorHandler() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .build();

        assertThat(optimizelyClient.errorHandler, instanceOf(NoOpErrorHandler.class));
    }

    @Test
    public void withUserProfileService() throws Exception {
        UserProfileService userProfileService = mock(UserProfileService.class);
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .withUserProfileService(userProfileService)
            .build();

        assertThat(optimizelyClient.getUserProfileService(), is(userProfileService));
    }

    @SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "Testing nullness contract violation")
    @Test
    public void nullDatafileResultsInInvalidOptimizelyInstance() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder((String) null, mockEventHandler).build();

        assertFalse(optimizelyClient.isValid());
    }

    @Test
    public void emptyDatafileResultsInInvalidOptimizelyInstance() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder("", mockEventHandler).build();

        assertFalse(optimizelyClient.isValid());
    }

    @Test
    public void invalidDatafileResultsInInvalidOptimizelyInstance() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder("{invalidDatafile}", mockEventHandler).build();

        assertFalse(optimizelyClient.isValid());
    }

    @Test
    public void unsupportedDatafileResultsInInvalidOptimizelyInstance() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(invalidProjectConfigV5(), mockEventHandler)
            .build();

        assertFalse(optimizelyClient.isValid());
    }

    @Test
    public void withValidProjectConfigManagerOnly() throws Exception {
        ProjectConfig projectConfig = new DatafileProjectConfig.Builder().withDatafile(validConfigJsonV4()).build();
        when(mockProjectConfigManager.getConfig()).thenReturn(projectConfig);

        Optimizely optimizelyClient = Optimizely.builder()
            .withConfigManager(mockProjectConfigManager)
            .withEventHandler(mockEventHandler)
            .build();

        assertTrue(optimizelyClient.isValid());
        verifyProjectConfig(optimizelyClient.getProjectConfig(), projectConfig);
    }

    @Test
    public void withInvalidProjectConfigManagerOnly() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder()
            .withConfigManager(mockProjectConfigManager)
            .withEventHandler(mockEventHandler)
            .build();
        assertFalse(optimizelyClient.isValid());
    }

    @Test
    public void withProjectConfigManagerAndFallbackDatafile() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV4(), mockEventHandler)
            .withConfigManager(new AtomicProjectConfigManager())
            .build();

        // Project Config manager takes precedence.
        assertFalse(optimizelyClient.isValid());
    }
}
