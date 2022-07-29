/**
 *    Copyright 2022, Optimizely Inc. and contributors
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

import com.optimizely.ab.config.parser.MissingJsonParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum JsonParserProvider {
    GSON_CONFIG_PARSER("com.google.gson.Gson"),
    JACKSON_CONFIG_PARSER("com.fasterxml.jackson.databind.ObjectMapper" ),
    JSON_CONFIG_PARSER("org.json.JSONObject"),
    JSON_SIMPLE_CONFIG_PARSER("org.json.simple.JSONObject");

    private static final Logger logger = LoggerFactory.getLogger(JsonParserProvider.class);

    private final String className;

    JsonParserProvider(String className) {
        this.className = className;
    }

    private boolean isAvailable() {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static JsonParserProvider getDefaultParser() {
        String defaultParserName = PropertyUtils.get("default_parser");

        if (defaultParserName != null) {
            try {
                JsonParserProvider parser = JsonParserProvider.valueOf(defaultParserName);
                if (parser.isAvailable()) {
                    logger.debug("using json parser: {}, based on override config", parser.className);
                    return parser;
                }

                logger.warn("configured parser {} is not available in the classpath", defaultParserName);
            } catch (IllegalArgumentException e) {
                logger.warn("configured parser {} is not a valid value", defaultParserName);
            }
        }

        for (JsonParserProvider parser: JsonParserProvider.values()) {
            if (!parser.isAvailable()) {
                continue;
            }

            logger.info("using json parser: {}", parser.className);
            return parser;
        }

        throw new MissingJsonParserException("unable to locate a JSON parser. "
            + "Please see <link> for more information");
    }
}
