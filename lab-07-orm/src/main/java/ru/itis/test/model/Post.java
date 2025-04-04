package ru.itis.test.model;

import lombok.*;
import ru.itis.orm.annotations.Entity;
import ru.itis.orm.annotations.Id;
import ru.itis.orm.annotations.JoinColumn;
import ru.itis.orm.annotations.ManyToOne;

@Entity(table = "post")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Post {

    @Id
    private long id;

    @ManyToOne(targetEntityName = "user")
    @JoinColumn(name = "user_id")
    private User user;

}
