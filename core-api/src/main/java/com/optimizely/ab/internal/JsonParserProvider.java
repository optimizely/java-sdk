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
