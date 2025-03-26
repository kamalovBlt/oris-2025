package ru.itis.test;

import ru.itis.context.Controller;
import ru.itis.mvc.annotations.GetMapping;
import ru.itis.mvc.annotations.RequestMapping;
import ru.itis.mvc.model.ModelAndView;
import ru.itis.mvc.model.ModelMap;

@Controller
@RequestMapping("/users")
public class UserController {

    @GetMapping
    public ModelAndView getById() {
        ModelMap modelMap = new ModelMap();
        User user = User.builder()
                .name("BULAT")
                .build();
        modelMap.addModel("user", user);
        return new ModelAndView(modelMap, "user");
    }

}
