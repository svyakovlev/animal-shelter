package com.teamwork.animalshelter.exception;

public class WrongFormatXmlFile extends RuntimeException {
    public WrongFormatXmlFile(String filePath) {
        super("Неверный формат файла: " + filePath);
    }
}
