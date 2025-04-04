package ru.itis.orm.entity;

public record ColumnMetadata(
        String fieldName,
        String columnName,
        boolean isUnique,
        boolean isId,
        boolean isNullable,
        String columnType) {
}
