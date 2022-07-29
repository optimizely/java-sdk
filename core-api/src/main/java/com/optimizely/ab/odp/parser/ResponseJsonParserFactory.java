package com.optimizely.ab.odp.parser;

import com.optimizely.ab.internal.JsonParserProvider;
import com.optimizely.ab.odp.parser.impl.GsonParser;
import com.optimizely.ab.odp.parser.impl.JacksonParser;
import com.optimizely.ab.odp.parser.impl.JsonParser;
import com.optimizely.ab.odp.parser.impl.JsonSimpleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseJsonParserFactory {
    private static final Logger logger = LoggerFactory.getLogger(ResponseJsonParserFactory.class);

    public static ResponseJsonParser getParser() {
        JsonParserProvider parserProvider = JsonParserProvider.getDefaultParser();
        ResponseJsonParser jsonParser = null;
        switch (parserProvider) {
            case GSON_CONFIG_PARSER:
                jsonParser = new GsonParser();
                break;
            case JACKSON_CONFIG_PARSER:
                jsonParser = new JacksonParser();
                break;
            case JSON_CONFIG_PARSER:
                jsonParser = new JsonParser();
                break;
            case JSON_SIMPLE_CONFIG_PARSER:
                jsonParser = new JsonSimpleParser();
                break;
        }
        logger.info("Using " + parserProvider.toString() + " parser");
        return jsonParser;
    }
}
