package com.mockly.core.exception;

public class TokenInvalidException extends UnauthorizedException {
    public TokenInvalidException() {
        super("Invalid or expired token");
    }

    public TokenInvalidException(String message) {
        super(message);
    }

    public TokenInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}

