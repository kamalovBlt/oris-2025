package ru.itis.orm.management.exception;

public class NotEntityException extends RuntimeException {
    public NotEntityException(String message) {
        super(message);
    }
}
