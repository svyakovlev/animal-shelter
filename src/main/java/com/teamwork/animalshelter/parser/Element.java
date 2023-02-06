package com.teamwork.animalshelter.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс, описывающий отдельный узел объектной модели, создаваемой классом {@link ParserXML}.
 * @see ParserXML
 */
public class Element {
    /**
     * Имя элемента XML
     */
    private String name;

    /**
     * Родительский элемент. Корневой элемент имеет значение {@code null}.
     */
    private Element parent;

    /**
     * Список дочерних элементов
     */
    private List<Element> childs;

    /**
     * Текст, относящийся к данному элементу.
     */
    private String text;

    /**
     * Атрибуты со значениями для данного элемента.
     */
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

    public String getName() {
        return name;
    }

    public Element getParent() {
        return parent;
    }

    public List<Element> getChilds() {
        return childs;
    }

    public String getText() {
        return text;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setText(String text) {
        this.text = text;
    }
}
