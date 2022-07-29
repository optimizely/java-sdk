package com.optimizely.ab.odp;

import java.util.List;

public interface ODPApiManager {
    String fetchQualifiedSegments(String apiKey, String apiEndpoint, String userKey, String userValue, List<String> segmentsToCheck);
}
