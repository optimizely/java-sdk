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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Objects;

import static com.optimizely.ab.internal.AttributesUtil.parseNumeric;

public final class SemanticVersion implements Comparable<SemanticVersion> {

    /**
     * Major version number
     */
    public final Integer major;

    /**
     * Minor version number
     */
    public final Integer minor;

    /**
     * Patch level
     */
    public final Integer patch;

    /**
     * Pre-release tags (potentially empty, but never null). This is private to
     * ensure read only access.
     */
    private final String[] preRelease;

    /**
     * Build meta data tags (potentially empty, but never null). This is private
     * to ensure read only access.
     */
    private final String[] buildMeta;

    /**
     * Construct a version object by parsing a string.
     *
     * @param version version in flat string format
     */
    public SemanticVersion(String version) throws ParseException {
        // Throw exception if version contains empty space
        if (version.contains(" ")) {
            throw new ParseException(version, version.indexOf(" "));
        }

        vParts = new Integer[3];
        preParts = new ArrayList<>(5);
        metaParts = new ArrayList<>(5);
        input = version.toCharArray();
        if (!stateMajor()) { // Start recursive descend
            throw new ParseException(version, errPos);
        }
        major = vParts[0];
        minor = vParts[1];
        patch = vParts[2];
        preRelease = preParts.toArray(new String[preParts.size()]);
        buildMeta = metaParts.toArray(new String[metaParts.size()]);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(major);
        if (minor != null) {
            ret.append('.');
            ret.append(minor);
        }
        if (patch != null) {
            ret.append('.');
            ret.append(patch);
        }
        if (preRelease.length > 0) {
            ret.append('-');
            for (int i = 0; i < preRelease.length; i++) {
                ret.append(preRelease[i]);
                if (i < preRelease.length - 1) {
                    ret.append('.');
                }
            }
        }
        if (buildMeta.length > 0) {
            ret.append('+');
            for (int i = 0; i < buildMeta.length; i++) {
                ret.append(buildMeta[i]);
                if (i < buildMeta.length - 1) {
                    ret.append('.');
                }
            }
        }
        return ret.toString();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SemanticVersion)) {
            return false;
        }
        SemanticVersion ov = (SemanticVersion) other;
        if (!Objects.equals(major, ov.major) ||
            !Objects.equals(minor, ov.minor) ||
            !Objects.equals(patch, ov.patch)) {
            return false;
        }
        if (ov.preRelease.length != preRelease.length) {
            return false;
        }
        for (int i = 0; i < preRelease.length; i++) {
            if (!preRelease[i].equals(ov.preRelease[i])) {
                return false;
            }
        }
        if (ov.buildMeta.length != buildMeta.length) {
            return false;
        }
        for (int i = 0; i < buildMeta.length; i++) {
            if (!buildMeta[i].equals(ov.buildMeta[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int compareTo(SemanticVersion targetVersion) {
        int result = major - targetVersion.major;
        if (result == 0) { // Same major
            if (targetVersion.minor != null) {
                if (minor != null) {
                    result = minor - targetVersion.minor;
                    if (result == 0) { // Same minor
                        if (targetVersion.patch != null) {
                            if (patch != null) {
                                result = patch - targetVersion.patch;
                                if (result == 0) { // Same patch
                                    if (preRelease.length == 0 && targetVersion.preRelease.length > 0) {
                                        result = 1; // No pre release wins over pre release
                                    }
                                    if (targetVersion.preRelease.length == 0 && preRelease.length > 0) {
                                        result = -1; // No pre release wins over pre release
                                    }
                                    if (preRelease.length > 0 && targetVersion.preRelease.length > 0) {
                                        int len = Math.min(preRelease.length, targetVersion.preRelease.length);
                                        int count;
                                        for (count = 0; count < len; count++) {
                                            result = comparePreReleaseTag(count, targetVersion);
                                            if (result != 0) {
                                                break;
                                            }
                                        }
                                        if (result == 0 && count == len) { // Longer version wins.
                                            result = preRelease.length - targetVersion.preRelease.length;
                                        }
                                    }
                                }
                            } else {
                                result = -1;
                            }
                        }
                    }
                } else {
                    result = -1;
                }
            }
        }
        return result;
    }

    private int comparePreReleaseTag(int pos, SemanticVersion ov) {
        Integer here = parseNumeric(preRelease[pos]);
        Integer there = parseNumeric(ov.preRelease[pos]);

        if (here != null && there == null) {
            return -1; // Strings take precedence over numbers
        }
        if (here == null && there != null) {
            return 1; // Strings take precedence over numbers
        }
        if (here == null) {
            return (preRelease[pos].compareTo(ov.preRelease[pos])); // ASCII compare
        }
        return here.compareTo(there); // Number compare
    }

    // Parser implementation below

    private final Integer[] vParts;
    private final ArrayList<String> preParts;
    private final ArrayList<String> metaParts;
    private int errPos;
    private final char[] input;

    private boolean stateMajor() {
        int pos = 0;
        while (pos < input.length && input[pos] >= '0' && input[pos] <= '9') {
            pos++; // match [0..9]+
        }
        if (pos == 0) { // Empty String -> Error
            return false;
        }
        if (input[0] == '0' && pos > 1) { // Leading zero
            return false;
        }

        vParts[0] = parseNumeric(new String(input, 0, pos));

        if (input.length > pos && input[pos] == '.') {
            return stateMinor(pos + 1);
        } else {
            vParts[1] = null;
            vParts[2] = null;
            return true;
        }
    }

    private boolean stateMinor(int index) {
        int pos = index;
        while (pos < input.length && input[pos] >= '0' && input[pos] <= '9') {
            pos++;// match [0..9]+
        }
        if (pos == index) { // Empty String -> Error
            errPos = index;
            return false;
        }
        if (input[0] == '0' && pos - index > 1) { // Leading zero
            errPos = index;
            return false;
        }
        vParts[1] = parseNumeric(new String(input, index, pos - index));

        if (input.length > pos && input[pos] == '.') {
            return statePatch(pos + 1);
        } else {
            vParts[2] = null;
            return true;
        }
    }

    private boolean statePatch(int index) {
        int pos = index;
        while (pos < input.length && input[pos] >= '0' && input[pos] <= '9') {
            pos++; // match [0..9]+
        }
        if (pos == index) { // Empty String -> Error
            errPos = index;
            return false;
        }
        if (input[0] == '0' && pos - index > 1) { // Leading zero
            errPos = index;
            return false;
        }

        vParts[2] = parseNumeric(new String(input, index, pos - index));

        if (pos >= input.length) { // We have a clean version string
            return true;
        }

        if (input[pos] == '+') { // We have build meta tags -> descend
            return stateMeta(pos + 1);
        }

        if (input[pos] == '-') { // We have pre release tags -> descend
            return stateRelease(pos + 1);
        }

        errPos = pos; // We have junk
        return false;
    }

    private boolean stateRelease(int index) {
        int pos = index;
        while ((pos < input.length)
            && ((input[pos] >= '0' && input[pos] <= '9')
            || (input[pos] >= 'a' && input[pos] <= 'z')
            || (input[pos] >= 'A' && input[pos] <= 'Z') || input[pos] == '-')) {
            pos++; // match [0..9a-zA-Z-]+
        }
        if (pos == index) { // Empty String -> Error
            errPos = index;
            return false;
        }

        preParts.add(new String(input, index, pos - index));
        if (pos == input.length) { // End of input
            return true;
        }
        if (input[pos] == '.') { // More parts -> descend
            return stateRelease(pos + 1);
        }
        if (input[pos] == '+') { // Build meta -> descend
            return stateMeta(pos + 1);
        }

        errPos = pos;
        return false;
    }

    private boolean stateMeta(int index) {
        int pos = index;
        while ((pos < input.length)
            && ((input[pos] >= '0' && input[pos] <= '9')
            || (input[pos] >= 'a' && input[pos] <= 'z')
            || (input[pos] >= 'A' && input[pos] <= 'Z') || input[pos] == '-')) {
            pos++; // match [0..9a-zA-Z-]+
        }
        if (pos == index) { // Empty String -> Error
            errPos = index;
            return false;
        }

        metaParts.add(new String(input, index, pos - index));
        if (pos == input.length) { // End of input
            return true;
        }
        if (input[pos] == '.') { // More parts -> descend
            return stateMeta(pos + 1);
        }
        errPos = pos;
        return false;
    }
}
