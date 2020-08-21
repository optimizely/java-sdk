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

import static org.junit.Assert.*;

public class SemanticVersionTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void semanticVersionInvalidMajorShouldBeNumberOnly() throws Exception {
        thrown.expect(Exception.class);
        SemanticVersion semanticVersion = new SemanticVersion("a.2.1");
        semanticVersion.splitSemanticVersion();
    }

    @Test
    public void semanticVersionInvalidMinorShouldBeNumberOnly() throws Exception {
        thrown.expect(Exception.class);
        SemanticVersion semanticVersion = new SemanticVersion("1.b.1");
        semanticVersion.splitSemanticVersion();
    }

    @Test
    public void semanticVersionInvalidPatchShouldBeNumberOnly() throws Exception {
        thrown.expect(Exception.class);
        SemanticVersion semanticVersion = new SemanticVersion("1.2.c");
        semanticVersion.splitSemanticVersion();
    }

    @Test
    public void semanticVersionInvalidShouldBeOfSizeLessThan3() throws Exception {
        thrown.expect(Exception.class);
        SemanticVersion semanticVersion = new SemanticVersion("1.2.2.3");
        semanticVersion.splitSemanticVersion();
    }

    @Test
    public void semanticVersionCompareTo() throws Exception {
        SemanticVersion targetSV = new SemanticVersion("3.7.1");
        SemanticVersion actualSV = new SemanticVersion("3.7.1");
        assertTrue(actualSV.compare(targetSV) == 0);
    }

    @Test
    public void semanticVersionCompareToActualLess() throws Exception {
        SemanticVersion targetSV = new SemanticVersion("3.7.1");
        SemanticVersion actualSV = new SemanticVersion("3.7.0");
        assertTrue(actualSV.compare(targetSV) < 0);
    }

    @Test
    public void semanticVersionCompareToActualGreater() throws Exception {
        SemanticVersion targetSV = new SemanticVersion("3.7.1");
        SemanticVersion actualSV = new SemanticVersion("3.7.2");
        assertTrue(actualSV.compare(targetSV) > 0);
    }

    @Test
    public void semanticVersionCompareToPatchMissing() throws Exception {
        SemanticVersion targetSV = new SemanticVersion("3.7");
        SemanticVersion actualSV = new SemanticVersion("3.7.1");
        assertTrue(actualSV.compare(targetSV) == 0);
    }

    @Test
    public void semanticVersionCompareToActualPatchMissing() throws Exception {
        SemanticVersion targetSV = new SemanticVersion("3.7.1");
        SemanticVersion actualSV = new SemanticVersion("3.7");
        assertTrue(actualSV.compare(targetSV) < 0);
    }

    @Test
    public void semanticVersionCompareToActualPreReleaseMissing() throws Exception {
        SemanticVersion targetSV = new SemanticVersion("3.7.1-beta");
        SemanticVersion actualSV = new SemanticVersion("3.7.1");
        assertTrue(actualSV.compare(targetSV) > 0);
    }

    @Test
    public void semanticVersionCompareToAlphaBetaAsciiComparision() throws Exception {
        SemanticVersion targetSV = new SemanticVersion("3.7.1-alpha");
        SemanticVersion actualSV = new SemanticVersion("3.7.1-beta");
        assertTrue(actualSV.compare(targetSV) > 0);
    }

    @Test
    public void semanticVersionCompareToIgnoreMetaComparision() throws Exception {
        SemanticVersion targetSV = new SemanticVersion("3.7.1-beta.1+2.3");
        SemanticVersion actualSV = new SemanticVersion("3.7.1-beta.1+2.3");
        assertTrue(actualSV.compare(targetSV) == 0);
    }

    @Test
    public void semanticVersionCompareToPreReleaseComparision() throws Exception {
        SemanticVersion targetSV = new SemanticVersion("3.7.1-beta.1");
        SemanticVersion actualSV = new SemanticVersion("3.7.1-beta.2");
        assertTrue(actualSV.compare(targetSV) > 0);
    }
}
