package ru.itis.mvc.model;

import java.lang.reflect.Method;

public record ControllerMethod(Object controller, Method method) {
}
