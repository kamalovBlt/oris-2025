package ru.itis.orm.entity;

public record SqlStatement(String save, String update, String find, String findAll, String remove) {
}
