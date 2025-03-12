package ru.itis.context.exception;

public class NoFoundPublicConstructorException extends RuntimeException {
    public NoFoundPublicConstructorException(String message) {
        super(message);
    }
}
