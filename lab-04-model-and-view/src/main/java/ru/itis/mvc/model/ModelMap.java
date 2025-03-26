package ru.itis.mvc.model;

import lombok.Getter;

import java.util.HashMap;

@Getter
public class ModelMap {

    private final HashMap<String, Object> models = new HashMap<>();

    public void addModel(String key, Object model) {
        models.put(key, model);
    }

}
