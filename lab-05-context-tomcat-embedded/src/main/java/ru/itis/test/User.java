package ru.itis.test;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

    private String username;
    private String password;

}
