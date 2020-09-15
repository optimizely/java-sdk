/**
 *
 *    Copyright 2020, Optimizely and contributors
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
package com.optimizely.ab.config.audience.match;

import org.junit.Test;

import static com.optimizely.ab.config.audience.match.MatchRegistry.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;

public class MatchRegistryTest {

    @Test
    public void testDefaultMatchers() throws UnknownMatchTypeException {
        assertThat(MatchRegistry.getMatch(EXACT), instanceOf(ExactMatch.class));
        assertThat(MatchRegistry.getMatch(EXISTS), instanceOf(ExistsMatch.class));
        assertThat(MatchRegistry.getMatch(GREATER_THAN), instanceOf(GTMatch.class));
        assertThat(MatchRegistry.getMatch(LESS_THAN), instanceOf(LTMatch.class));
        assertThat(MatchRegistry.getMatch(GREATER_THAN_EQ), instanceOf(GEMatch.class));
        assertThat(MatchRegistry.getMatch(LESS_THAN_EQ), instanceOf(LEMatch.class));
        assertThat(MatchRegistry.getMatch(LEGACY), instanceOf(DefaultMatchForLegacyAttributes.class));
        assertThat(MatchRegistry.getMatch(SEMVER_EQ), instanceOf(SemanticVersionEqualsMatch.class));
        assertThat(MatchRegistry.getMatch(SEMVER_GE), instanceOf(SemanticVersionGEMatch.class));
        assertThat(MatchRegistry.getMatch(SEMVER_GT), instanceOf(SemanticVersionGTMatch.class));
        assertThat(MatchRegistry.getMatch(SEMVER_LE), instanceOf(SemanticVersionLEMatch.class));
        assertThat(MatchRegistry.getMatch(SEMVER_LT), instanceOf(SemanticVersionLTMatch.class));
        assertThat(MatchRegistry.getMatch(SUBSTRING), instanceOf(SubstringMatch.class));
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
