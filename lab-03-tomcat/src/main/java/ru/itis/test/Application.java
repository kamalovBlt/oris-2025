package ru.itis.test;

import ru.itis.mvc.WebMvcApplication;

public class Application {

    public static void main(String[] args) {
        WebMvcApplication webMvcApplication = new WebMvcApplication("ru.itis.test");
        webMvcApplication.run("");
    }
}



