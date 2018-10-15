package com.optimizely.ab.internal;

import com.optimizely.ab.OptimizelyRuntimeException;

public class AudienceConditionsAlreadyResolved extends OptimizelyRuntimeException {

    public AudienceConditionsAlreadyResolved(String message) {
        super(message);
    }

    public AudienceConditionsAlreadyResolved(String message, Throwable cause) {
        super(message, cause);
    }
}
