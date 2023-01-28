package com.teamwork.animalshelter.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Element {
    private String name;
    private Element parent;
    private List<Element> childs;
    private String text;
    private Map<String, String> attributes;

    public Element(String name, Element parent) {
        this.name = name;
        this.parent = parent;
        this.childs = new ArrayList<>();
        this.attributes = new HashMap<>();
    }

    public void addChild(Element child) {
        childs.add(child);
    }

    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }
}
