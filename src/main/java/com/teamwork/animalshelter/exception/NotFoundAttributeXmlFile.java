package com.teamwork.animalshelter.exception;

public class NotFoundAttributeXmlFile extends RuntimeException {
    public NotFoundAttributeXmlFile(String nameElement, String nameAttribute) {
        super(String.format("Не найден атрибут '%s' у элемента '%s'", nameAttribute, nameElement));
    }

    public NotFoundAttributeXmlFile(String nameElement, String nameAttribute, String nameLabel) {
        super(String.format("Не найден атрибут '%s' у элемента '%s' с меткой '%s'", nameAttribute, nameElement, nameLabel));
    }
}
