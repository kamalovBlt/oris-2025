package ru.itis.context;

import java.util.Map;

public interface ApplicationContext {

    Object getBean(String name);

    <T> T getBean(String name, Class<T> requiredType);

    void run();

    void close();

    Map<String, Object> getControllers();

}
