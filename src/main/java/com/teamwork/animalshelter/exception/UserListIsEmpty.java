package com.teamwork.animalshelter.exception;

import com.teamwork.animalshelter.service.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NO_CONTENT)
public class UserListIsEmpty extends ServiceException {
    public UserListIsEmpty() {
        super("Список пользователей пуст");
    }
}
