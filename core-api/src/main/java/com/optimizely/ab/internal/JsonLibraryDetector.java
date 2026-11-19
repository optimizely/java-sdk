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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects whether a supported JSON library is available on the classpath.
 * <p>
 * Class names are only ever looked up if they are in a fixed allowlist of known JSON library classes, so
 * {@code Class.forName} can never be driven by an arbitrary (for example user-supplied) class name.
 * Classes are also loaded without being initialized, so no static initializer of the probed class runs.
 */
public final class JsonLibraryDetector {

    private static final Set<String> ALLOWED_CLASS_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "com.google.gson.Gson",
        "com.fasterxml.jackson.databind.ObjectMapper",
        "org.json.JSONObject",
        "org.json.simple.JSONObject"
    )));

    private JsonLibraryDetector() {
    }

    /**
     * @param className fully qualified class name to look for
     * @return true if the class is one of the supported JSON library classes and is present on the classpath,
     * false if it is absent or is not in the allowlist
     */
    public static boolean isPresent(String className) {
        if (className == null || !ALLOWED_CLASS_NAMES.contains(className)) {
            return false;
        }

        try {
            // nosemgrep: java.lang.security.audit.unsafe-reflection.unsafe-reflection
            // className is validated against the fixed allowlist above and the class is not initialized.
            Class.forName(className, false, JsonLibraryDetector.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
