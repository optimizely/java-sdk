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
package com.optimizely.ab.optimizelyjson;

import com.optimizely.ab.config.parser.ConfigParser;
import com.optimizely.ab.config.parser.GsonConfigParser;
import com.optimizely.ab.config.parser.UnsupportedOperationException;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class OptimizelyJSONWithGsonParserTest extends OptimizelyJSONTest {
    @Override
    protected ConfigParser getParser() {
        return new GsonConfigParser();
    }

    // Tests for GSON only

    @Test
    public void testGetValueWithNotMatchingType() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        NotMatchingType md = oj1.getValue(null, NotMatchingType.class);
        assertNull(md.x99);
    }
}
