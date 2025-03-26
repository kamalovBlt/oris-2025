package ru.itis.mvc.exception;

import ru.itis.mvc.util.RequestKey;

public class NoFoundSuchControllerMethodException extends RuntimeException {
    public NoFoundSuchControllerMethodException(RequestKey requestKey) {
        super(requestKey.toString() + " not found");
    }
}
