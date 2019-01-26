/**
 *
 *    Copyright 2019, Optimizely and contributors
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
package com.optimizely.ab.config.audience;

import com.optimizely.ab.config.ProjectConfig;
import org.junit.Test;

import java.nio.CharBuffer;
import java.util.Map;

import static com.optimizely.ab.config.audience.AudienceTestUtils.attributes;
import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class UserAttributeTest {
    @Test
    public void testExistsMatch() {
        Map<String, ?> attributes = attributes().add("city", "sf").get();
        UserAttribute cityExists = AudienceTestUtils.matchAny("city");
        UserAttribute stateExists = AudienceTestUtils.matchAny("state");
        assertThat(cityExists.evaluate(projectConfig(), attributes), is(true));
        assertThat(stateExists.evaluate(projectConfig(), attributes), is(false));
        assertThat(stateExists.evaluate(projectConfig(), null), is(false));
    }

    @Test
    public void testExactMatch() {
        UserAttribute strMatch = AudienceTestUtils.matchExact("str", "abc");
        UserAttribute intMatch = AudienceTestUtils.matchExact("int", 123);
        UserAttribute doubleMatch = AudienceTestUtils.matchExact("double", 42.42);
        UserAttribute trueMatch = AudienceTestUtils.matchExact("true", true);
        UserAttribute falseMatch = AudienceTestUtils.matchExact("false", false);

        Map<String, ?> attributes = attributes()
            .add("str", "abc")
            .add("int", 123)
            .add("double", 42.42)
            .add("true", true)
            .add("false", false)
            .get();

        assertThat(strMatch.evaluate(projectConfig(), attributes), is(true));
        assertThat(intMatch.evaluate(projectConfig(), attributes), is(true));
        assertThat(doubleMatch.evaluate(projectConfig(), attributes), is(true));
        assertThat(trueMatch.evaluate(projectConfig(), attributes), is(true));
        assertThat(falseMatch.evaluate(projectConfig(), attributes), is(true));

        attributes = attributes().add("str", CharBuffer.wrap("abc")).get();
        assertThat(strMatch.evaluate(projectConfig(), attributes), is(true));
    }

    @Test
    public void testExactMatchNegative() {
        Map<String, ?> attributes = attributes()
            .add("str", "def")
            .add("int", 123.5)
            .add("double", 42)
            .add("true", false)
            .add("false", true)
            .get();

        UserAttribute strMatch = AudienceTestUtils.matchExact("str", "abc");
        UserAttribute intMatch = AudienceTestUtils.matchExact("int", 123);
        UserAttribute doubleMatch = AudienceTestUtils.matchExact("double", 42.42);
        UserAttribute trueMatch = AudienceTestUtils.matchExact("true", true);
        UserAttribute falseMatch = AudienceTestUtils.matchExact("false", false);
        assertThat(strMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(intMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(doubleMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(trueMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(falseMatch.evaluate(projectConfig(), attributes), is(false));

        UserAttribute strMatchCase = AudienceTestUtils.matchExact("str", "DEF");
        assertThat(strMatchCase.evaluate(projectConfig(), attributes), is(false));

        attributes = attributes()
            .add("str", "")
            .add("int", Double.POSITIVE_INFINITY)
            .add("double", Double.POSITIVE_INFINITY)
            .add("true", "true")
            .add("false", 0)
            .get();
        assertThat(strMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(intMatch.evaluate(projectConfig(), attributes), nullValue()); // infinity
        assertThat(doubleMatch.evaluate(projectConfig(), attributes), nullValue()); // infinity
        assertThat(trueMatch.evaluate(projectConfig(), attributes), nullValue()); // mismatching types
        assertThat(falseMatch.evaluate(projectConfig(), attributes), nullValue()); // mismatching types
    }

    @Test
    public void testExactMatchUnknown() {
        UserAttribute strMatch = AudienceTestUtils.matchExact("str", "abc");
        UserAttribute intMatch = AudienceTestUtils.matchExact("int", 123);
        UserAttribute doubleMatch = AudienceTestUtils.matchExact("double", 42.42);
        UserAttribute trueMatch = AudienceTestUtils.matchExact("true", true);
        UserAttribute falseMatch = AudienceTestUtils.matchExact("false", false);

        Map<String, ?> attributes = null;
        assertThat(strMatch.evaluate(projectConfig(), attributes), nullValue());
        assertThat(intMatch.evaluate(projectConfig(), attributes), nullValue());
        assertThat(doubleMatch.evaluate(projectConfig(), attributes), nullValue());
        assertThat(trueMatch.evaluate(projectConfig(), attributes), nullValue());
        assertThat(falseMatch.evaluate(projectConfig(), attributes), nullValue());

        attributes = emptyMap();
        assertThat(strMatch.evaluate(projectConfig(), attributes), nullValue());
        assertThat(intMatch.evaluate(projectConfig(), attributes), nullValue());
        assertThat(doubleMatch.evaluate(projectConfig(), attributes), nullValue());
        assertThat(trueMatch.evaluate(projectConfig(), attributes), nullValue());
        assertThat(falseMatch.evaluate(projectConfig(), attributes), nullValue());
    }

    @Test
    public void testSubstringMatch() {
        assertThat(AudienceTestUtils.matchSubstring("n", "")
            .evaluate(projectConfig(), null), nullValue());
        assertThat(AudienceTestUtils.matchSubstring("n", "")
            .evaluate(projectConfig(), emptyMap()), nullValue());
        assertThat(AudienceTestUtils.matchSubstring("n", null)
            .evaluate(projectConfig(), attributes().add("n", "v").get()), nullValue());

        UserAttribute a = AudienceTestUtils.matchSubstring("str", "a");
        UserAttribute b = AudienceTestUtils.matchSubstring("str", "b");
        UserAttribute c = AudienceTestUtils.matchSubstring("str", "c");
        UserAttribute ab = AudienceTestUtils.matchSubstring("str", "ab");
        UserAttribute bc = AudienceTestUtils.matchSubstring("str", "ab");
        UserAttribute ac = AudienceTestUtils.matchSubstring("str", "ac");
        UserAttribute abc = AudienceTestUtils.matchSubstring("str", "abc");
        UserAttribute abcd = AudienceTestUtils.matchSubstring("str", "abcd");

        Map<String, ?> attributes = attributes().add("str", "abc").get();
        assertThat(a.evaluate(projectConfig(), attributes), is(true));
        assertThat(b.evaluate(projectConfig(), attributes), is(true));
        assertThat(c.evaluate(projectConfig(), attributes), is(true));
        assertThat(ab.evaluate(projectConfig(), attributes), is(true));
        assertThat(bc.evaluate(projectConfig(), attributes), is(true));
        assertThat(abc.evaluate(projectConfig(), attributes), is(true));

        assertThat(ac.evaluate(projectConfig(), attributes), is(false));
        assertThat(abcd.evaluate(projectConfig(), attributes), is(false));
    }

    @Test
    public void testLessThanMatch() {
        assertThat(new UserAttribute("n", "custom_attribute", "lt", "")
            .evaluate(projectConfig(), null), nullValue());
        assertThat(new UserAttribute("n", "custom_attribute", "lt", "")
            .evaluate(projectConfig(), emptyMap()), nullValue());
        assertThat(AudienceTestUtils.matchLessThan("n", null)
            .evaluate(projectConfig(), attributes().add("n", 0).get()), nullValue());

        Map<String, ?> attributes = attributes()
            .add("-1", -1)
            .add("0", 0)
            .add("1", 1)
            .add("42.42f", 42.42f)
            .add("42.42d", 42.42d)
            .get();

        // int matching
        assertThat(AudienceTestUtils.matchLessThan("-1", 0)
            .evaluate(projectConfig(), attributes), is(true));
        assertThat(AudienceTestUtils.matchLessThan("1", 2)
            .evaluate(projectConfig(), attributes), is(true));
        assertThat(AudienceTestUtils.matchLessThan("1", 1)
            .evaluate(projectConfig(), attributes), is(false));

        // decimal input value
        assertThat(AudienceTestUtils.matchLessThan("1", 1.0 / 2.0)
            .evaluate(projectConfig(), attributes), is(false));
        assertThat(AudienceTestUtils.matchLessThan("1", 3.0 / 2.0)
            .evaluate(projectConfig(), attributes), is(true));

        // decimal match value
        assertThat(AudienceTestUtils.matchLessThan("42.42f", 50)
            .evaluate(projectConfig(), attributes), is(true));
        assertThat(AudienceTestUtils.matchLessThan("42.42f", 40)
            .evaluate(projectConfig(), attributes), is(false));

        // double matching
        assertThat(AudienceTestUtils.matchLessThan("42.42f", 42.423d)
            .evaluate(projectConfig(), attributes), is(true));
        assertThat(AudienceTestUtils.matchLessThan("42.42d", 42.423d)
            .evaluate(projectConfig(), attributes), is(true));
    }

    @Test
    public void testGreaterThanMatch() {
        assertThat(new UserAttribute("n", "custom_attribute", "gt", "")
            .evaluate(projectConfig(), null), nullValue());
        assertThat(new UserAttribute("n", "custom_attribute", "gt", "")
            .evaluate(projectConfig(), emptyMap()), nullValue());
        assertThat(AudienceTestUtils.matchGreaterThan("n", null)
            .evaluate(projectConfig(), attributes().add("n", 0).get()), nullValue());

        Map<String, ?> attributes = attributes()
            .add("-1", -1)
            .add("0", 0)
            .add("1", 1)
            .add("42.42f", 42.42f)
            .add("42.42d", 42.42d)
            .get();

        // int matching
        assertThat(AudienceTestUtils.matchGreaterThan("-1", 0)
            .evaluate(projectConfig(), attributes), is(false));
        assertThat(AudienceTestUtils.matchGreaterThan("1", 0)
            .evaluate(projectConfig(), attributes), is(true));
        assertThat(AudienceTestUtils.matchGreaterThan("1", 1)
            .evaluate(projectConfig(), attributes), is(false));

        // decimal input value
        assertThat(AudienceTestUtils.matchGreaterThan("1", 1.0 / 2.0)
            .evaluate(projectConfig(), attributes), is(true));
        assertThat(AudienceTestUtils.matchGreaterThan("1", 3.0 / 2.0)
            .evaluate(projectConfig(), attributes), is(false));

        // decimal match value
        assertThat(AudienceTestUtils.matchGreaterThan("42.42f", 50)
            .evaluate(projectConfig(), attributes), is(false));
        assertThat(AudienceTestUtils.matchGreaterThan("42.42f", 40)
            .evaluate(projectConfig(), attributes), is(true));

        // double matching
        assertThat(AudienceTestUtils.matchGreaterThan("42.42f", 42.423d)
            .evaluate(projectConfig(), attributes), is(false));
        assertThat(AudienceTestUtils.matchGreaterThan("42.42d", 42.423d)
            .evaluate(projectConfig(), attributes), is(false));
    }

    @Test
    public void testUnknownMatch() {
        assertThat(new UserAttribute("name", "custom_attribute", "???", 0)
            .evaluate(projectConfig(), emptyMap()), nullValue());
        assertThat(new UserAttribute("name", "???", "exact", 0)
            .evaluate(projectConfig(), emptyMap()), nullValue());
    }

    @Test
    public void testLegacyMatch() {
        Object value = "foo";

        assertThat(AudienceTestUtils.matchLegacy("a", null)
            .evaluate(projectConfig(), emptyMap()), nullValue()); // null b/c value is null
        assertThat(AudienceTestUtils.matchLegacy("a", null)
            .evaluate(projectConfig(), null), nullValue()); // null b/c value is null

        assertThat(AudienceTestUtils.matchLegacy("a", value)
            .evaluate(projectConfig(), emptyMap()), is(false));
        assertThat(AudienceTestUtils.matchLegacy("a", value)
            .evaluate(projectConfig(), attributes().get()), is(false));

        assertThat(AudienceTestUtils.matchLegacy("a", value)
            .evaluate(projectConfig(), attributes().add("a", "foo").get()), is(true));
        assertThat(AudienceTestUtils.matchLegacy("a", value)
            .evaluate(projectConfig(), attributes().add("a", "bar").get()), is(false));
        assertThat(AudienceTestUtils.matchLegacy("a", value)
            .evaluate(projectConfig(), attributes().add("a", "bar").get()), is(false));
        assertThat(AudienceTestUtils.matchLegacy("A", value)
            .evaluate(projectConfig(), attributes().add("a", "foo").get()), is(false));
        assertThat(AudienceTestUtils.matchLegacy("a", value)
            .evaluate(projectConfig(), attributes().add("A", "foo").get()), is(false));
        assertThat(AudienceTestUtils.matchLegacy("a", value)
            .evaluate(projectConfig(), attributes().add("a", "Foo").get()), is(false));
    }

    private ProjectConfig projectConfig() {
        return mock(ProjectConfig.class);
    }
}