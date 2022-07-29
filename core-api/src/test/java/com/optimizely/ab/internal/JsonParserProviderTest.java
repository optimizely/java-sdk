package com.optimizely.ab.internal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class JsonParserProviderTest {
    @Before
    @After
    public void clearParserSystemProperty() {
        PropertyUtils.clear("default_parser");
    }

    @Test
    public void getGsonParserProviderWhenNoDefaultIsSet() {
        assertEquals(JsonParserProvider.GSON_CONFIG_PARSER, JsonParserProvider.getDefaultParser());
    }

    @Test
    public void getCorrectParserProviderWhenValidDefaultIsProvided() {
        PropertyUtils.set("default_parser", "JSON_CONFIG_PARSER");
        assertEquals(JsonParserProvider.JSON_CONFIG_PARSER, JsonParserProvider.getDefaultParser());
    }

    @Test
    public void getGsonParserWhenProvidedDefaultParserDoesNotExist() {
        PropertyUtils.set("default_parser", "GARBAGE_VALUE");
        assertEquals(JsonParserProvider.GSON_CONFIG_PARSER, JsonParserProvider.getDefaultParser());
    }
}