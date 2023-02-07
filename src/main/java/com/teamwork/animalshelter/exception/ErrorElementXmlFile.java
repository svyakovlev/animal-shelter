package com.teamwork.animalshelter.exception;

public class ErrorElementXmlFile extends RuntimeException {
    public ErrorElementXmlFile(String nameClosingElement, String nameCurrentElement) {
        super(String.format("Закрывающий тег '%s' не соответствует текущему элементу '%s'", nameClosingElement, nameCurrentElement));
    }

    public ErrorElementXmlFile(String name) {
        super(String.format("Для элемента '%s' не найден родительский элемент в стеке", name));
    }

    public ErrorElementXmlFile(char ch[], int start, int length) {
        super(String.format("Для строки '%s' не определен элемент", String.valueOf(ch, start, length)));
    }
}
