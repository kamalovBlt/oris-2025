package ru.itis.app;

import ru.itis.context.AnnotationApplicationContext;

public class Application {

    public static void main(String[] args) {
        AnnotationApplicationContext annotationApplicationContext = new AnnotationApplicationContext("ru.itis");
        annotationApplicationContext.run();
        Application bean = annotationApplicationContext.getBean("application", Application.class);
    }

}
