package ru.itis.service.api;

import ru.itis.model.Student;

import java.util.List;

public interface StudentService {

    Student findById(int id);
    List<Student> findAll();
    Student findByName(String name);
    Student save(Student student);
    void delete(Student student);
    Student update(Student student);

}
