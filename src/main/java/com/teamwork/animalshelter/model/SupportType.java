package com.teamwork.animalshelter.model;

public enum SupportType {
    CHAT("чат"),
    CALL("звонок"),
    MEETING("встреча");

    private String name;

    SupportType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
