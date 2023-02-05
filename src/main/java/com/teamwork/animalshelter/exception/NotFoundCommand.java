package com.teamwork.animalshelter.exception;

public class NotFoundCommand extends RuntimeException {
    public NotFoundCommand(String command) {
        super(String.format("Команда '%s' не найдена", command));
    }
}
