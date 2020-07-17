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

import static com.optimizely.ab.internal.AttributesUtil.parseNumeric;

abstract class SemanticVersionAttributeMatch implements Match {

    /**
     * Compare an actual userVersion against a targetedVersion; return -1 if the actual userVersion is "semantically less"
     * than the targetedVersion, 1 if it is "semantically greater", and 0 if they are "semantically identical".
     *
     * "Semantically" means the following: given both userVersion numbers expressed in x.y.z... format, to the level of
     * precision of the targetedVersion, compare the corresponding userVersion parts (e.g. major to major, minor to minor).
     *
     * @param userVersion expressed as a string x.y.z...
     * @param targetedVersion expressed as a string x.y.z...
     * @return -1 if userVersion < targetedVersion, 1 if userVersion > targetedVersion, 0 if they are approx. equal
     */
    public int compareVersion(String userVersion, String targetedVersion) {
        if (targetedVersion == null || targetedVersion.isEmpty()) {
            // Any version.
            return 0;
        }

        // Expect a version string of the form x.y.z
        String[] userVersionParts = userVersion.split("\\.");
        String[] targetVersionParts = targetedVersion.split("\\.");

        // Check only till the precision point of targetVersionParts
        for (int targetIndex = 0; targetIndex < targetVersionParts.length; targetIndex++) {
            if (userVersionParts.length <= targetIndex) {
                return -1;
            }
            Integer userVersionPart  = parseNumeric(userVersionParts[targetIndex]);
            Integer targetVersionPart = parseNumeric(targetVersionParts[targetIndex]);

            if (userVersionPart == null) {
                //Compare strings
                if (!userVersionParts[targetIndex].equals(targetVersionParts[targetIndex])) {
                    return -1;
                }
            } else if (targetVersionPart != null) {
                if (userVersionPart < targetVersionPart) {
                    return -1;
                } else if (userVersionPart > targetVersionPart) {
                    return 1;
                }
            } else {
                return -1;
            }
        }
        return 0;
    }

}
