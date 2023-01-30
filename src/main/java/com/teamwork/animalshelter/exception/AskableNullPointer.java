package com.teamwork.animalshelter.exception;

import com.teamwork.animalshelter.action.Askable;

/**
 * Исключение вызывается, если не удастся получить объект {@link Askable}
 */
public class AskableNullPointer extends RuntimeException {
    public AskableNullPointer(String name) {
        super(String.format("Не удалось получить объект Askable с названием %s", name));
    }
}
