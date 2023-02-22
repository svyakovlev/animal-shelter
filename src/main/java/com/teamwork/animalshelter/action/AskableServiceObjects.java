package com.teamwork.animalshelter.action;

import com.teamwork.animalshelter.concurrent.ShetlerThread;
import com.teamwork.animalshelter.configuration.TelegramBotConfiguration;
import com.teamwork.animalshelter.exception.TemplateAlreadyExist;
import com.teamwork.animalshelter.exception.TemplateNotExist;
import com.teamwork.animalshelter.service.BotService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Класс определяет набор служебных структур, используемых при диалоге с пользователем.
 * Бин этого класса инжектится в объект класса {@link BotService}.
 * @see TelegramBotConfiguration
 * @see BotService
 */
@Service
public class AskableServiceObjects {

    /**
     * Вспомогательная структура.
     * Служит для записи ответов пользователей на заданные им вопросы.
     * Используется в алгоритме опросника.
     * <ul>
     * <li> key: идентификатор чата пользователя</li>
     * <li> value: полученный ответ пользователя</li></ul>
     */
    private Map<Long, String> waitingResponses;

    /**
     * Вспомогательная структура.
     * Служит для кэширования шаблонов объектов, создаваемых при запуске приложения из файлов xml.
     * <ul>
     * <li> key: метка шаблона (метка берется из файла xml)</li>
     * <li> value: объект, реализующий интерфейс {@code Askable}</li></ul>
     * @see Askable
     */
    private Map<String, Askable> cacheTemplates;

    /**
     * Вспомогательная структура.
     * Служит для кэширования объектов в разрезе пользователей чата.
     * <br><br>При запросе требуемого объекта вначале будет делаться попытка получить из кэша объектов.
     * Если его еще нет в кэше, то будет создан новый объект по шаблону из {@link #cacheTemplates}.
     * <ul>
     * <li> key: идентификатор чата</li>
     * <li> value: объект, создаваемый по шаблону из набора шаблонов {@link #cacheTemplates}</li></ul>
     * @see Askable
     */
    private Map<Long, Map<String, Askable>> cacheObjects;

    /**
     * Вспомогательная структура.
     * Требуется для работы с одновременными запросами сотрудникам, кто из них готов взять клиента в работу.
     * В эту структуру данных приходит ответ сотрудника.
     * <ul>
     * <li> key: идентификатор чата пользователя</li>
     * <li> value: {@code Map}, где ключом является идентификатор чата сотрудника, а значением
     * - булево значение, где {@code true} означает, что сотрудник дал положительный ответ. Ответ будет
     * считаться положительным, если сотрудник отправит специальную команду {@code '/+'.}</li></ul>
     */
    private Map<Long, Map<Long, Boolean>> concurrentQuery;

    /**
     * Вспомогательная структура.
     * Требуется для хранения потоков пользователей для своевременного завершения.
     * <ul>
     * <li> key: идентификатор чата пользователя</li>
     * <li> value: {@code Thread} поток пользователя</li></ul>
     */
    private Map<Long, ShetlerThread> threads;

    /**
     * Вспомогательная структура.
     * Требуется для работы чата. Служит промежуточным узлом хранения сообщений.
     * <ul>
     * <li> key: идентификатор чата</li>
     * <li> value: очередь сообщений</li></ul>
     */
    private Map<Long, Queue<String>> queueChat;

    public AskableServiceObjects() {
        this.waitingResponses = new HashMap<>();
        this.cacheTemplates = new HashMap<>();
        this.cacheObjects = new HashMap<>();
        this.concurrentQuery = new HashMap<>();
        this.threads = new HashMap<>();
        this.queueChat = new HashMap<>();
    }

    public synchronized void addResponse(long chatId, String response) {
        waitingResponses.put(chatId, response);
    }

    public synchronized boolean isChatIdForResponse(long chatId) {
        return waitingResponses.containsKey(chatId);
    }

    public synchronized String getResponse(long chatId) {
        if (!waitingResponses.containsKey(chatId)) {
            // исключение
            // "Попытка получить ответ пользователя по несуществующему ключу"
        }
        return waitingResponses.get(chatId);
    }

    public synchronized void removeResponse(long chatId) {
        waitingResponses.remove(chatId);
    }

    public synchronized void addTemplate(String name, Askable ask) {
        if (cacheTemplates.containsKey(name)) {
            throw new TemplateAlreadyExist(name);
        }
        cacheTemplates.put(name, ask);
    }

    public synchronized void removeTemplate(String name) {
        cacheTemplates.remove(name);
    }

    private void addObject(Long chatId, String name, Askable ask) {
        Map<String, Askable> map;
        if (cacheObjects.containsKey(chatId)) {
            map = cacheObjects.get(chatId);
        } else {
            map = new HashMap<>();
            cacheObjects.put(chatId, map);
        }
        map.put(name, ask);
    }

    /**
     * Функция возвращает объект {@code Askable} по заданному имени
     * @param name название шаблона объекта {@code Askable}
     * @param chatId идентификатор чата
     * @return возвращает объект {@code Askable}
     * @throws TemplateNotExist вызывается, если шаблон с таким названием не существует
     * @see Askable
     */
    public synchronized Askable getObject(String name, long chatId) {
        Askable ask = null;
        if (cacheObjects.containsKey(chatId)) {
            Map<String, Askable> map = cacheObjects.get(chatId);
            ask = map.get(name);
            if (ask != null) return ask;
        }
        if (!cacheTemplates.containsKey(name)) {
            throw new TemplateNotExist(name);
        }
        ask = cacheTemplates.get(name).dublicate();
        addObject(chatId, name, ask);
        return ask;
    }

    /**
     * Если для указанного идентификатора чата уже создавался поток, то он прерывается,
     * и на его место в структуре устанавливается новый поток.
     * @param chatId идентификатор чата
     * @param newThread новый поток пользователя
     * @see ShetlerThread
     */
    public synchronized void updateUserThread(long chatId, ShetlerThread newThread) {
        ShetlerThread thread = threads.get(chatId);
        if (thread != null) {
            thread.interrupt();
        }
        threads.put(chatId, newThread);
    }

    /**
     * Функция служит для подготовки структуры {@code concurrentQuery} перед запуском
     * параллельного опроса сотрудников о том, кто готов взять пользователя в работу.
     * @param userChatId идентифиактор чата пользователя
     * @param employeeChatId идентификатор чата сотрудника
     */
    public synchronized void addEmployeeChatConcurrentQuery(long userChatId, long employeeChatId) {
        Map<Long, Boolean> employeesChats;
        if (concurrentQuery.containsKey(userChatId)) {
            employeesChats = concurrentQuery.get(userChatId);
        } else {
            employeesChats = new HashMap<>();
            concurrentQuery.put(userChatId, employeesChats);
        }
        employeesChats.put(employeeChatId, false);
    }

    /**
     * Возвращает идентификатор чата сотрудника, который готов начать работу с пользователем.
     * Если их будет несколько, то возвращается идентификатор по первому найденному сотруднику,
     * ответившему на запрос положительно.
     * @param userChatId идентификатор чата пользователя, который запустил выполнение команды
     * @return идентификатор чата сотрудника, который готов поработать с пользователем
     */
    public synchronized Long findPositiveReactionOfConcurrentQuery(long userChatId) {
        Map<Long, Boolean> employeesChats = concurrentQuery.get(userChatId);
        if (employeesChats == null) return null;
        for (Map.Entry<Long, Boolean> entry : employeesChats.entrySet()) {
            if (entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Функция устанавливает готовность взять пользователя в работу сотрудником c указанным идентификатором.
     * @param employeeChatId идентификатор чата сотрудника
     * @return {@code true} если сотрудник указал готовность работать с пользователем и при этом данный ответ
     * был записан в структуру {@link #concurrentQuery}
     */
    public synchronized boolean setPositiveReactionOfConcurrentQuery(long employeeChatId) {
        for (Map.Entry<Long, Map<Long, Boolean>> entry : concurrentQuery.entrySet()) {
            Map<Long, Boolean> employeesChats = entry.getValue();
            if (employeesChats.containsKey(employeeChatId)) {
                employeesChats.put(employeeChatId, true);
                return true;
            }
        }
        return false;
    }

    /**
     * Очищает сотрудников в структуре {@link #concurrentQuery}. Отсутствие сотрудников означает,
     * что параллельный запрос на текущий момент не активен для пользователя с
     * идентификатором {@code userChatId}.
     * @param userChatId идентификатор чата пользователя, который запустил выполнение команды
     */
    public synchronized void resetConcurrentQuery(long userChatId) {
        Map<Long, Boolean> employeesChats = concurrentQuery.get(userChatId);
        if (employeesChats == null) return;
        employeesChats.clear();
    }

    public synchronized void addMessageIntoQueueChat(long chatId, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Queue<String> queue = queueChat.get(chatId);
        if (queue == null) {
            queue = new ArrayDeque<>();
            queueChat.put(chatId, queue);
        }
        queue.offer(message);
    }

    public synchronized String getMessageFromQueueChat(long chatId) {
        Queue<String> queue = queueChat.get(chatId);
        if (queue == null) {
            return null;
        }
        return queue.poll();
    }

    public synchronized void resetQueueChat(long chatId) {
        Queue<String> queue = queueChat.get(chatId);
        if (queue == null) {
            return;
        }
        queue.clear();
    }

    public synchronized boolean isEmptyQueue(long chatId) {
        Queue<String> queue = queueChat.get(chatId);
        if (queue == null) {
            return true;
        }
        return queue.isEmpty();
    }

    public synchronized void resetServiceObjects(long chatId) {
        removeResponse(chatId);
        resetConcurrentQuery(chatId);
        resetQueueChat(chatId);
    }

}
