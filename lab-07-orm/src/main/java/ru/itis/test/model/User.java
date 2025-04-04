package ru.itis.test.model;

import lombok.*;
import ru.itis.orm.annotations.*;

import java.util.List;

@Entity(table = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

    @Id
    private long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @OneToOne(targetEntityName = "passport", joinColumn = "user_id")
    @JoinColumn(name = "passport_id")
    private Passport passport;

    @OneToMany(targetEntity = "post", joinColumn = "user_id")
    private List<Post> posts;

}
