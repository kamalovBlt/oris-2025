package ru.itis.test;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    public User getById(int id) {
        return new User("test1", "test2");
    }

}
