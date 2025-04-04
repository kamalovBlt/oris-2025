package ru.itis.orm.entity;

import java.util.List;

public record EntityMetadata(
        String tableName,
        Class<?> entityClass,
        List<ColumnMetadata> columnsMetadata,
        List<ForeignKeyMetadata> foreignKeysMetadata,
        List<FetchMetadata> fetchMetadata
) {

}
