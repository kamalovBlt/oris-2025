package ru.itis.mvc;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.itis.mvc.model.ModelAndView;
import ru.itis.mvc.service.ControllerService;
import ru.itis.mvc.model.HttpMethod;
import ru.itis.mvc.model.RequestKey;
import ru.itis.mvc.service.ViewResolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.stream.Collectors;

public class DispatcherServlet extends HttpServlet {

    private final ControllerService controllerService;
    private final ViewResolver viewResolver;

    public DispatcherServlet(ControllerService controllerService, ViewResolver viewResolver) {
        this.controllerService = controllerService;
        this.viewResolver = viewResolver;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String httpMethod = req.getMethod();
        Map<String, String[]> parameterMap = req.getParameterMap();
        Map<String, String> collect = parameterMap.entrySet().stream()
                .filter(entry -> entry.getValue().length > 0)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()[0]
                ));

        RequestKey requestKey = new RequestKey(req.getRequestURI(), HttpMethod.valueOf(httpMethod.toUpperCase()));

        Object object = this.controllerService.invokeControllerMethod(requestKey, collect);

        if (object instanceof ModelAndView modelAndView) {
            resp.setContentType("text/html");
            resp.setCharacterEncoding("UTF-8");
            PrintWriter writer = resp.getWriter();
            String response = this.viewResolver.getResponse(modelAndView);
            writer.write(response);
        }

    }


}
