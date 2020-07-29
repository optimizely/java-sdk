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
package com.optimizely.ab.config.audience;

import com.optimizely.ab.config.audience.match.SemanticVersion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.text.ParseException;

import static org.junit.Assert.*;

public class SemanticVersionTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void semanticVersionEmptyStringThrowsException() throws ParseException {
        thrown.expect(ParseException.class);
        // Semantic version must not be empty
        new SemanticVersion("");
    }

    @Test
    public void semanticVersionInvalidLeadingZerosThrowsException() throws ParseException {
        thrown.expect(ParseException.class);
        // Semantic version must not contain leading zeros
        new SemanticVersion("03.7.1");
    }

    @Test
    public void semanticVersionInvalidMajorShouldBeNumberOnly() throws ParseException {
        thrown.expect(ParseException.class);
        new SemanticVersion("a.2.1");
    }


    @Test
    public void semanticVersionInvalidMinorShouldBeNumberOnly() throws ParseException {
        thrown.expect(ParseException.class);
        new SemanticVersion("1.b.1");
    }

    @Test
    public void semanticVersionInvalidPatchShouldBeNumberOnly() throws ParseException {
        thrown.expect(ParseException.class);
        new SemanticVersion("1.2.c");
    }

    @Test
    public void semanticVersionEqualsTrue() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.0");
        SemanticVersion actualSV = new SemanticVersion("3.7.0");
        assertTrue(actualSV.equals(targetSV));
    }

    @Test
    public void semanticVersionEqualsWithPreReleaseAndMetaTrue() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.0-beta+2.3");
        SemanticVersion actualSV = new SemanticVersion("3.7.0-beta+2.3");
        assertTrue(actualSV.equals(targetSV));
    }

    @Test
    public void semanticVersionEqualsWithPreReleaseAndMetaFalse() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.0-beta+2.3");
        SemanticVersion actualSV = new SemanticVersion("3.7.0-beta+3");
        assertFalse(actualSV.equals(targetSV));
    }

    @Test
    public void semanticVersionEqualsSameObjectTrue() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.0");
        assertTrue(targetSV.equals(targetSV));
    }

    @Test
    public void semanticVersionSame() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7");
        SemanticVersion actualSV = new SemanticVersion("3.7");
        assertTrue(actualSV.equals(targetSV));
    }

    @Test
    public void semanticVersionEqualsComparesPreRelease() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1-beta");
        SemanticVersion actualSV = new SemanticVersion("3.7.1-alpha");
        assertFalse(actualSV.equals(targetSV));
    }

    @Test
    public void semanticVersionEqualsComparesMeta() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1+beta");
        SemanticVersion actualSV = new SemanticVersion("3.7.1+alpha");
        assertFalse(actualSV.equals(targetSV));
    }

    @Test
    public void semanticVersionTargetIsNotSemanticVersionObject() throws ParseException {
        SemanticVersion actualSV = new SemanticVersion("3.7.0");
        assertFalse(actualSV.equals(3.7));
    }

    @Test
    public void semanticVersionTargetToString() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7");
        assertEquals(targetSV.toString(), "3.7");
    }

    @Test
    public void semanticVersionTargetToStringComplete() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1-2.2.e+b.d2");
        assertEquals(targetSV.toString(), "3.7.1-2.2.e+b.d2");
    }

    @Test
    public void semanticVersionTargetHashCode() throws ParseException {
        String str = "3.7.1";
        SemanticVersion targetSV = new SemanticVersion("3.7.1");
        assertEquals(targetSV.hashCode(), str.hashCode());
    }

    @Test
    public void semanticVersionCompareTo() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1");
        SemanticVersion actualSV = new SemanticVersion("3.7.1");
        assertTrue(actualSV.compareTo(targetSV) == 0);
    }

    @Test
    public void semanticVersionCompareToActualLess() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1");
        SemanticVersion actualSV = new SemanticVersion("3.7.0");
        assertTrue(actualSV.compareTo(targetSV) < 0);
    }

    @Test
    public void semanticVersionCompareToActualGreater() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1");
        SemanticVersion actualSV = new SemanticVersion("3.7.2");
        assertTrue(actualSV.compareTo(targetSV) > 0);
    }

    @Test
    public void semanticVersionCompareToPatchMissing() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7");
        SemanticVersion actualSV = new SemanticVersion("3.7.1");
        assertTrue(actualSV.compareTo(targetSV) == 0);
    }

    @Test
    public void semanticVersionCompareToActualPatchMissing() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1");
        SemanticVersion actualSV = new SemanticVersion("3.7");
        assertTrue(actualSV.compareTo(targetSV) < 0);
    }

    @Test
    public void semanticVersionCompareToActualPreReleaseMissing() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1-beta");
        SemanticVersion actualSV = new SemanticVersion("3.7.1");
        assertTrue(actualSV.compareTo(targetSV) > 0);
    }

    @Test
    public void semanticVersionCompareToAlphaBetaAsciiComparision() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1-alpha");
        SemanticVersion actualSV = new SemanticVersion("3.7.1-beta");
        assertTrue(actualSV.compareTo(targetSV) > 0);
    }

    @Test
    public void semanticVersionCompareToIgnoreMetaComparision() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1-beta.1+2.3");
        SemanticVersion actualSV = new SemanticVersion("3.7.1-beta.1+3.45");
        assertTrue(actualSV.compareTo(targetSV) == 0);
    }

    @Test
    public void semanticVersionCompareToPreReleaseComparision() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1-beta.1");
        SemanticVersion actualSV = new SemanticVersion("3.7.1-beta.2");
        assertTrue(actualSV.compareTo(targetSV) > 0);
    }

    @Test
    public void semanticVersionCompareToDoNotCompareBuildVersionComparision() throws ParseException {
        SemanticVersion targetSV = new SemanticVersion("3.7.1+beta.1");
        SemanticVersion actualSV = new SemanticVersion("3.7.1+beta.2");
        assertTrue(actualSV.compareTo(targetSV) == 0);
    }
}
