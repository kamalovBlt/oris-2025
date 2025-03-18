package ru.itis.mvc.util;

public record RequestKey(String requestUri, HttpMethod httpMethod) {
}
