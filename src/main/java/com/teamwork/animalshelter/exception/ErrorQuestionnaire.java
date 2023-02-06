package com.teamwork.animalshelter.exception;

public class ErrorQuestionnaire extends RuntimeException {
    public ErrorQuestionnaire(String name, String error) {
        super(String.format("%s. (Опросник '%s')", error, name));
    }
}
