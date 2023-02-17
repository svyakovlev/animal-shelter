package com.teamwork.animalshelter.exception;

import com.teamwork.animalshelter.service.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserExists extends ServiceException {
    public UserExists(long chatId) {
        super(String.format("Пользователь с идентификатором чата %d уже существует", chatId));
    }

    public UserExists(String phone) {
        super(String.format("Пользователь с телефоном %s уже существует", phone));
    }
}
