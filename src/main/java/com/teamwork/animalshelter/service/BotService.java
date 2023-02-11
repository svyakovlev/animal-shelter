package com.teamwork.animalshelter.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import com.teamwork.animalshelter.action.Askable;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.exception.*;
import com.teamwork.animalshelter.model.*;
import com.teamwork.animalshelter.repository.ProbationJournalRepository;
import com.teamwork.animalshelter.repository.ProbationRepository;
import com.teamwork.animalshelter.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class BotService {
    private final TelegramBot telegramBot;
    private final Logger logger = LoggerFactory.getLogger(BotService.class);
    private final AskableServiceObjects askableServiceObjects;
    private final AnimalShetlerInfoService animalShetlerInfoService;
    private final UserRepository userRepository;
    private final ProbationJournalRepository probationJournalRepository;
    private final ProbationRepository probationRepository;

    public BotService(TelegramBot telegramBot, AskableServiceObjects askableServiceObjects,
                      AnimalShetlerInfoService animalShetlerInfoService,
                      UserRepository userRepository, ProbationJournalRepository probationJournalRepository,
                      ProbationRepository probationRepository) {
        this.telegramBot = telegramBot;
        this.askableServiceObjects = askableServiceObjects;
        this.animalShetlerInfoService = animalShetlerInfoService;
        this.userRepository = userRepository;
        this.probationJournalRepository = probationJournalRepository;
        this.probationRepository = probationRepository;
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
        SendPhoto sendPhoto = new SendPhoto(chatId, (File) object);
        SendResponse response = telegramBot.execute(sendPhoto);
        verifyResponse(response, chatId);
    }

    private void sendDocumentInfo(Object object, long chatId) {
        SendDocument sendDocument = new SendDocument(chatId, (File) object);
        SendResponse response = telegramBot.execute(sendDocument);
        verifyResponse(response, chatId);
    }

    /**
     * Функция разделяет способы передачи данных. Является общей функцией для отправки информации в чат.
     *
     * @param object отправляемые данные
     * @param type   тип отправляемой информации
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
     * Функция выполняет переход к следующему вопросу или пукту меню и отправляет вопрос
     * или дочерние пункты меню пользователя для выбора. Также запускается режим ожидания
     * ответа от пользователя.
     *
     * @param ask    объект, реализующий интерфейс {@code Askable}
     * @param chatId идентификатор чата
     * @param s      данная строка добавляется в самое начало сообщения, отправляемого пользователю
     * @see Askable
     */
    private void doAction(Askable ask, long chatId, String s) {
        if (Thread.currentThread().isInterrupted()) return;
        String action = ask.nextAction();
        if (action == null) return;
        askableServiceObjects.addResponse(chatId, "");
        ask.setWaitingResponse(true);
        String info = s.isEmpty() ? action : s + "\n" + action;
        sendInfo(info, ProbationDataType.TEXT, chatId);
    }

    /**
     * Функция запускает работу опросника или меню
     *
     * @param name   название объекта, реализующего интерфейс {@code Askable}
     * @param chatId идентификатор чата
     * @return Map , где key - это строковая метка, value - строковое значение (для опросника является
     * ответом пользователя на вопрос с меткой, указанной в {@code key})
     * Возможные значения key:
     * <ul>
     *     <li>{@code command} - в value находится строковая метка команды, которая используется в функции {@link UserService#runCommands(String, Long)} </li>
     *     <li>{@code interrupt} - value не используется</li>
     *     <li>{@code <строковая метка>} - метка используется, чтобы определить </li>
     * </ul>
     * @throws InterruptedException
     */
    Map<String, String> startAction(String name, long chatId) throws InterruptedException {
        Askable ask = askableServiceObjects.getObject(name, chatId);
        if (ask == null) throw new AskableNullPointer(name);

        Map<String, String> resultInterrupted = new HashMap<>();
        resultInterrupted.put("interrupt", "");

        LocalDateTime startTime = LocalDateTime.now();
        String s = "";
        ask.init();
        while (!ask.empty()) {
            if (Thread.currentThread().isInterrupted()) return resultInterrupted;
            while (ask.isWaitingResponse()) {
                if (Thread.currentThread().isInterrupted()) return resultInterrupted;
                String response = askableServiceObjects.getResponse(chatId);
                s = "";
                if (response.isEmpty()) {
                    long minutesPassed = ChronoUnit.MINUTES.between(startTime, LocalDateTime.now());
                    if (ask.intervalExceeded((int) minutesPassed)) {
                        ask.setWaitingResponse(false);
                        askableServiceObjects.removeResponse(chatId);
                        return resultInterrupted;
                    }
                    Thread.sleep(2_000);
                } else {
                    ask.setWaitingResponse(false);
                    askableServiceObjects.removeResponse(chatId);
                    if (response.equals("0") || response.equals("'0'")) {
                        s = "Можете выбрать другую команду.";
                        sendInfo(s, ProbationDataType.TEXT, chatId);
                        return resultInterrupted;
                    }
                    if (ask.verificationRequired() && !ask.checkResponse(response)) {
                        s = "В вашем ответе была допущена ошибка: " + ask.getLastError() + "\n Введите ваш ответ еще раз (для выхода из команды отправьте '0')";
                    } else {
                        ask.setResponse(response);
                    }
                }
            }
            //s = "(для выхода из команды отправьте '0')";
            doAction(ask, chatId, s);
            startTime = LocalDateTime.now();
        }
        return ask.getResult();
    }

    /**
     * Создает и запускает работу чата. Бот выступает посредником при пересылке сообщений
     * между сотрудником и пользователем. Интервал ожидания новых сообщений задается
     * в переменной {@code intervalWaiting} (в минутах). Если этот интервал превышен, то чат будет закрыт.
     * Отсчет времени бездействия начинается после отправки последнего сообщения.
     *
     * @param userChatId     идентификатор чата пользователя
     * @param employeeChatId идентификатор чата сотрудника
     */
    public void createChat(long userChatId, long employeeChatId) throws InterruptedException {
        final int intervalWaiting = 10;

        askableServiceObjects.resetQueueChat(userChatId);
        askableServiceObjects.resetQueueChat(employeeChatId);
        askableServiceObjects.addResponse(userChatId, "chat");
        askableServiceObjects.addResponse(employeeChatId, "chat");

        String message = "Чат открыт. Можете начинать беседу";
        sendInfo(message, ProbationDataType.TEXT, employeeChatId);
        sendInfo(message, ProbationDataType.TEXT, userChatId);

        LocalDateTime startTime = LocalDateTime.now();
        long minutesPassed = 0;
        boolean chatStopped = false;

        while (minutesPassed < intervalWaiting) {
            while (!askableServiceObjects.isEmptyQueue(userChatId)) {
                message = askableServiceObjects.getMessageFromQueueChat(userChatId);
                sendInfo(message, ProbationDataType.TEXT, employeeChatId);
                startTime = LocalDateTime.now();
            }
            while (!askableServiceObjects.isEmptyQueue(employeeChatId)) {
                message = askableServiceObjects.getMessageFromQueueChat(employeeChatId);
                if (message.equals("/close")) {
                    chatStopped = true;
                    break;
                }
                sendInfo(message, ProbationDataType.TEXT, userChatId);
                startTime = LocalDateTime.now();
            }
            if (chatStopped) break;
            Thread.sleep(10_000);
            if (Thread.currentThread().isInterrupted()) return;
            minutesPassed = ChronoUnit.MINUTES.between(startTime, LocalDateTime.now());
        }
        askableServiceObjects.resetServiceObjects(userChatId);
        askableServiceObjects.resetServiceObjects(employeeChatId);

        message = "Чат остановлен. Всего вам хорошего!";
        sendInfo(message, ProbationDataType.TEXT, employeeChatId);
        sendInfo(message, ProbationDataType.TEXT, userChatId);
    }

    /**
     * Функция выполняет отправку напоминания клиенту в случае, если он отправил либо только форму отчета,
     * либо только фотографии питомца. Напоминания приходят только в день отправки документов.
     * <ul>
     *     Выполняются следующие проверки записей журнала ({@link ProbationJournal}
     *     <li>полученные записи находятся в интервале с полуночи текущего дня до текущей даты минус 1 час</li>
     *     <li>клиент находится на испытательном сроке </li>
     *     <li>было отправлено только что-то одно: форма отчета или фотографии</li>
     * </ul>
     */
    @Scheduled(cron = "* * 9-20/2 * * *")
    public void remindAboutReport() {
        List<ProbationJournal> records = probationJournalRepository.getJournalRecordsOnIncompleteReport();
        if (records == null) return;
        Probation probation = null;
        String message;
        LocalDateTime currentDate = LocalDateTime.now();
        for (ProbationJournal record : records) {
            if (!(record.isPhotoReceived() ^ record.isReportReceived())) continue;
            probation = record.getProbation();
            if (currentDate.isAfter(probation.getDateBegin()) && currentDate.isBefore(probation.getDateFinish())) {
                User user = probation.getUser();
                Pet pet = probation.getPet();
                if (record.isPhotoReceived())
                    message = String.format("%s, напоминаем Вам, что сегодня необходимо еще отправить заполненную форму отчета по питомцу '%s'!", user.getName(), pet.getNickname());
                else
                    message = String.format("%s, напоминаем Вам, что сегодня необходимо еще отправить фотографии питомца '%s'!", user.getName(), pet.getNickname());
                sendInfo(message, ProbationDataType.TEXT, user.getChatId());
            }
        }
    }

    /**
     * Отправляет сообщение пользователю, котоое было подготовлено сотрудником.
     */
    @Scheduled(cron = "0 0 0/1 * * *")
    public void sendMessageOnProbation() {
        List<Probation> probations = probationRepository.getActiveProbationsWithMessages();
        if (probations == null) return;
        String message = "Вам сообщение от сотрудника приюта.\n";
        for (Probation probation : probations) {
            sendInfo(message + probation.getMessage(), ProbationDataType.TEXT, probation.getUser().getChatId());
            probation.setMessage("");
            probationRepository.saveAndFlush(probation);
        }
    }

}
