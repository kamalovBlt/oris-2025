package ru.itis.mvc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.itis.mvc.annotations.GetMapping;
import ru.itis.mvc.annotations.PostMapping;
import ru.itis.mvc.annotations.RequestMapping;
import ru.itis.mvc.exception.NoFoundSuchControllerMethodException;
import ru.itis.mvc.util.ControllerMethod;
import ru.itis.mvc.util.HttpMethod;
import ru.itis.mvc.util.RequestKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class ControllerService {

    private final Map<RequestKey, ControllerMethod> methods;
    private final ObjectMapper objectMapper;

    public ControllerService(Map<String, Object> controllers) {
        this.methods = new HashMap<>();
        this.objectMapper = new ObjectMapper();
        registerControllerMethods(controllers);
    }

    public String invokeControllerMethod(RequestKey requestKey, Map<String, String> parameters) {
        ControllerMethod controllerMethod = this.methods.get(requestKey);
        if (controllerMethod == null) {
            throw new NoFoundSuchControllerMethodException(requestKey);
        }
        Method method = controllerMethod.method();
        Object controller = controllerMethod.controller();
        method.setAccessible(true);

        Parameter[] methodParameters = method.getParameters();

        Object[] methodArgs = new Object[methodParameters.length];

        for (int i = 0; i < methodParameters.length; i++) {

            Parameter methodParam = methodParameters[i];
            String paramName = methodParam.getName();
            String paramValue = parameters.get(paramName);

            if (paramValue == null) {
                throw new IllegalArgumentException("Отсутствует обязательный параметр: " + paramName);
            }

            methodArgs[i] = convertParameter(paramValue, methodParam.getType());
        }

        try {
            Object returnedObject = method.invoke(controller, methodArgs);
            return this.objectMapper.writeValueAsString(returnedObject);
        } catch (IllegalAccessException | InvocationTargetException | JsonProcessingException e) {
            throw new RuntimeException("Ошибка при вызове метода контроллера: " + method.getName(), e);
        }
    }

    private void registerControllerMethods(Map<String, Object> controllers) {

        for (Object controller : controllers.values()) {
            Class<?> controllerClass = controller.getClass();
            StringBuilder requestURI;
            if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
                requestURI = new StringBuilder(requestMapping.value());
            }
            else {
                requestURI = new StringBuilder("/");
            }

            Method[] actualControllerClassMethods = controllerClass.getDeclaredMethods();
            for (Method actualControllerClassMethod : actualControllerClassMethods) {
                if (actualControllerClassMethod.isAnnotationPresent(GetMapping.class)) {
                    GetMapping getMapping = actualControllerClassMethod.getAnnotation(GetMapping.class);
                    requestURI.append(getMapping.value());
                    RequestKey requestKey = new RequestKey(requestURI.toString(), HttpMethod.GET);
                    this.methods.put(requestKey, new ControllerMethod(controller, actualControllerClassMethod));
                } else if (actualControllerClassMethod.isAnnotationPresent(RequestMapping.class)) {
                    PostMapping postMapping = actualControllerClassMethod.getAnnotation(PostMapping.class);
                    requestURI.append(postMapping.value());
                    RequestKey requestKey = new RequestKey(requestURI.toString(), HttpMethod.POST);
                    this.methods.put(requestKey, new ControllerMethod(controller, actualControllerClassMethod));
                }
            }
        }
    }

    private Object convertParameter(String paramValue, Class<?> paramType) {

        if (paramType == String.class) {
            return paramValue;
        } else if (paramType == int.class || paramType == Integer.class) {
            return Integer.parseInt(paramValue);
        } else if (paramType == long.class || paramType == Long.class) {
            return Long.parseLong(paramValue);
        } else if (paramType == double.class || paramType == Double.class) {
            return Double.parseDouble(paramValue);
        }

        throw new IllegalArgumentException("Неизвестный тип параметра: " + paramType.getName());
    }

}
