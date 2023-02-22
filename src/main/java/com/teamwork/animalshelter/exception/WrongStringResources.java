package com.teamwork.animalshelter.exception;
import com.teamwork.animalshelter.parser.ParserXML;

/**
 * Исключение вызывается при ошибке парсинга строки ресурсов.
 * @see ParserXML
 */
public class WrongStringResources extends RuntimeException {
    public WrongStringResources(String resources) {
        super("Неверно указана строка ресурсов в файле свойств:  " + resources);
    }
}
