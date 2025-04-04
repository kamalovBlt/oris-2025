package ru.itis.orm.management.api;

import java.util.List;

public interface EntityManager {

    <T> T save(T entity);
    <T> T update(T entity);
    void remove(Object entity);
    <T> T find(Class<T> entityType, long id);
    <T> List<T> findAll(Class<T> entityType);

}
