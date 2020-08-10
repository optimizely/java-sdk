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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.optimizely.ab.internal.AttributesUtil.parseNumeric;
import static com.optimizely.ab.internal.AttributesUtil.stringIsNullOrEmpty;

public final class SemanticVersion {

    private final String version;

    public SemanticVersion(String version) {
        this.version = version;
    }

    public int compare(SemanticVersion targetedVersion) throws Exception {

        if (targetedVersion == null || stringIsNullOrEmpty(targetedVersion.version)) {
            return 0;
        }

        String[] targetedVersionParts = SemanticVersionExtension.splitSemanticVersion(targetedVersion.version);
        String[] userVersionParts = SemanticVersionExtension.splitSemanticVersion(version);

        for (int index = 0; index < targetedVersionParts.length; index++) {

            if (userVersionParts.length <= index) {
                return SemanticVersionExtension.isPreRelease(targetedVersion.version) ? 1 : -1;
            }
            Integer targetVersionPartInt = parseNumeric(targetedVersionParts[index]);
            Integer userVersionPartInt = parseNumeric(userVersionParts[index]);

            if (userVersionPartInt == null) {
                // Compare strings
                int result = userVersionParts[index].compareTo(targetedVersionParts[index]);
                if (result != 0) {
                    return result;
                }
            } else if (targetVersionPartInt != null) {
                if (!userVersionPartInt.equals(targetVersionPartInt)) {
                    return userVersionPartInt < targetVersionPartInt ? -1 : 1;
                }
            } else {
                return -1;
            }
        }

        if (!SemanticVersionExtension.isPreRelease(targetedVersion.version) &&
            SemanticVersionExtension.isPreRelease(version)) {
            return -1;
        }

        return 0;
    }

    public static class SemanticVersionExtension {
        public static final String BUILD_SEPERATOR = "+";
        public static final String PRE_RELEASE_SEPERATOR = "-";

        public static boolean isPreRelease(String semanticVersion) {
            return semanticVersion.contains(PRE_RELEASE_SEPERATOR);
        }

        public static boolean isBuild(String semanticVersion) {
            return semanticVersion.contains(BUILD_SEPERATOR);
        }

        public static String[] splitSemanticVersion(String version) throws Exception {
            List<String> versionParts = new ArrayList<>();
            // pre-release or build.
            String versionSuffix = "";
            // for example: beta.2.1
            String[] preVersionParts;

            // Contains white spaces
            if (version.contains(" ")) {   // log and throw error
                throw new Exception("Semantic version contains white spaces. Invalid Semantic Version.");
            }

            if (isBuild(version) || isPreRelease(version)) {
                String[] partialVersionParts = version.split(isPreRelease(version) ?
                    PRE_RELEASE_SEPERATOR : BUILD_SEPERATOR);

                if (partialVersionParts.length <= 1) {
                    // throw error
                    throw new Exception("Invalid Semantic Version.");
                }
                // major.minor.patch
                String versionPrefix = partialVersionParts[0];

                versionSuffix = partialVersionParts[1];

                preVersionParts = versionPrefix.split("\\.");
            } else {
                preVersionParts = version.split("\\.");
            }

            if (preVersionParts.length > 3) {
                // Throw error as pre version should only contain major.minor.patch version
                throw new Exception("Invalid Semantic Version.");
            }

            for (String preVersionPart : preVersionParts) {
                if (parseNumeric(preVersionPart) == null) {
                    throw new Exception("Invalid Semantic Version.");
                }
            }

            Collections.addAll(versionParts, preVersionParts);
            if (!stringIsNullOrEmpty(versionSuffix)) {
                versionParts.add(versionSuffix);
            }

            return versionParts.toArray(new String[0]);
        }
    }
}
