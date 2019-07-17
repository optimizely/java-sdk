/**
 *
 *    Copyright 2016-2017, 2019, Optimizely and contributors
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
package com.optimizely.ab.config.parser;

import com.optimizely.ab.internal.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * Factory for generating {@link ConfigParser} instances, based on the json parser available on the classpath.
 */
public final class DefaultConfigParser {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConfigParser.class);

    private DefaultConfigParser() {
    }

    public static ConfigParser getInstance() {
        return LazyHolder.INSTANCE;
    }

    //======== Helper methods ========//

    public enum ConfigParserSupplier {
        // WARNING THESE MUST REMAIN LAMBDAS!!!
        // SWITCHING TO METHOD REFERENCES REQUIRES REQUIRES
        // ALL PARSERS IN THE CLASSPATH.
        GSON_CONFIG_PARSER("com.google.gson.Gson", () -> { return new GsonConfigParser(); }),
        JACKSON_CONFIG_PARSER("com.fasterxml.jackson.databind.ObjectMapper", () -> { return new JacksonConfigParser(); }),
        JSON_CONFIG_PARSER("org.json.JSONObject", () -> { return new JsonConfigParser(); }),
        JSON_SIMPLE_CONFIG_PARSER("org.json.simple.JSONObject", () -> { return new JsonSimpleConfigParser(); });

        private final String className;
        private final Supplier<ConfigParser> supplier;

        ConfigParserSupplier(String className, Supplier<ConfigParser> supplier) {
            this.className = className;
            this.supplier = supplier;
        }

        ConfigParser get() {
            return supplier.get();
        }

        private boolean isPresent() {
            try {
                Class.forName(className);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
    /**
     * Creates and returns a {@link ConfigParser} using a json parser available on the classpath.
     *
     * @return the created config parser
     * @throws MissingJsonParserException if there are no supported json parsers available on the classpath
     */
    private static @Nonnull
    ConfigParser create() {

        String configParserName = PropertyUtils.get("default_parser");

        if (configParserName != null) {
            try {
                ConfigParserSupplier supplier = ConfigParserSupplier.valueOf(configParserName);
                if (supplier.isPresent()) {
                    ConfigParser configParser = supplier.get();
                    logger.debug("using json parser: {}, based on override config", configParser.getClass().getSimpleName());
                    return configParser;
                }

                logger.warn("configured parser {} is not available in the classpath", configParserName);
            } catch (IllegalArgumentException e) {
                logger.warn("configured parser {} is not a valid value", configParserName);
            }
        }

        for (ConfigParserSupplier supplier: ConfigParserSupplier.values()) {
            if (!supplier.isPresent()) {
                continue;
            }

            ConfigParser configParser = supplier.get();
            logger.info("using json parser: {}", configParser.getClass().getSimpleName());
            return configParser;
        }

        throw new MissingJsonParserException("unable to locate a JSON parser. "
            + "Please see <link> for more information");
    }

   //======== Lazy-init Holder ========//

    private static class LazyHolder {
        private static final ConfigParser INSTANCE = create();
    }
}
