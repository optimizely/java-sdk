package com.optimizely.ab.event.internal.serializer;

import com.optimizely.ab.OptimizelyRuntimeException;

/**
 * Wrapper around all types of JSON serialization exceptions.
 */
public class SerializationException extends OptimizelyRuntimeException{

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
