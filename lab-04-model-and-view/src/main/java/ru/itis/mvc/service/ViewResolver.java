package ru.itis.mvc.service;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import ru.itis.mvc.model.ModelAndView;
import ru.itis.mvc.model.ModelMap;

import java.io.IOException;
import java.io.StringWriter;

public class ViewResolver {

    private final Configuration configuration;

    public ViewResolver(Configuration configuration) {
        this.configuration = configuration;
        configuration.setTemplateLoader(new ClassTemplateLoader(ViewResolver.class, "/"));
    }

    public String getResponse(ModelAndView modelAndView) {
        ModelMap modelMap = modelAndView.modelMap();
        String viewName = modelAndView.viewName();
        try {
            Template template = configuration.getTemplate(viewName + ".ftl");
            StringWriter writer = new StringWriter();
            template.process(modelMap.getModels(), writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e);
        }
    }
}
