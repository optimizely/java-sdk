package com.optimizely.ab.odp.parser;

import com.optimizely.ab.internal.JsonParserProvider;
import com.optimizely.ab.internal.PropertyUtils;
import com.optimizely.ab.odp.parser.impl.GsonParser;
import com.optimizely.ab.odp.parser.impl.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResponseJsonParserFactoryTest {
    @Before
    @After
    public void clearParserSystemProperty() {
        PropertyUtils.clear("default_parser");
    }

    @Test
    public void getGsonParserWhenNoDefaultIsSet() {
        assertEquals(GsonParser.class, ResponseJsonParserFactory.getParser().getClass());
    }

    @Test
    public void getCorrectParserWhenValidDefaultIsProvided() {
        PropertyUtils.set("default_parser", "JSON_CONFIG_PARSER");
        assertEquals(JsonParser.class, ResponseJsonParserFactory.getParser().getClass());
    }

    @Test
    public void getGsonParserWhenGivenDefaultParserDoesNotExist() {
        PropertyUtils.set("default_parser", "GARBAGE_VALUE");
        assertEquals(GsonParser.class, ResponseJsonParserFactory.getParser().getClass());
    }
}