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

    /**
     * Compare an actual version against a targetedVersion; return -1 if the actual version is "semantically less"
     * than the targetedVersion, 1 if it is "semantically greater", and 0 if they are "semantically identical".
     *
     * "Semantically" means the following: given both version numbers expressed in x.y.z... format, to the level of
     * precision of the targetedVersion, compare the corresponding version parts (e.g. major to major, minor to minor).
     *
     * @param version expressed as a string x.y.z...
     * @param targetedVersion expressed as a string x.y.z...
     * @return -1 if version < targetedVersion, 1 if version > targetedVersion, 0 if they are approx. equal
     */
    public int compareVersion(String version, String targetedVersion) {
        if (targetedVersion == null || targetedVersion.isEmpty()) {
            // Any version.
            return 0;
        }

        // Expect a version string of the form x.y.z
        String[] versionParts = version.split("\\.");
        String[] targetVersionParts = targetedVersion.split("\\.");

        // Check only till the precision point of targetVersionParts
        for (int targetIndex = 0; targetIndex < targetVersionParts.length; targetIndex++) {
            if ((versionParts.length - 1) < targetIndex) {
                return -1;
            }
            Double part  = parseNumeric(versionParts[targetIndex]);
            Double target = parseNumeric(targetVersionParts[targetIndex]);

            if (part == null) {
                //Compare strings
                if (!versionParts[targetIndex].equals(targetVersionParts[targetIndex])) {
                    return -1;
                }
            } else if (target != null) {
                if (part < target) {
                    return -1;
                } else if (part > target) {
                    return 1;
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
