package com.optimizely.ab.config;

import com.optimizely.ab.config.parser.ConfigParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.optimizely.ab.config.ProjectConfigTestUtils.invalidProjectConfigV5;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link com.optimizely.ab.config.ProjectConfig.Builder}.
 */
public class ProjectConfigBuilderTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void withNullDatafile() throws Exception {
        thrown.expect(ConfigParseException.class);
        new ProjectConfig.Builder()
                .withDatafile(null)
                .build();
    }

    @Test
    public void withEmptyDatafile() throws Exception {
        thrown.expect(ConfigParseException.class);
        new ProjectConfig.Builder()
                .withDatafile("")
                .build();
    }

    @Test
    public void withValidDatafile() throws Exception {
        ProjectConfig projectConfig = new ProjectConfig.Builder()
                .withDatafile(validConfigJsonV4())
                .build();
        assertNotNull(projectConfig);
        assertEquals("4", projectConfig.getVersion());
    }

    @Test
    public void withInvalidNewerDatafile() throws Exception {
        thrown.expect(ConfigParseException.class);
        new ProjectConfig.Builder()
                .withDatafile(invalidProjectConfigV5())
                .build();
    }
}
