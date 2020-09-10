package com.optimizely.ab.config.audience.match;

import org.junit.Test;

import static com.optimizely.ab.config.audience.match.MatchRegistry.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;

public class MatchRegistryTest {

    @Test
    public void testDefaultMatchers() throws UnknownMatchTypeException {
        assertThat(MatchRegistry.getMatch(EXISTS), instanceOf(ExistsMatch.class));
        assertThat(MatchRegistry.getMatch(EXACT), instanceOf(ExactMatch.class));
        assertThat(MatchRegistry.getMatch(GREATER_THAN), instanceOf(GTMatch.class));
        assertThat(MatchRegistry.getMatch(LESS_THAN), instanceOf(LTMatch.class));
        assertThat(MatchRegistry.getMatch(LESS_THAN_EQ), instanceOf(LEMatch.class));
        assertThat(MatchRegistry.getMatch(GREATER_THAN_EQ), instanceOf(GEMatch.class));
        assertThat(MatchRegistry.getMatch(SUBSTRING), instanceOf(SubstringMatch.class));
        assertThat(MatchRegistry.getMatch(SEMVER_EQ), instanceOf(SemanticVersionEqualsMatch.class));
        assertThat(MatchRegistry.getMatch(SEMVER_GE), instanceOf(SemanticVersionGEMatch.class));
        assertThat(MatchRegistry.getMatch(SEMVER_GT), instanceOf(SemanticVersionGTMatch.class));
        assertThat(MatchRegistry.getMatch(SEMVER_LE), instanceOf(SemanticVersionLEMatch.class));
        assertThat(MatchRegistry.getMatch(SEMVER_LT), instanceOf(SemanticVersionLTMatch.class));
    }

    @Test(expected = UnknownMatchTypeException.class)
    public void testUnknownMatcher() throws UnknownMatchTypeException {
        MatchRegistry.getMatch("UNKNOWN");
    }

    @Test
    public void testRegister() throws UnknownMatchTypeException {
        class TestMatcher implements Match {
            @Override
            public Boolean eval(Object conditionValue, Object attributeValue) {
                return null;
            }
        }

        MatchRegistry.register("test-matcher", new TestMatcher());
        assertThat(MatchRegistry.getMatch("test-matcher"), instanceOf(TestMatcher.class));
    }
}
