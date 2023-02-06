package com.teamwork.animalshelter.exception;

public class ErrorMenu extends RuntimeException {
    public ErrorMenu(String name, String error) {
        super(String.format("%s. (Меню '%s')", error, name));
    }
}
