package com.teamwork.animalshelter.exception;

import com.teamwork.animalshelter.parser.ParserXML;

/**
 * Исключение вызывается при ошибке получения шаблона по имени.
 */
public class TemplateNotExist extends RuntimeException {
    public TemplateNotExist(String name) {
        super(String.format("Шаблон объекта с названием '%s' не существует", name));
    }
}
