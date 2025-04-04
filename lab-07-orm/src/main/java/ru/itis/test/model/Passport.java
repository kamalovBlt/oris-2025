package ru.itis.test.model;

import lombok.*;
import ru.itis.orm.annotations.Entity;
import ru.itis.orm.annotations.Id;
import ru.itis.orm.annotations.JoinColumn;
import ru.itis.orm.annotations.OneToOne;

@Entity(table = "passport")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Passport {

    @Id
    private long id;

    @OneToOne(targetEntityName = "user", joinColumn = "passport_id")
    @JoinColumn(name = "user_id")
    private User user;

}
