package ru.itis.orm.entity;

public record ForeignKeyMetadata(RelationType relationType, String targetTableName, String joinColumn) {}
