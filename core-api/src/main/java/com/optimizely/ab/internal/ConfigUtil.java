/**
 *
 *    Copyright 2019, Optimizely
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * ConfigUtil is a utility for pulling environment config from system properties or a properties file.
 */
public final class ConfigUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    /**
     * The filename of the Optimizely configuration file.
     */
    private static final String CONFIG_FILE_NAME = "optimizely.properties";
    /**
     * Properties loaded from the Sentry configuration file, or null if no file was
     * found or it failed to parse.
     */
    private static Properties properties;

    static {
        InputStream input = null;
        try {
            input = getInputStream(CONFIG_FILE_NAME);

            if (input != null) {
                properties = new Properties();
                properties.load(input);
            } else {
                logger.debug("Sentry configuration file not found in filesystem or classpath: '{}'.", CONFIG_FILE_NAME);
            }
        } catch (Exception e) {
            logger.error("Error loading Sentry configuration file '{}': ", CONFIG_FILE_NAME, e);
        } finally {
            closeSafely(input);
        }
    }

    /**
     * Clears a System property prepended with "optimizely.".
     */
    public static void clear(String key) {
        System.clearProperty("optimizely." + key);
    }

    /**
     * Sets a System property prepended with "optimizely.".
     */
    public static void set(String key, String value) {
        System.setProperty("optimizely." + key, value);
    }

    /**
     * Get a configuration value from one of the supported locations.
     * <ul>
     *   <li>System Properties - Key is prepended with "optimizely."</li>
     *   <li>Environment variables - Key is prepended with "optimizely.", uppercased and "." are replaced with "_".</li>
     *   <li>Optimizely Properties - Key is sourced as-is.</li>
     * </ul>
     */
    public static String get(String key) {
        return get(key, null);
    }

    /**
     * Get a configuration value from one of the supported locations. If a value cannot be found, then the default
     * is returned.
     * <ul>
     *   <li>System Properties - Key is prepended with "optimizely."</li>
     *   <li>Environment variables - Key is prepended with "optimizely.", uppercased and "." are replaced with "_".</li>
     *   <li>Optimizely Properties - Key is sourced as-is.</li>
     * </ul>
     */
    public static String get(String key, String dafault) {
        // Try to obtain from a Java System Property
        String value = System.getProperty("optimizely." + key.toLowerCase());
        if (value != null) {
            logger.debug("Found {}={} in Java System Properties.", key, value);
            return value.trim();
        }

        // Try to obtain from a System Environment Variable
        value = System.getenv("SENTRY_" + key.replace(".", "_").toUpperCase());
        if (value != null) {
            logger.debug("Found {}={} in System Environment Variables.", key, value);
            return value.trim();
        }

        // Try to obtain from config file
        value = properties == null ? null : properties.getProperty(key);
        if (value != null) {
            logger.debug("Found {}={} in {}.", key, value, CONFIG_FILE_NAME);
            return value.trim();
        }

        return dafault;
    }

    private static InputStream getInputStream(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        if (file.isFile() && file.canRead()) {
            return new FileInputStream(file);
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader.getResourceAsStream(filePath);
    }

    private static void closeSafely(InputStream input) {
        if (input == null) {
            return;
        }

        try {
            input.close();
        } catch (IOException e) {
            logger.warn("Error closing configuration file.", e);
        }
    }

}
