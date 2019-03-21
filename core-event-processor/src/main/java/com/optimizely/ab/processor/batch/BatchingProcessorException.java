package com.optimizely.ab.processor.batch;

public class BatchingProcessorException extends RuntimeException {
    public BatchingProcessorException(Throwable cause) {
        super(cause);
    }

    public BatchingProcessorException(String message) {
        super(message);
    }

    public BatchingProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
