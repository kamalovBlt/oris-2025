package ru.itis.orm.management.impl;

import org.reflections.Reflections;
import org.reflections.scanners.*;
import ru.itis.orm.annotations.*;
import ru.itis.orm.entity.*;
import ru.itis.orm.management.api.EntityManager;
import ru.itis.orm.management.api.EntityManagerFactory;
import ru.itis.orm.utils.TypesMapper;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class SimpleEntityManagerFactory implements EntityManagerFactory {

    private final DataSource dataSource;
    private final String packageToScan;

    private final Map<String, EntityMetadata> entityMetadataMap = new HashMap<>();
    private final Map<String, SqlStatement> entitySqlStatementMap = new HashMap<>();
    private final Map<String, String> entityTableNameMap = new HashMap<>();

    public SimpleEntityManagerFactory(DataSource dataSource, String packageToScan) {
        this.dataSource = dataSource;
        this.packageToScan = packageToScan;
        scanPackageAndSaveTableName();
        scanPackageAndBuildMetadata();
        entityMetadataMap.forEach((k, v) -> System.out.println(k + " " + v));
        String createOrUpdateSql = createOrUpdate(entityMetadataMap);
        initializeSqlStatements();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(createOrUpdateSql)
        ) {
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public EntityManager createEntityManager() {

        try {
            return new SimpleEntityManager(this.dataSource.getConnection(), this.entitySqlStatementMap, this.entityMetadataMap);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private void scanPackageAndSaveTableName() {
        if (this.packageToScan == null) {
            return;
        }


        Reflections reflections = new Reflections(
                this.packageToScan,
                new SubTypesScanner(false),
                new TypeAnnotationsScanner()
        );
        Set<Class<?>> entityClasses = reflections.getTypesAnnotatedWith(Entity.class);
        entityClasses.forEach(entityClass -> {
            String tableName = entityClass.getAnnotation(Entity.class).table();
            this.entityTableNameMap.put(toLowerCaseFirstChar(entityClass.getSimpleName()), tableName);
        });

    }

    private void scanPackageAndBuildMetadata() {

        if (this.packageToScan == null) {
            return;
        }


        Reflections reflections = new Reflections(
                this.packageToScan,
                new SubTypesScanner(false),
                new TypeAnnotationsScanner()
        );
        Set<Class<?>> entityClasses = reflections.getTypesAnnotatedWith(Entity.class);


        for (Class<?> entityClass : entityClasses) {

            Entity entityClassAnnotation = entityClass.getAnnotation(Entity.class);

            Field[] fields = entityClass.getDeclaredFields();

            List<ColumnMetadata> columnMetadataList = new ArrayList<>();
            List<ForeignKeyMetadata> foreignKeyMetadataList = new ArrayList<>();
            List<FetchMetadata> fetchMetadata = new ArrayList<>();

            for (Field field : fields) {

                ColumnMetadata columnMetadata = null;

                if (field.isAnnotationPresent(Id.class)) {

                    columnMetadata = new ColumnMetadata(
                            toLowerCaseFirstChar(field.getName()),
                            toLowerCaseFirstChar(field.getName()),
                            true,
                            true,
                            true,
                            TypesMapper.mapJavaTypeToPostgres(field.getType())
                    );
                } else if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    columnMetadata = new ColumnMetadata(
                            toLowerCaseFirstChar(field.getName()),
                            column.name(),
                            false,
                            column.nullable(),
                            column.unique(),
                            TypesMapper.mapJavaTypeToPostgres(field.getType())
                    );


                }

                if (columnMetadata != null) {
                    columnMetadataList.add(columnMetadata);
                }

                checkRelation(field, foreignKeyMetadataList, fetchMetadata);


            }

            String entityName = toLowerCaseFirstChar(entityClass.getSimpleName());
            EntityMetadata entityMetadata = new EntityMetadata(
                    entityClassAnnotation.table(),
                    entityClass,
                    columnMetadataList,
                    foreignKeyMetadataList,
                    fetchMetadata
            );

            this.entityMetadataMap.put(entityName, entityMetadata);

        }

    }

    private void checkRelation(Field field,
                               List<ForeignKeyMetadata> foreignKeyMetadataList,
                               List<FetchMetadata> fetchMetadataList) {

        if (field.isAnnotationPresent(OneToOne.class) && field.isAnnotationPresent(JoinColumn.class)) {

            OneToOne oneToOne = field.getAnnotation(OneToOne.class);
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

            ForeignKeyMetadata foreignKeyMetadata = new ForeignKeyMetadata(
                    RelationType.ONE_TO_ONE,
                    entityTableNameMap.get(oneToOne.targetEntityName()),
                    joinColumn.name()
            );
            foreignKeyMetadataList.add(foreignKeyMetadata);
            FetchMetadata fetchMetadata = new FetchMetadata(
                    RelationType.ONE_TO_ONE,
                    entityTableNameMap.get(oneToOne.targetEntityName()),
                    oneToOne.joinColumn()
            );
            fetchMetadataList.add(fetchMetadata);
        } else if (field.isAnnotationPresent(OneToMany.class)) {
            OneToMany oneToMany = field.getAnnotation(OneToMany.class);
            FetchMetadata fetchMetadata = new FetchMetadata(
                    RelationType.ONE_TO_MANY,
                    this.entityTableNameMap.get(oneToMany.targetEntity()),
                    oneToMany.joinColumn()
            );
            fetchMetadataList.add(fetchMetadata);
        } else if (field.isAnnotationPresent(ManyToOne.class) && field.isAnnotationPresent(JoinColumn.class)) {
            ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            ForeignKeyMetadata foreignKeyMetadata = new ForeignKeyMetadata(
                    RelationType.MANY_TO_ONE,
                    this.entityTableNameMap.get(manyToOne.targetEntityName()),
                    joinColumn.name()
            );

            FetchMetadata fetchMetadata = new FetchMetadata(
                    RelationType.MANY_TO_ONE,
                    this.entityTableNameMap.get(manyToOne.targetEntityName()),
                    joinColumn.name()
            );
            foreignKeyMetadataList.add(foreignKeyMetadata);
            fetchMetadataList.add(fetchMetadata);

        }

    }

    private String toLowerCaseFirstChar(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toLowerCase() + input.substring(1);
    }

    private String createOrUpdate(Map<String, EntityMetadata> entityMetadataMap) {

        StringBuilder sql = new StringBuilder();

        for (Map.Entry<String, EntityMetadata> entry : entityMetadataMap.entrySet()) {

            EntityMetadata entityMetadata = entry.getValue();

            sql.append("CREATE TABLE IF NOT EXISTS ");
            sql.append(entityMetadata.tableName()).append("(");

            List<ColumnMetadata> columnsMetadata = entityMetadata.columnsMetadata();
            for (ColumnMetadata columnMetadata : columnsMetadata) {
                if (columnMetadata.isId()) {
                    sql.append("id SERIAL PRIMARY KEY,");
                } else {
                    sql.append(columnMetadata.columnName()).append(" ");
                    sql.append(columnMetadata.columnType()).append(" ");
                    if (!columnMetadata.isNullable()) {
                        sql.append("NOT NULL ");
                    }
                    if (columnMetadata.isUnique()) {
                        sql.append("UNIQUE ");
                    }
                    sql.append(",");
                }
            }

            for (ForeignKeyMetadata foreignKeyMetadata : entityMetadata.foreignKeysMetadata()) {
                sql.append(foreignKeyMetadata.joinColumn()).append(" INT,");
            }

            sql.deleteCharAt(sql.length() - 1);
            sql.append(");\n");

        }

        for (Map.Entry<String, EntityMetadata> entityMetadata : entityMetadataMap.entrySet()) {

            for (ForeignKeyMetadata foreignKeyMetadata : entityMetadata.getValue().foreignKeysMetadata()) {

                String foreignTableName = foreignKeyMetadata.targetTableName();



                sql.append("DO $$ BEGIN ")
                        .append("IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_")
                        .append(foreignTableName)
                        .append("') THEN ")
                        .append("ALTER TABLE ")
                        .append(entityMetadata.getValue().tableName())
                        .append(" ADD CONSTRAINT fk_")
                        .append(foreignTableName)
                        .append(" FOREIGN KEY (")
                        .append(foreignKeyMetadata.joinColumn())
                        .append(")")
                        .append(" REFERENCES ")
                        .append(foreignTableName)
                        .append("(id) ")
                        .append("ON DELETE CASCADE;")
                        .append("END IF; END $$;");

            }
        }


        return sql.toString();
    }

    private void initializeSqlStatements() {
        for (Map.Entry<String, EntityMetadata> entry : entityMetadataMap.entrySet()) {

            String entityName = entry.getKey();
            EntityMetadata entityMetadata = entry.getValue();
            String saveSqlStatement = initializeSaveSqlStatement(entityMetadata);
            String updateSqlStatement = initializeUpdateSqlStatement(entityMetadata);
            String findSqlStatement = initializeFindSqlStatement(entityMetadata);
            String findAllSqlStatement = initializeFindAllSqlStatement(entityMetadata);
            String deleteSqlStatement = initializeDeleteSqlStatement(entityMetadata);
            SqlStatement sql = new SqlStatement(saveSqlStatement, updateSqlStatement, findSqlStatement, findAllSqlStatement, deleteSqlStatement);
            this.entitySqlStatementMap.put(entityName, sql);
        }
    }

    private String initializeSaveSqlStatement(EntityMetadata entityMetadata) {
        StringBuilder saveSql = new StringBuilder("INSERT INTO ")
                .append(entityMetadata.tableName()).append(" (");

        List<ColumnMetadata> columnMetadataList = entityMetadata.columnsMetadata();
        List<ForeignKeyMetadata> foreignKeyMetadataList = entityMetadata.foreignKeysMetadata();

        for (ColumnMetadata columnMetadata : columnMetadataList) {
            if (!columnMetadata.isId()) {
                String columnName = columnMetadata.columnName();
                saveSql.append(columnName).append(",");
            }
        }
        for (ForeignKeyMetadata foreignKeyMetadata : foreignKeyMetadataList) {
            if (foreignKeyMetadata.relationType().equals(RelationType.ONE_TO_ONE)) {
                saveSql.append(foreignKeyMetadata.joinColumn()).append(",");
            }
            if (foreignKeyMetadata.relationType().equals(RelationType.MANY_TO_ONE)) {
                saveSql.append(foreignKeyMetadata.joinColumn()).append(",");
            }
        }
        saveSql.deleteCharAt(saveSql.length() - 1);
        saveSql.append(") VALUES (");

        int count = (int) columnMetadataList.stream().filter(x -> !x.isId()).count();
        count += foreignKeyMetadataList.size();
        saveSql.append("?,".repeat(count));
        saveSql.deleteCharAt(saveSql.length() - 1);
        saveSql.append(") RETURNING id;");
        return saveSql.toString();
    }

    private String initializeUpdateSqlStatement(EntityMetadata entityMetadata) {
        StringBuilder updateSql = new StringBuilder("UPDATE ")
                .append(entityMetadata.tableName())
                .append(" SET ");

        List<ColumnMetadata> columnMetadataList = entityMetadata.columnsMetadata();
        List<ForeignKeyMetadata> foreignKeyMetadataList = entityMetadata.foreignKeysMetadata();

        for (ColumnMetadata columnMetadata : columnMetadataList) {
            if (!columnMetadata.isId()) {
                String columnName = columnMetadata.columnName();
                updateSql.append(columnName).append(" = ?,");
            }
        }

        for (ForeignKeyMetadata foreignKeyMetadata : foreignKeyMetadataList) {
            if (foreignKeyMetadata.relationType().equals(RelationType.ONE_TO_ONE)) {
                updateSql.append(foreignKeyMetadata.joinColumn()).append(" = ?,");
            }
            if (foreignKeyMetadata.relationType().equals(RelationType.MANY_TO_ONE)) {
                updateSql.append(foreignKeyMetadata.joinColumn()).append(" = ?,");
            }
        }

        updateSql.deleteCharAt(updateSql.length() - 1);

        updateSql.append(" WHERE id = ?;");

        return updateSql.toString();
    }

    private String initializeFindSqlStatement(EntityMetadata entityMetadata) {
        StringBuilder findSql = new StringBuilder("SELECT * FROM ");
        findSql.append(entityMetadata.tableName());
        findSql.append(" WHERE ");
        findSql.append(entityMetadata.tableName()).append(".id=?;");
        return findSql.toString();
    }
    private String initializeFindAllSqlStatement(EntityMetadata entityMetadata) {
        return "SELECT * FROM %s ;".formatted(entityMetadata.tableName());
    }

    private String initializeDeleteSqlStatement(EntityMetadata entityMetadata) {
        return "DELETE FROM %s WHERE id=?;".formatted(entityMetadata.tableName());
    }

}
