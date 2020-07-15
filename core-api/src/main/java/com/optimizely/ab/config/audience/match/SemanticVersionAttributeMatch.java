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

abstract class SemanticVersionAttributeMatch implements Match {
    public int compareVersion(String actualVersion, String targetVersion) {
        if (actualVersion.equals(targetVersion)) {
            // Any version.
            return 0;
        }

        // Expect a version string of the form x.y.z-(string)
        String[] actualVersionParts = actualVersion.split("[-\\.]");
        String[] targetVersionParts = targetVersion.split("[-\\.]");

        // Check only till the precision point of actualVersionParts
        for (int i = 0; i < actualVersionParts.length; i++) {
            if (i < targetVersionParts.length) {
                Double actual = parseNumeric(actualVersionParts[i]);
                Double target = parseNumeric(targetVersionParts[i]);
                // Check if the both actual and target are number then compare else if compare the string and if it's not equal than return -1
                if (actual != null && target != null) {
                    if (actual < target) {
                        return -1;
                    } else if (actual > target) {
                        return 1;
                    }
                } else if (!actualVersionParts[i].equals(targetVersionParts[i])) {
                    return -1;
                }
            } else {
                // If actualVersionParts is greater than targetVersionParts and is not zero than return 1 else if actual is string then return -1
                // So if actualVersionParts[i] is beta/alpha then this means targetVersion is greater than actualVersion
                Double actual = parseNumeric(actualVersionParts[i]);
                if (actual != null && actual != 0) {
                    return 1;
                } else if (actual == null) {
                    return -1;
                }
            }
        }

        return 0;
    }

    private static Double parseNumeric(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
