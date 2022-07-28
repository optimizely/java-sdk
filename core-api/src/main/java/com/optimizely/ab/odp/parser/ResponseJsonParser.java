package com.optimizely.ab.odp.parser;

import java.util.List;

public interface ResponseJsonParser {
    public List<String> parseQualifiedSegments(String responseJson);
}
