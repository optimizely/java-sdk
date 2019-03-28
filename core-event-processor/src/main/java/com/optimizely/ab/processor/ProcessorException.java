package com.optimizely.ab.processor;

public class ProcessorException extends RuntimeException {
    public ProcessorException(Throwable cause) {
        super(cause);
    }

    public ProcessorException(String message) {
        super(message);
    }

    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
