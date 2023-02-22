package com.teamwork.animalshelter.exception;

import com.teamwork.animalshelter.service.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WrongPhoneNumber extends ServiceException {
    public WrongPhoneNumber() {
        super("Введенный номер телефона не соответствует формату." +
                "Номер сотового телефона должен содержать 10 цифр без пробелов (вначале могут находиться символы '+7' или '7')");
    }
}
