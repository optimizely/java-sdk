/**
 *
 *    Copyright 2016-2017, 2019, 2022, Optimizely and contributors
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
package com.optimizely.ab.event.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Exception;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.annotation.concurrent.Immutable;

/**
 * Helper class to retrieve the SDK version information.
 */
@Immutable
public final class BuildVersionInfo {

    private static final Logger logger = LoggerFactory.getLogger(BuildVersionInfo.class);

    public static String VERSION = readVersionNumber();

    // can be overridden by other wrapper client (android-sdk)
    public static void setClientVersion(String version) {
         VERSION = version;
    }

    public static String getClientVersion() {
        return VERSION;
    }

    private static String readVersionNumber() {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader =
                new BufferedReader(
                    new InputStreamReader(BuildVersionInfo.class.getResourceAsStream("/optimizely-build-version"),
                        Charset.forName("UTF-8")));

            return bufferedReader.readLine();
        } catch (Exception e) {
            logger.error("unable to read version number");
            return "unknown";
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (Exception e) {
                logger.error("unable to close reader cleanly");
            }
        }
    }

    private BuildVersionInfo() {
    }
}
