package ru.itis.test;

import lombok.RequiredArgsConstructor;
import ru.itis.context.Controller;
import ru.itis.mvc.annotations.GetMapping;
import ru.itis.mvc.annotations.RequestMapping;
import ru.itis.test.model.User;
import ru.itis.test.service.UserService;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public User getById(int id) {
        return userService.getById(id);
    }

}
