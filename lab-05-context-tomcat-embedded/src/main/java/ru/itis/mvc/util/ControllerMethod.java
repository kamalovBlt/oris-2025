package ru.itis.mvc.util;

import java.lang.reflect.Method;

public record ControllerMethod(Object controller, Method method) {
}
