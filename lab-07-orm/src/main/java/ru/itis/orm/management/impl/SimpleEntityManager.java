package ru.itis.orm.management.impl;

import ru.itis.orm.annotations.*;
import ru.itis.orm.entity.*;
import ru.itis.orm.management.api.EntityManager;
import ru.itis.orm.management.exception.NotEntityException;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public class SimpleEntityManager implements EntityManager {

    private final Connection connection;
    private final Map<String, SqlStatement> sqlStatements;
    private final Map<String, EntityMetadata> entityMetadataMap;

    public SimpleEntityManager(Connection connection, Map<String, SqlStatement> sqlStatements, Map<String, EntityMetadata> entityMetadataMap) {
        this.connection = connection;
        this.sqlStatements = sqlStatements;
        this.entityMetadataMap = entityMetadataMap;
    }

    @Override
    public <T> T save(T entity) {

        Class<?> entityClass = entity.getClass();
        validate(entityClass);

        String entityName = toLowerCaseFirstChar(entityClass.getSimpleName());

        SqlStatement sqlStatement = sqlStatements.get(entityName);

        if (sqlStatement == null) {
            throw new RuntimeException("Cannot find SQL statement for entity " + entityName);
        }

        String saveSql = sqlStatement.save();

        List<Object> oneToOneSavedObjects = new ArrayList<>();
        List<Object> oneToManyToSaveObjects = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(saveSql)) {

            int parameterIndex = 1;
            Field[] declaredFields = entityClass.getDeclaredFields();

            for (Field field : declaredFields) {
                field.setAccessible(true);

                try {
                    if (field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(Id.class)) {

                        Object value = field.get(entity);
                        preparedStatement.setObject(parameterIndex++, value);
                    } else if (field.isAnnotationPresent(OneToOne.class)) {
                        Object value = field.get(entity);
                        if (value == null) {
                            preparedStatement.setNull(parameterIndex++, java.sql.Types.NULL);
                            continue;
                        }

                        Object savedObject = save(value);
                        long id = getId(savedObject);
                        preparedStatement.setObject(parameterIndex++, id);
                        oneToOneSavedObjects.add(savedObject);
                    } else if (field.isAnnotationPresent(ManyToOne.class)) {
                        Object value = field.get(entity);
                        try {
                            long id = getId(value);
                            preparedStatement.setObject(parameterIndex++, id);
                        } catch (NotEntityException e) {
                            preparedStatement.setNull(parameterIndex++, java.sql.Types.NULL);
                        }

                    } else if (field.isAnnotationPresent(OneToMany.class)) {
                        List<?> value = (List<?>) field.get(entity);
                        oneToManyToSaveObjects.addAll(value);
                    }

                } catch (IllegalAccessException e) {

                    throw new RuntimeException(e);
                }
                field.setAccessible(false);
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    long generatedId = resultSet.getLong("id");

                    for (Field field : declaredFields) {
                        if (field.isAnnotationPresent(Id.class)) {
                            field.setAccessible(true);
                            field.set(entity, generatedId);
                            field.setAccessible(false);
                            break;
                        }
                    }

                    for (Object savedObject : oneToOneSavedObjects) {
                        Field declaredField = savedObject.getClass()
                                .getDeclaredField(toLowerCaseFirstChar(entity.getClass().getSimpleName()));
                        declaredField.setAccessible(true);
                        declaredField.set(savedObject, entity);
                        declaredField.setAccessible(false);
                        update(savedObject);
                    }

                    for (Object toSaveObject : oneToManyToSaveObjects) {
                        Field declaredField = toSaveObject.getClass()
                                .getDeclaredField(toLowerCaseFirstChar(entity.getClass().getSimpleName()));
                        declaredField.setAccessible(true);
                        declaredField.set(toSaveObject, entity);
                        declaredField.setAccessible(false);
                        save(toSaveObject);
                    }


                }

            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    @Override
    public <T> T update(T entity) {
        Class<?> entityClass = entity.getClass();
        validate(entityClass);
        String entityName = toLowerCaseFirstChar(entityClass.getSimpleName());

        SqlStatement sqlStatement = sqlStatements.get(entityName);
        String updateSql = sqlStatement.update();
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {
            int parameterIndex = 1;
            for (Field field : entityClass.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Column.class)) {
                    preparedStatement.setObject(parameterIndex++, field.get(entity));
                } else if (field.isAnnotationPresent(OneToOne.class)) {
                    Object object = field.get(entity);
                    if (object == null) {
                        preparedStatement.setNull(parameterIndex++, java.sql.Types.NULL);
                        continue;
                    }
                    preparedStatement.setObject(parameterIndex++, getId(object));
                }


            }

            preparedStatement.setObject(parameterIndex, getId(entity));

            preparedStatement.execute();

        } catch (SQLException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return entity;

    }

    @Override
    public void remove(Object entity) {
        SqlStatement sqlStatement = sqlStatements.get(
                toLowerCaseFirstChar(entity.getClass().getSimpleName())
        );
        String removeSql = sqlStatement.remove();
        try (PreparedStatement preparedStatement = connection.prepareStatement(removeSql)) {

            preparedStatement.setObject(1, getId(entity));
            preparedStatement.execute();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private final Map<Class<?>, Map<Long, Object>> cache = new HashMap<>();

    @Override
    public <T> T find(Class<T> entityType, long id) {

        if (cache.containsKey(entityType) && cache.get(entityType).containsKey(id)) {
            return (T) cache.get(entityType).get(id);
        }

        SqlStatement sqlStatement = sqlStatements.get(toLowerCaseFirstChar(entityType.getSimpleName()));
        String findSql = sqlStatement.find();

        try (PreparedStatement preparedStatement = connection.prepareStatement(findSql)) {
            preparedStatement.setLong(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                return null;
            }

            int parameterIndex = 1;
            Constructor<?> declaredConstructor = entityType.getDeclaredConstructor();
            Object object = declaredConstructor.newInstance();

            cache.computeIfAbsent(entityType, k -> new HashMap<>()).put(id, object);

            Field[] declaredFields = entityType.getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(Column.class)) {
                    field.set(object, resultSet.getObject(parameterIndex++));
                }
            }

            EntityMetadata entityMetadata = entityMetadataMap.get(toLowerCaseFirstChar(entityType.getSimpleName()));
            List<FetchMetadata> fetchMetadataList = entityMetadata.fetchMetadata();
            for (FetchMetadata fetchMetadata : fetchMetadataList) {
                if (fetchMetadata.relationType().equals(RelationType.ONE_TO_ONE)) {
                    String entityName = getEntityName(fetchMetadata.tableName());
                    Class<?> oneToOneEntityClass = entityMetadataMap.get(entityName).entityClass();
                    long oneToOneEntityId = resultSet.getLong(parameterIndex++);

                    if (oneToOneEntityId != 0) {
                        Object relatedEntity = find(oneToOneEntityClass, oneToOneEntityId);
                        for (Field field : declaredFields) {
                            if (field.getType().equals(oneToOneEntityClass)) {
                                field.set(object, relatedEntity);
                                break;
                            }
                        }
                    }
                } else if (fetchMetadata.relationType().equals(RelationType.ONE_TO_MANY)) {

                    String entityName = getEntityName(fetchMetadata.tableName());
                    Class<?> oneToManyEntityClass = entityMetadataMap.get(entityName).entityClass();

                    String oneToManySql = "SELECT * FROM " + fetchMetadata.tableName() + " WHERE " + toLowerCaseFirstChar(entityType.getSimpleName()) + "_id = ?";
                    System.out.println(oneToManySql);
                    try (PreparedStatement oneToManyStmt = connection.prepareStatement(oneToManySql)) {
                        oneToManyStmt.setLong(1, id);
                        ResultSet oneToManyResultSet = oneToManyStmt.executeQuery();

                        List<Object> children = new ArrayList<>();
                        while (oneToManyResultSet.next()) {
                            long childId = oneToManyResultSet.getLong(fetchMetadata.id());
                            Object childEntity = find(oneToManyEntityClass, childId);
                            children.add(childEntity);
                        }

                        for (Field field : declaredFields) {
                            if (field.getType().equals(List.class)) {
                                field.setAccessible(true);
                                field.set(object, children);
                                break;
                            }
                        }
                    }
                } else if (fetchMetadata.relationType().equals(RelationType.MANY_TO_ONE)) {
                    String entityName = getEntityName(fetchMetadata.tableName());
                    Class<?> manyToOneEntityClass = entityMetadataMap.get(entityName).entityClass();

                    long parentId = resultSet.getLong(parameterIndex++);

                    if (parentId != 0) {
                        Object parentEntity = find(manyToOneEntityClass, parentId);

                        for (Field field : declaredFields) {
                            if (field.getType().equals(manyToOneEntityClass)) {
                                field.setAccessible(true);
                                field.set(object, parentEntity);
                                break;
                            }
                        }
                    }
                }
            }

            return (T) object;

        } catch (SQLException | IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> entityType) {
        List<T> results = new ArrayList<>();

        SqlStatement sqlStatement = sqlStatements.get(toLowerCaseFirstChar(entityType.getSimpleName()));
        String findAllSql = sqlStatement.findAll();

        try (PreparedStatement preparedStatement = connection.prepareStatement(findAllSql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                results.add(find(entityType, id));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return results;
    }


    private void validate(Class<?> entityType) {

        if (!entityType.isAnnotationPresent(Entity.class)) {
            throw new NotEntityException(entityType.getSimpleName() + " is not an entity");
        }

    }

    private String toLowerCaseFirstChar(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toLowerCase() + input.substring(1);
    }

    private long getId(Object entity) {
        for (Field objectField : entity.getClass().getDeclaredFields()) {
            if (objectField.isAnnotationPresent(Id.class)) {
                objectField.setAccessible(true);
                try {
                    return (long) objectField.get(entity);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new NotEntityException(entity.getClass().getSimpleName() + " is not have id");
    }

    private String getEntityName(String tableName) {
        for (Map.Entry<String, EntityMetadata> entry : this.entityMetadataMap.entrySet()) {
            EntityMetadata entityMetadata = entry.getValue();
            if (entityMetadata.tableName().equals(tableName)) {
                return entry.getKey();
            }
        }
        return null;
    }

}
