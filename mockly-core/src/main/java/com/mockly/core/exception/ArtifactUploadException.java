package com.mockly.core.exception;

/**
 * Exception thrown when artifact upload verification fails.
 */
public class ArtifactUploadException extends RuntimeException {

    public ArtifactUploadException(String message) {
        super(message);
    }

    public ArtifactUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}

