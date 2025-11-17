package com.mockly.core.exception;

public class EmailAlreadyExistsException extends BadRequestException {
    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email);
    }

    public EmailAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

