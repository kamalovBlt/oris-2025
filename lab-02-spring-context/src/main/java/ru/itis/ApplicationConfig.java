package ru.itis;

import org.springframework.context.annotation.Bean;

public class ApplicationConfig {

    @Bean
    public Car car(User user) {
        return new Car(user);
    }

    @Bean
    public User user(Car car) {
        return new User(car);
    }

}
