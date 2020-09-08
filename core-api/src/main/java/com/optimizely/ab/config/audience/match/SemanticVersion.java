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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.optimizely.ab.internal.AttributesUtil.parseNumeric;
import static com.optimizely.ab.internal.AttributesUtil.stringIsNullOrEmpty;

public final class SemanticVersion {

    private static final Logger logger = LoggerFactory.getLogger(SemanticVersion.class);
    private static final String BUILD_SEPERATOR = "\\+";
    private static final String PRE_RELEASE_SEPERATOR = "-";

    private final String version;

    public SemanticVersion(String version) {
        this.version = version;
    }

    public static int compare(Object o1, Object o2) throws UnexpectedValueTypeException {
        if (o1 instanceof String && o2 instanceof String) {
            SemanticVersion v1 = new SemanticVersion((String) o1);
            SemanticVersion v2 = new SemanticVersion((String) o2);
            try {
                return v1.compare(v2);
            } catch (Exception e) {
                logger.warn("Error comparing semantic versions", e);
            }
        }

        throw new UnexpectedValueTypeException();
    }

    public int compare(SemanticVersion targetedVersion) throws Exception {

        if (targetedVersion == null || stringIsNullOrEmpty(targetedVersion.version)) {
            return 0;
        }

        String[] targetedVersionParts = targetedVersion.splitSemanticVersion();
        String[] userVersionParts = splitSemanticVersion();

        for (int index = 0; index < targetedVersionParts.length; index++) {

            if (userVersionParts.length <= index) {
                return targetedVersion.isPreRelease() ? 1 : -1;
            }
            Integer targetVersionPartInt = parseNumeric(targetedVersionParts[index]);
            Integer userVersionPartInt = parseNumeric(userVersionParts[index]);

            if (userVersionPartInt == null) {
                // Compare strings
                int result = userVersionParts[index].compareTo(targetedVersionParts[index]);
                if (result < 0) {
                    return targetedVersion.isPreRelease() && !isPreRelease() ? 1 : -1;
                } else if (result > 0) {
                    return !targetedVersion.isPreRelease() && isPreRelease() ? -1 : 1;
                }
            } else if (targetVersionPartInt != null) {
                if (!userVersionPartInt.equals(targetVersionPartInt)) {
                    return userVersionPartInt < targetVersionPartInt ? -1 : 1;
                }
            } else {
                return -1;
            }
        }

        if (!targetedVersion.isPreRelease() &&
            isPreRelease()) {
            return -1;
        }

        return 0;
    }

    public boolean isPreRelease() {
        int buildIndex = version.indexOf("+");
        int preReleaseIndex = version.indexOf("-");
        if (buildIndex < 0) {
            return preReleaseIndex > 0;
        } else if(preReleaseIndex < 0) {
            return false;
        }
        return  preReleaseIndex < buildIndex;
    }

    public boolean isBuild() {
        int buildIndex = version.indexOf("+");
        int preReleaseIndex = version.indexOf("-");
        if (preReleaseIndex < 0) {
            return buildIndex > 0;
        } else if(buildIndex < 0) {
            return false;
        }
        return buildIndex < preReleaseIndex;
    }

    private int dotCount(String prefixVersion) {
        char[] vCharArray = prefixVersion.toCharArray();
        int count = 0;
        for (char c : vCharArray) {
            if (c == '.') {
                count++;
            }
        }
        return count;
    }

    private boolean isValidBuildMetadata() {
        char[] vCharArray = version.toCharArray();
        int count = 0;
        for (char c : vCharArray) {
            if (c == '+') {
                count++;
            }
        }
        return count > 1;
    }

    public String[] splitSemanticVersion() throws Exception {
        List<String> versionParts = new ArrayList<>();
        String versionPrefix = "";
        // pre-release or build.
        String versionSuffix = "";
        // for example: beta.2.1
        String[] preVersionParts;

        // Contains white spaces
        if (version.contains(" ") || isValidBuildMetadata()) {   // log and throw error
            throw new Exception("Invalid Semantic Version.");
        }

        if (isBuild() || isPreRelease()) {
            String[] partialVersionParts = version.split(isPreRelease() ?
                PRE_RELEASE_SEPERATOR : BUILD_SEPERATOR, 2);

            if (partialVersionParts.length <= 1) {
                // throw error
                throw new Exception("Invalid Semantic Version.");
            }
            // major.minor.patch
            versionPrefix = partialVersionParts[0];

            versionSuffix = partialVersionParts[1];

        } else {
            versionPrefix = version;
        }

        preVersionParts = versionPrefix.split("\\.");

        if (preVersionParts.length > 3 ||
            preVersionParts.length == 0 ||
            dotCount(versionPrefix) >= preVersionParts.length) {
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
