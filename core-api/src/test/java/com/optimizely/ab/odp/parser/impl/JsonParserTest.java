package com.optimizely.ab.odp.parser.impl;

import com.optimizely.ab.odp.parser.ResponseJsonParser;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class JsonParserTest extends TestCase {
    @Test
    public void test1() {
        String responseToParse = "{\"data\":{\"customer\":{\"audiences\":{\"edges\":[{\"node\":{\"name\":\"has_email\",\"state\":\"qualified\"}},{\"node\":{\"name\":\"has_email_opted_in\",\"state\":\"qualified\"}}]}}}}";
        ResponseJsonParser jsonParser = new JsonParser();
        List<String> parsedSegments =  jsonParser.parseQualifiedSegments(responseToParse);
        assertEquals(Arrays.asList("has_email", "has_email_opted_in"), parsedSegments);
    }
}
