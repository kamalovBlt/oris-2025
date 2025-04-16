package ru.itis;

import jakarta.transaction.Transactional;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.itis.config.DbConfig;
import ru.itis.model.Group;
import ru.itis.model.Lecturer;
import ru.itis.model.Student;
import ru.itis.model.Subject;
import ru.itis.service.api.StudentService;

import java.util.Set;

public class Main {
    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DbConfig.class);
        StudentService studentService = context.getBean(StudentService.class);



        Group group = Group.builder()
                .number("11-301")
                .build();

        Lecturer lecturer1 = Lecturer.builder()
                .name("lecturer1")
                .build();

        Lecturer lecturer2 = Lecturer.builder()
                .name("lecturer2")
                .build();

        Subject subject = Subject.builder()
                .name("oris")
                .lecturers(Set.of(lecturer1, lecturer2))
                .build();

        Student student = Student.builder()
                .name("student1")
                .group(group)
                .subjects(Set.of(subject))
                .build();

        Main main = new Main();
        Student findedStudent = main.test(student, studentService);
        Student findedByNameStudent = main.test2(studentService);
        System.out.println();

    }

    @Transactional
    public Student test(Student student, StudentService studentService) {

        Student save = studentService.save(student);
        student = studentService.findById(save.getId());
        return student;

    }

    @Transactional
    public Student test2(StudentService studentService) {
        return studentService.findByName("student1");
    }

}
