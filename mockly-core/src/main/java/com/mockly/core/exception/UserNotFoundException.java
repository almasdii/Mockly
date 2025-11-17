package com.mockly.core.exception;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String identifier) {
        super("User not found: " + identifier);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

