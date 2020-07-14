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
            return 0;
        }
        String[] actualVersionParts = actualVersion.split("[-\\.]");
        String[] targetVersionParts = targetVersion.split("[-\\.]");

        int i = 0;
        while (i < actualVersionParts.length) {
            if (i < targetVersionParts.length) {
                Double actual = parseNumeric(actualVersionParts[i]);
                Double target = parseNumeric(targetVersionParts[i]);
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
                Double actual = parseNumeric(actualVersionParts[i]);
                if (actual != null && actual != 0) {
                    return 1;
                } else if (actual == null) {
                    return -1;
                }
            }
            i++;
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
