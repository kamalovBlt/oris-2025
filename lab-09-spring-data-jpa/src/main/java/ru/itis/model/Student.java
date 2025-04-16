package ru.itis.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Student extends Person {

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private Set<Subject> subjects;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private Group group;

}
