package ru.itis.mvc;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.itis.mvc.service.ControllerService;
import ru.itis.mvc.util.HttpMethod;
import ru.itis.mvc.util.RequestKey;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.stream.Collectors;

public class DispatcherServlet extends HttpServlet {

    private final ControllerService controllerService;

    public DispatcherServlet(ControllerService controllerService) {
        this.controllerService = controllerService;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String httpMethod = req.getMethod();
        String object;
        Map<String, String[]> parameterMap = req.getParameterMap();
        Map<String, String> collect = parameterMap.entrySet().stream()
                .filter(entry -> entry.getValue().length > 0)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()[0]
                ));

        RequestKey requestKey = new RequestKey(req.getRequestURI(), HttpMethod.valueOf(httpMethod.toUpperCase()));

        object = controllerService.invokeControllerMethod(requestKey, collect);

        PrintWriter writer = resp.getWriter();
        writer.write(object);

    }


}
