package com.teamwork.animalshelter.exception;

public class NotFoundChatId extends RuntimeException {
    public NotFoundChatId(int id) {
        super(String.format("Для пользователя с идентификатором '%d' не задан идентификатор чата", id));
    }
}
