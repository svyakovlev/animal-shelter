package com.teamwork.animalshelter.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.teamwork.animalshelter.action.Askable;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.exception.AskableNullPointer;
import com.teamwork.animalshelter.model.ProbationDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class BotService {
    private final TelegramBot telegramBot;
    private final Logger logger = LoggerFactory.getLogger(BotService.class);
    private final AskableServiceObjects askableServiceObjects;
    private final AnimalShetlerInfoService animalShetlerInfoService;

    public BotService(TelegramBot telegramBot, AskableServiceObjects askableServiceObjects, AnimalShetlerInfoService animalShetlerInfoService) {
        this.telegramBot = telegramBot;
        this.askableServiceObjects = askableServiceObjects;
        this.animalShetlerInfoService = animalShetlerInfoService;
    }

    private void verifyResponse(SendResponse response, long chatId) {
        // в этом методе следует вызвать исключение в случае ошибки доставки
        // и обдумать как это отразится на выполнении алгоритма
        if (response.isOk()) {
            logger.info("Message is sent into chat <{}> successfully", chatId);
        } else {
            logger.error("Error: message is not sent into chat <{}> (command '/start')", chatId);
        }
    }

    private void sendTextInfo(Object object, long chatId) {
        SendMessage sendMessage = new SendMessage(chatId, (String) object);
        SendResponse response = telegramBot.execute(sendMessage);
        verifyResponse(response, chatId);
    }

    private void sendPhotoInfo(Object object, long chatId) {

    }

    private void sendDocumentInfo(Object object, long chatId) {

    }

    /**
     * Функция разделяет способы передачи данных. Является общей функцией для отправки информации в чат.
     * @param object отправляемые данные
     * @param type тип отправляемой информации
     * @param chatId идентификатор чата
     */
    public void sendInfo(Object object, ProbationDataType type, long chatId) {
        switch (type) {
            case TEXT -> sendTextInfo(object, chatId);
            case DOCUMENT -> sendDocumentInfo(object, chatId);
            case PHOTO -> sendPhotoInfo(object, chatId);
        }
    }

    /**
     * Функция перенаправляет данные, получаемые из класса {@code AnimalShetlerInfoService}
     * @param info карта записей, в которой ключом может быть либо передаваемый текст, либо путь к файлу
     * @param chatId идентификатор чата
     * @see AnimalShetlerInfoService
     * @see ProbationDataType
     */
    public void sendShetlerInfoByCommand(Map<String, ProbationDataType> info, long chatId) {
        if(info == null) return;
        for(Map.Entry entry : info.entrySet()) {
            sendInfo(entry.getKey(), (ProbationDataType) entry.getValue(), chatId);
        }
    }

    /**
     * Осуществляется вызов необходимой функции в зависимости от строкового идентификатора команды.
     * @param command идентификатор команды
     * @param chatId идентификатор чата
     */
    public void runCommands(String command, Long chatId) {
        switch(command) {
            case "common_info":
                sendShetlerInfoByCommand(animalShetlerInfoService.getCommonInfo(), chatId);
                break;
            case "contact_info":
                sendShetlerInfoByCommand(animalShetlerInfoService.getContacts(), chatId);
                break;
            case "accident_prevention_info":
                sendShetlerInfoByCommand(animalShetlerInfoService.getAccidentPrevention(), chatId);
                break;
            case "chat":
                break;
            case "phone_call":
                break;

        }
    }

    /**
     * Функция выполняет переход к следующему вопросу или пукту меню и отправляет вопрос
     * или дочерние пункты меню пользователя для выбора. Также запускается режим ожидания
     * ответа от пользователя.
     * @param ask объект, реализующий интерфейс {@code Askable}
     * @param chatId идентификатор чата
     * @param s данная строка добавляется в самое начало сообщения, отправляемого пользователю
     * @see Askable
     */
    private void doAction(Askable ask,  long chatId, String s) {
        String action = ask.nextAction();
        if (action == null) return;
        askableServiceObjects.addResponse(chatId, "");
        ask.setWaitingResponse(true);
        String info = s.isEmpty() ? action : s + "\n" + action;
        sendInfo(info, ProbationDataType.TEXT, chatId);
    }

    /**
     * Функция запускает работу опросника или меню
     * @param name название объекта, реализующего интерфейс {@code Askable}
     * @param chatId идентификатор чата
     * @return Map , где key - это строковая метка, value - строковое значение (для опросника является
     * ответом пользователя на вопрос с меткой, указанной в {@code key})
     * Возможные значения key:
     * <ul>
     *     <li>{@code command} - в value находится строковая метка команды, которая используется в функции {@link #runCommands(String, Long)} </li>
     *     <li>{@code interrupt} - value не используется</li>
     *     <li>{@code <строковая метка>} - метка используется, чтобы определить </li>
     * </ul>
     * @throws InterruptedException
     */
    private Map<String, String> startAction(String name, long chatId) throws InterruptedException {
        Askable ask = askableServiceObjects.getObject(name, chatId);
        if (ask == null) throw new AskableNullPointer(name);

        LocalDateTime startTime = LocalDateTime.now();
        String s = "";
        ask.init();
        while (!ask.empty()) {
            while (ask.isWaitingResponse()) {
                String response = askableServiceObjects.getResponse(chatId);
                s = "";
                if (response.isEmpty()) {
                    long minutesPassed = ChronoUnit.MINUTES.between(startTime, LocalDateTime.now());
                    if (ask.intervalExceeded((int) minutesPassed)) {
                        ask.setWaitingResponse(false);
                        askableServiceObjects.removeResponse(chatId);
                        Map<String, String> result = new HashMap<>();
                        result.put("interrupt_time", "");
                        return result;
                    }
                    Thread.sleep(10_000);
                } else {
                    ask.setWaitingResponse(false);
                    askableServiceObjects.removeResponse(chatId);
                    if (response.equals("0") || response.equals("'0'")) {
                        s = "Можете выбрать другую команду.";
                        sendInfo(s, ProbationDataType.TEXT, chatId);
                        Map<String, String> result = new HashMap<>();
                        result.put("interrupt_user", "");
                        return result;
                    }
                    if (ask.verificationRequired() && !ask.checkResponse(response)) {
                        s = "В вашем ответе была допущена ошибка: " + ask.getLastError() + "\n Введите ваш ответ еще раз (для выхода из команды отправьте '0')";
                    } else {
                        ask.setResponse(response);
                    }
                }
            }
            s = "(для выхода из команды отправьте '0')";
            doAction(ask, chatId, s);
            startTime = LocalDateTime.now();
        }
        return ask.getResult();
    }

}
