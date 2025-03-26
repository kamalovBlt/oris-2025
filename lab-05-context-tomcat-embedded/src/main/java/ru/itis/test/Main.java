package ru.itis.test;

import ru.itis.mvc.WebMvcApplication;

public class Main {
    public static void main(String[] args) {
        WebMvcApplication webMvcApplication = new WebMvcApplication(Config.class);
        webMvcApplication.run("");
    }
}
