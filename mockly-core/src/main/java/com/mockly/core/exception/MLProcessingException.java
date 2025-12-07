package com.mockly.core.exception;

/**
 * Exception thrown when ML service processing fails.
 */
public class MLProcessingException extends RuntimeException {

    public MLProcessingException(String message) {
        super(message);
    }

    public MLProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

