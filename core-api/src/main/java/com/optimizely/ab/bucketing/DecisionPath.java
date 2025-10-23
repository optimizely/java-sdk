package com.optimizely.ab.bucketing;

public enum DecisionPath {
    WITH_CMAB,      // Use CMAB logic
    WITHOUT_CMAB    // Skip CMAB logic (traditional A/B testing)
}
