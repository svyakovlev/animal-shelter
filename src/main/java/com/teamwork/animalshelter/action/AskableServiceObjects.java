package com.teamwork.animalshelter.action;

import java.util.HashMap;
import java.util.Map;

public class AskableServiceObjects {

    /**
     * Вспомогательная структура.
     * Служит для записи ответов пользователей на заданные им вопросы.
     * <ul>
     * <li> key: идентификатор чата пользователя</li>
     * <li> value: полученный ответ пользователя</li></ul>
     */
    private Map<Long, String> waitingResponses;

    private Map<String, Askable> cacheTemplates;
    private Map<Long, Map<String, Askable>> cacheObjects;

    public AskableServiceObjects() {
        this.waitingResponses = new HashMap<>();
        this.cacheTemplates = new HashMap<>();
        this.cacheObjects = new HashMap<>();
    }

    public void addResponse(long chatId, String response) {
        waitingResponses.put(chatId, response);
    }

    public String getResponse(long chatId) {
        if (!waitingResponses.containsKey(chatId)) {
            // исключение
            // "Попытка получить ответ пользователя по несуществующему ключу"
        }
        return waitingResponses.get(chatId);
    }

    public void removeResponse(long chatId) {
        waitingResponses.remove(chatId);
    }

    public  void addTemplate(String name, Askable ask) {
        cacheTemplates.put(name, ask);
    }

    public  void removeTemplate(String name) {
        cacheTemplates.remove(name);
    }

    public  void addObject(Long chatId, String name, Askable ask) {
        Map<String, Askable> map;
        if (cacheObjects.containsKey(chatId)) {
            map = cacheObjects.get(chatId);
        } else {
            map = new HashMap<>();
            cacheObjects.put(chatId, map);
        }
        map.put(name, ask);
    }
}
