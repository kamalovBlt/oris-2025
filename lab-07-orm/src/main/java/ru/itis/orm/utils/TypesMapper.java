package ru.itis.orm.utils;

import java.util.HashMap;
import java.util.Map;

public class TypesMapper {

    private static final Map<Class<?>, String> javaToPostgresTypeMap = new HashMap<>();

    static {

        javaToPostgresTypeMap.put(Integer.class, "INTEGER");
        javaToPostgresTypeMap.put(Long.class, "BIGINT");
        javaToPostgresTypeMap.put(Double.class, "DOUBLE PRECISION");
        javaToPostgresTypeMap.put(Boolean.class, "BOOLEAN");
        javaToPostgresTypeMap.put(String.class, "TEXT");
        javaToPostgresTypeMap.put(java.util.Date.class, "TIMESTAMP");
        javaToPostgresTypeMap.put(java.time.LocalDateTime.class, "TIMESTAMP");
        javaToPostgresTypeMap.put(java.util.UUID.class, "UUID");
        javaToPostgresTypeMap.put(int.class, "INTEGER");
        javaToPostgresTypeMap.put(long.class, "BIGINT");
        javaToPostgresTypeMap.put(double.class, "DOUBLE PRECISION");
        javaToPostgresTypeMap.put(boolean.class, "BOOLEAN");
    }

    public static String mapJavaTypeToPostgres(Class<?> javaType) {
        String postgresType = javaToPostgresTypeMap.get(javaType);
        if (postgresType != null) {
            return postgresType;
        }
        throw new IllegalArgumentException("Unsupported Java type: " + javaType.getName());
    }
}
