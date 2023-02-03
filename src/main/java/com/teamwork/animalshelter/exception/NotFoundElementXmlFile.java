package com.teamwork.animalshelter.exception;

public class NotFoundElementXmlFile extends RuntimeException {
    public NotFoundElementXmlFile(String nameElement, String nameClass) {
        super(String.format("Для объекта '%s' имя элемента '%s' в xml файле не предусмотрено", nameClass, nameElement));
    }
}
