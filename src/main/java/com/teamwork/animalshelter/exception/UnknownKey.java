package com.teamwork.animalshelter.exception;

public class UnknownKey extends RuntimeException {
    public UnknownKey(String key, String hint) {
        super(String.format("При разборе структуры Map найден неизвестный ключ '%s' (%s)", key, hint));
    }
}
