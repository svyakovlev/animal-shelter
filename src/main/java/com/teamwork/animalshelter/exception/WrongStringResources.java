package com.teamwork.animalshelter.exception;

public class WrongStringResources extends RuntimeException {
    public WrongStringResources(String resources) {
        super("Неверно указана строка ресурсов в файле свойств:  " + resources);
    }
}
