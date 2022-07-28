package com.optimizely.ab.odp.parser;

import com.optimizely.ab.odp.parser.ResponseJsonParser;
import static junit.framework.TestCase.assertEquals;

import com.optimizely.ab.odp.parser.impl.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

@RunWith(Parameterized.class)
public class ResponseJsonParserTest {
    private ResponseJsonParser jsonParser;

    public ResponseJsonParserTest(ResponseJsonParser jsonParser) {
        super();
        this.jsonParser = jsonParser;
    }

    @Parameterized.Parameters
    public static List<ResponseJsonParser> input() {
        return Arrays.asList(new GsonParser(), new JsonParser(), new JsonSimpleParser(), new JacksonParser());
    }

    @Test
    public void returnSegmentsListWhenResponseIsCorrect() {
        String responseToParse = "{\"data\":{\"customer\":{\"audiences\":{\"edges\":[{\"node\":{\"name\":\"has_email\",\"state\":\"qualified\"}},{\"node\":{\"name\":\"has_email_opted_in\",\"state\":\"qualified\"}}]}}}}";
        List<String> parsedSegments =  jsonParser.parseQualifiedSegments(responseToParse);
        assertEquals(Arrays.asList("has_email", "has_email_opted_in"), parsedSegments);
    }

    @Test
    public void excludeSegmentsWhenStateNotQualified() {
        String responseToParse = "{\"data\":{\"customer\":{\"audiences\":{\"edges\":[{\"node\":{\"name\":\"has_email\",\"state\":\"qualified\"}},{\"node\":{\"name\":\"has_email_opted_in\",\"state\":\"not_qualified\"}}]}}}}";
        List<String> parsedSegments =  jsonParser.parseQualifiedSegments(responseToParse);
        assertEquals(Arrays.asList("has_email"), parsedSegments);
    }

    @Test
    public void returnEmptyListWhenResponseHasEmptyArray() {
        String responseToParse = "{\"data\":{\"customer\":{\"audiences\":{\"edges\":[]}}}}";
        List<String> parsedSegments =  jsonParser.parseQualifiedSegments(responseToParse);
        assertEquals(Arrays.asList(), parsedSegments);
    }

    @Test
    public void returnNullWhenJsonFormatIsValidButUnexpectedData() {
        String responseToParse = "{\"data\"\"consumer\":{\"randomKey\":{\"edges\":[{\"node\":{\"name\":\"has_email\",\"state\":\"qualified\"}},{\"node\":{\"name\":\"has_email_opted_in\",\"state\":\"qualified\"}}]}}}}";
        List<String> parsedSegments =  jsonParser.parseQualifiedSegments(responseToParse);
        assertEquals(null, parsedSegments);
    }

    @Test
    public void returnNullWhenJsonIsMalformed() {
        String responseToParse = "{\"data\"\"customer\":{\"audiences\":{\"edges\":[{\"node\":{\"name\":\"has_email\",\"state\":\"qualified\"}},{\"node\":{\"name\":\"has_email_opted_in\",\"state\":\"qualified\"}}]}}}}";
        List<String> parsedSegments =  jsonParser.parseQualifiedSegments(responseToParse);
        assertEquals(null, parsedSegments);
    }
}
