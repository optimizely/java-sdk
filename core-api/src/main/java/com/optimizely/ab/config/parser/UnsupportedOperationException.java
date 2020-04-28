package com.optimizely.ab.config.parser;

/**
 * Wrapper around all types of JSON parser exceptions.
 */
public final class UnsupportedOperationException extends Exception {
    public UnsupportedOperationException(String message) {
        super(message);
    }

    public UnsupportedOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
