/**
 *
 *    Copyright 2019,2021, Optimizely
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
 * PropertyUtils is a utility for pulling parameters from system properties, environment variable
 * or a Optimizely properties file respectively.
 */
public final class PropertyUtils {
    private static final Logger logger = LoggerFactory.getLogger(PropertyUtils.class);
    /**
     * The filename of the Optimizely configuration file.
     */
    private static final String OPTIMIZELY_PROP_FILE = "optimizely.properties";
    /**
     * Properties loaded from the Optimizely configuration file, or null if no file was
     * found or it failed to parse.
     */
    private static Properties properties;

    // Attempt to load an Optimizely configuration class for override parameters.
    static {
        InputStream input = null;
        try {
            input = getInputStream(OPTIMIZELY_PROP_FILE);

            if (input != null) {
                properties = new Properties();
                properties.load(input);
            } else {
                logger.debug("Optimizely properties file not found in filesystem or classpath: '{}'.", OPTIMIZELY_PROP_FILE);
            }
        } catch (Exception e) {
            logger.error("Error loading Optimizely properties file '{}': ", OPTIMIZELY_PROP_FILE, e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.warn("Error closing properties file.", e);
                }
            }
        }
    }

    /**
     * Clears a System property prepended with "optimizely.".
     * @param key       The configuration key
     */
    public static void clear(String key) {
        System.clearProperty("optimizely." + key);
    }

    /**
     * Sets a System property prepended with "optimizely.".
     * 
     * @param key       The configuration key
     * @param value     The String value
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
     *
     * @param key       The configuration key
     * @return          The String value
     */
    public static String get(String key) {
        return get(key, null);
    }

    /**
     * Get a configuration value from one of the supported locations. If a value cannot be found, then the default
     * is returned.
     * <ul>
     *   <li>System Properties - Key is prepended with "optimizely."</li>
     *   <li>Environment variables - Key is prepended with "optimizely.", upper cased and "."s are replaced with "_"s.</li>
     *   <li>Optimizely Properties - Key is sourced as-is.</li>
     * </ul>
     *
     * @param key       The configuration key
     * @param dafault   The default value
     * @return          The String value
     */
    public static String get(String key, String dafault) {
        // Try to obtain from a Java System Property
        String value = System.getProperty("optimizely." + key.toLowerCase());
        if (value != null) {
            logger.debug("Found {}={} in Java System Properties.", key, value);
            return value.trim();
        }

        // Try to obtain from a System Environment Variable
        value = System.getenv("OPTIMIZELY_" + key.replace(".", "_").toUpperCase());
        if (value != null) {
            logger.debug("Found {}={} in System Environment Variables.", key, value);
            return value.trim();
        }

        // Try to obtain from config file
        value = properties == null ? null : properties.getProperty(key);
        if (value != null) {
            logger.debug("Found {}={} in {}.", key, value, OPTIMIZELY_PROP_FILE);
            return value.trim();
        }

        return dafault;
    }

    /**
     * Get a configuration value as Long from one of the supported locations. If a value cannot be found, or if
     * the value is not a valid number, then null is returned.
     * <ul>
     *   <li>System Properties - Key is prepended with "optimizely."</li>
     *   <li>Environment variables - Key is prepended with "optimizely.", upper cased and "."s are replaced with "_"s.</li>
     *   <li>Optimizely Properties - Key is sourced as-is.</li>
     * </ul>
     *
     * @param key       The configuration key
     * @return          The integer value
     */
    public static Long getLong(String key) {
        return getLong(key, null);
    }

    /**
     * Get a configuration value as Long from one of the supported locations. If a value cannot be found, then the default
     * is returned.
     * <ul>
     *   <li>System Properties - Key is prepended with "optimizely."</li>
     *   <li>Environment variables - Key is prepended with "optimizely.", upper cased and "."s are replaced with "_"s.</li>
     *   <li>Optimizely Properties - Key is sourced as-is.</li>
     * </ul>
     *
     * @param key       The configuration key
     * @param dafault   The default value
     * @return          The long value
     */
    public static Long getLong(String key, Long dafault) {
        String value = get(key);
        if (value == null) {
            return dafault;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.warn("Cannot convert {} to an long.", value, e);
        }

        return dafault;
    }

    /**
     * Get a configuration value as Integer from one of the supported locations. If a value cannot be found, or if
     *      * the value is not a valid number, then null is returned.
     * <ul>
     *   <li>System Properties - Key is prepended with "optimizely."</li>
     *   <li>Environment variables - Key is prepended with "optimizely.", upper cased and "."s are replaced with "_"s.</li>
     *   <li>Optimizely Properties - Key is sourced as-is.</li>
     * </ul>
     *
     * @param key       The configuration key
     * @return          The integer value
     */

    public static Integer getInteger(String key) {
        return getInteger(key, null);
    }

    /**
     * Get a configuration value as Integer from one of the supported locations. If a value cannot be found, or if
     * the value is not a valid number, then the default is returned.
     * <ul>
     *   <li>System Properties - Key is prepended with "optimizely."</li>
     *   <li>Environment variables - Key is prepended with "optimizely.", upper cased and "."s are replaced with "_"s.</li>
     *   <li>Optimizely Properties - Key is sourced as-is.</li>
     * </ul>
     *
     * @param key       The configuration key
     * @param dafault   The default value
     * @return          The integer value
     */

    public static Integer getInteger(String key, Integer dafault) {
        String value = get(key);
        if (value == null) {
            return dafault;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Cannot convert {} to an integer.", value, e);
        }

        return dafault;
    }

    /**
     * Get a configuration value as Enum from one of the supported locations. Of not a valid enum value, then null is returned.
     * <ul>
     *   <li>System Properties - Key is prepended with "optimizely."</li>
     *   <li>Environment variables - Key is prepended with "optimizely.", upper cased and "."s are replaced with "_"s.</li>
     *   <li>Optimizely Properties - Key is sourced as-is.</li>
     * </ul>
     *
     * @param key       The configuration key
     * @param clazz     The value class
     * @param <T> This is the type parameter
     * @return          The value
     */
    public static <T> T getEnum(String key, Class<T> clazz) {
        return getEnum(key, clazz, null);
    }

    /**
     * Get a configuration value as Enum from one of the supported locations. If a value cannot be found, or if it
     * is not a valid enum value, then the default is returned.
     * <ul>
     *   <li>System Properties - Key is prepended with "optimizely."</li>
     *   <li>Environment variables - Key is prepended with "optimizely.", upper cased and "."s are replaced with "_"s.</li>
     *   <li>Optimizely Properties - Key is sourced as-is.</li>
     * </ul>
     *
     * @param key       The configuration key
     * @param clazz     The value class
     * @param dafault   The default value
     * @param <T> This is the type parameter
     * @return          The value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getEnum(String key, Class<T> clazz,  T dafault) {
        String value = get(key);
        if (value == null) {
            return dafault;
        }

        try {
            return (T)Enum.valueOf((Class<Enum>)clazz, value);
        } catch (Exception e) {
            logger.warn("Cannot convert {} to an integer.", value, e);
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
}
