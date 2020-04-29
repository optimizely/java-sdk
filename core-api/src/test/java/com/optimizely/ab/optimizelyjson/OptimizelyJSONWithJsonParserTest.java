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
import com.optimizely.ab.config.parser.JsonConfigParser;
import com.optimizely.ab.config.parser.UnsupportedOperationException;
import org.junit.Test;

import static org.junit.Assert.*;

public class OptimizelyJSONWithJsonParserTest extends OptimizelyJSONCoreTest {
    @Override
    protected ConfigParser getParser() {
        return new JsonConfigParser();
    }

    // Tests for Json only

    @Test
    public void testGetValueThrowsException() {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        try {
            MD1 md1 = oj1.getValue(null, MD1.class);
            fail("GetValue is not supported for or.json paraser");
        } catch (UnsupportedOperationException e) {
            assertEquals(e.getMessage(), "A proper JSON parser is not available. Use Gson or Jackson parser for this operation.");
        }
    }

}
