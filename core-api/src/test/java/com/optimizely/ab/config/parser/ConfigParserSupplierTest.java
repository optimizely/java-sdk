/**
 *    Copyright 2026, Optimizely Inc. and contributors
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
package com.optimizely.ab.config.parser;

import com.optimizely.ab.internal.JsonLibraryDetector;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ConfigParserSupplierTest {

    @Test
    public void everyConfigParserSupplierClassNameIsAllowlisted() {
        // guards against adding a parser to the enum and forgetting the JsonLibraryDetector allowlist
        for (DefaultConfigParser.ConfigParserSupplier supplier : DefaultConfigParser.ConfigParserSupplier.values()) {
            assertTrue(supplier + " is missing from the JsonLibraryDetector allowlist",
                JsonLibraryDetector.isAllowed(supplier.getClassName()));
        }
    }
}
