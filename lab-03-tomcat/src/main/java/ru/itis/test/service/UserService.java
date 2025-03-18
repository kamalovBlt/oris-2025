package ru.itis.test.service;

import ru.itis.context.Component;
import ru.itis.test.model.User;

@Component
public class UserService {

    public User getById(int id) {
        return User.builder()
                .id(id)
                .name("name=" + id)
                .password("password=" + id)
                .build();
    }

}
