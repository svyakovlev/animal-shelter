package com.teamwork.animalshelter.exception;

public class TemplateAlreadyExist extends RuntimeException {
    public TemplateAlreadyExist(String name) {
        super(String.format("Шаблон объекта с названием '%s' уже существует", name));
    }
}
