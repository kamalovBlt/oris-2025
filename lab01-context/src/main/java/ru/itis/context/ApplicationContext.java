package ru.itis.context;

import java.io.File;
import java.util.List;
import java.util.Objects;

public interface ApplicationContext {

    Object getBean(String name);

    <T> T getBean(String name, Class<T> requiredType);

    void run();

    void close();

}
