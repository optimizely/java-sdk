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
package com.optimizely.ab.odp.parser;

import ch.qos.logback.classic.Level;
import com.optimizely.ab.internal.LogbackVerifier;
import static junit.framework.TestCase.assertEquals;

import com.optimizely.ab.odp.parser.impl.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

@RunWith(Parameterized.class)
public class ResponseJsonParserTest {
    private final ResponseJsonParser jsonParser;

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

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
        List<String> parsedSegments = jsonParser.parseQualifiedSegments(responseToParse);
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
        logbackVerifier.expectMessage(Level.ERROR, "Error parsing qualified segments from response");
        assertEquals(null, parsedSegments);
    }

    @Test
    public void returnNullWhenJsonIsMalformed() {
        String responseToParse = "{\"data\"\"customer\":{\"audiences\":{\"edges\":[{\"node\":{\"name\":\"has_email\",\"state\":\"qualified\"}},{\"node\":{\"name\":\"has_email_opted_in\",\"state\":\"qualified\"}}]}}}}";
        List<String> parsedSegments =  jsonParser.parseQualifiedSegments(responseToParse);
        logbackVerifier.expectMessage(Level.ERROR, "Error parsing qualified segments from response");
        assertEquals(null, parsedSegments);
    }

    @Test
    public void returnNullAndLogCorrectErrorWhenErrorResponseIsReturned() {
        String responseToParse = "{\"errors\":[{\"message\":\"Exception while fetching data (/customer) : java.lang.RuntimeException: could not resolve _fs_user_id = wrong_id\",\"locations\":[{\"line\":2,\"column\":3}],\"path\":[\"customer\"],\"extensions\":{\"classification\":\"InvalidIdentifierException\"}}],\"data\":{\"customer\":null}}";
        List<String> parsedSegments =  jsonParser.parseQualifiedSegments(responseToParse);
        logbackVerifier.expectMessage(Level.ERROR, "Exception while fetching data (/customer) : java.lang.RuntimeException: could not resolve _fs_user_id = wrong_id");
        assertEquals(null, parsedSegments);
    }
}
