package com.optimizely.ab.internal;

import com.optimizely.ab.OptimizelyRuntimeException;

public class InvalidAudienceCondition  extends OptimizelyRuntimeException {

    public InvalidAudienceCondition(String message) {
        super(message);
    }

    public InvalidAudienceCondition(String message, Throwable cause) {
        super(message, cause);
    }
}
