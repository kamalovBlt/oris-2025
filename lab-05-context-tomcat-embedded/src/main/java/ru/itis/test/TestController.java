package ru.itis.test;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import ru.itis.mvc.annotations.GetMapping;

@Controller
@RequiredArgsConstructor
public class TestController {

    private final UserService userService;

    @GetMapping("/users")
    public User getById(int id) {
        return userService.getById(id);
    }

}
