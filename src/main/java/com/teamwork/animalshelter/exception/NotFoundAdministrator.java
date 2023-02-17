package com.teamwork.animalshelter.exception;

public class NotFoundAdministrator extends RuntimeException {
    public NotFoundAdministrator() {
        super("В базе не найдено ни одного администратора");
    }
}
