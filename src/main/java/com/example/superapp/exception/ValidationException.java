package com.example.superapp.exception;

public class ValidationException extends RuntimeException {

    private final String messageKey;

    public ValidationException(String messageKey, String message) {
        super(message);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
