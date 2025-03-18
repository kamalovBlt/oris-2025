package ru.itis.mvc;

import lombok.Getter;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import ru.itis.context.AnnotationApplicationContext;
import ru.itis.mvc.service.ControllerService;

import java.io.File;

@Getter
public class WebMvcApplication {

    private final AnnotationApplicationContext applicationContext;

    public WebMvcApplication(String packageName) {
        this.applicationContext = new AnnotationApplicationContext(packageName);
    }

    public void run(String contextPath) {

        this.applicationContext.run();
        runTomcat(contextPath);


    }

    private void runTomcat(String contextPath) {

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir("temp");
        Connector connector = new Connector();
        connector.setPort(8080);
        tomcat.setConnector(connector);
        String docBase = new File(".").getAbsolutePath();
        Context tomcatContext = tomcat.addContext(contextPath, docBase);
        ControllerService controllerService = new ControllerService(this.applicationContext.getControllers());
        DispatcherServlet dispatcherServlet = new DispatcherServlet(controllerService);
        tomcat.addServlet(contextPath, "dispatcherServlet", dispatcherServlet);
        tomcatContext.addServletMappingDecoded("/", "dispatcherServlet");

        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }

    }

}
