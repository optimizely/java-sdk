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
package com.optimizely.ab.internal;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonLibraryDetectorTest {

    @Test
    public void allowlistedLibrariesOnTheClasspathAreDetected() {
        assertTrue(JsonLibraryDetector.isPresent("com.google.gson.Gson"));
        assertTrue(JsonLibraryDetector.isPresent("com.fasterxml.jackson.databind.ObjectMapper"));
        assertTrue(JsonLibraryDetector.isPresent("org.json.JSONObject"));
        assertTrue(JsonLibraryDetector.isPresent("org.json.simple.JSONObject"));
    }

    @Test
    public void classNamesOutsideTheAllowlistAreRejectedEvenWhenTheClassExists() {
        assertFalse(JsonLibraryDetector.isPresent("java.lang.String"));
        assertFalse(JsonLibraryDetector.isPresent("java.lang.Runtime"));
        assertFalse(JsonLibraryDetector.isPresent("com.optimizely.ab.internal.JsonLibraryDetector"));
    }

    @Test
    public void unknownAndMalformedNamesAreRejected() {
        assertFalse(JsonLibraryDetector.isPresent(null));
        assertFalse(JsonLibraryDetector.isPresent(""));
        assertFalse(JsonLibraryDetector.isPresent("not.a.real.Clazz"));
        assertFalse(JsonLibraryDetector.isPresent("com.google.gson.Gson "));
    }
}
