package com.teamwork.animalshelter.exception;

public class NotFoundResource extends RuntimeException {
    public NotFoundResource(String name) {
        super(String.format("Не найден ресурс: '%s'", name));
    }
}
