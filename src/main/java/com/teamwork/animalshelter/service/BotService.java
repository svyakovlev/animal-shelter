package com.teamwork.animalshelter.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.teamwork.animalshelter.action.Askable;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.exception.AskableNullPointer;
import com.teamwork.animalshelter.exception.NotFoundAdministrator;
import com.teamwork.animalshelter.exception.NotFoundCommand;
import com.teamwork.animalshelter.exception.UnknownKey;
import com.teamwork.animalshelter.model.*;
import com.teamwork.animalshelter.repository.ProbationJournalRepository;
import com.teamwork.animalshelter.repository.ProbationRepository;
import com.teamwork.animalshelter.repository.SupportRepository;
import com.teamwork.animalshelter.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
    private final UserService userService;
    private final SupportRepository supportRepository;

    public BotService(TelegramBot telegramBot, AskableServiceObjects askableServiceObjects,
                      AnimalShetlerInfoService animalShetlerInfoService,
                      UserRepository userRepository, ProbationJournalRepository probationJournalRepository,
                      ProbationRepository probationRepository, UserService userService, SupportRepository supportRepository) {
        this.telegramBot = telegramBot;
        this.askableServiceObjects = askableServiceObjects;
        this.animalShetlerInfoService = animalShetlerInfoService;
        this.userRepository = userRepository;
        this.probationJournalRepository = probationJournalRepository;
        this.probationRepository = probationRepository;
        this.userService = userService;
        this.supportRepository = supportRepository;
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
     * Функция перенаправляет данные, получаемые из класса {@code AnimalShetlerInfoService}
     *
     * @param info   карта записей, в которой ключом может быть либо передаваемый текст, либо путь к файлу
     * @param chatId идентификатор чата
     * @see AnimalShetlerInfoService
     * @see ProbationDataType
     */
    public void sendShetlerInfoByCommand(Map<String, ProbationDataType> info, long chatId) {
        if (info == null) return;
        for (Map.Entry entry : info.entrySet()) {
            sendInfo(entry.getKey(), (ProbationDataType) entry.getValue(), chatId);
        }
    }

    /**
     * Осуществляется вызов необходимой функции в зависимости от строкового идентификатора команды.
     *
     * @param command идентификатор команды
     * @param chatId  идентификатор чата
     */
    public void runCommands(String command, Long chatId) {
        switch (command) {
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
            case "form_daily_report":
                break;
            default:
                throw new NotFoundCommand(command);
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
     *     <li>{@code command} - в value находится строковая метка команды, которая используется в функции {@link #runCommands(String, Long)} </li>
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
                    Thread.sleep(10_000);
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
     * @param userChatId идентификатор чата пользователя
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

    public void processMessageText(String message, long chatId) {
        try {
            switch (message) {
                case "/info":
                    Map<String, String> result = startAction("menu_info", chatId);
                    if (result.containsKey("interrupt")) return;
                    if (result.containsKey("command")) {
                        runCommands(result.get("command"), chatId);
                    }
                    callErrorKeyMap(result, "processMessageText() -> message");
                    break;

                default:

            }

        } catch(InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void callErrorKeyMap(Map<String, String> map, String hint) {
        String[] keys = (String[]) map.keySet().toArray();

        if (keys.length == 0) {
            throw new UnknownKey("", hint);
        } else {
            throw new UnknownKey(keys[0], hint);
        }
    }

    public void sendGreeting(long chatId, LocalDateTime newVisit ) {
       User user=userRepository.findUserByChatId(chatId);
        if (user != null) {
            LocalDateTime lastVisit = user.getLastVisit();
            if (lastVisit == null||lastVisit.toLocalDate().atStartOfDay().compareTo(newVisit.toLocalDate().atStartOfDay())!=0) {
                sendInfo(String.format("Добро пожаловать, %s",user.getName()),ProbationDataType.TEXT,chatId);
            }
            user.setLastVisit(newVisit);
            userRepository.saveAndFlush(user);
        }
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
        List<ProbationJournal> records =  probationJournalRepository.getJournalRecordsOnIncompleteReport();
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

    /**
     * Отправляет волонтерам сообщения: выяснить причины, по которым клиент перестал
     * посылать отчеты по питомцу. Отправка сообщения происходит в том случае, если
     * клиент не посылал отчеты более 2-х суток (расчет интервала идет от полуночи текущего дня
     * в обратную сторону).
     * @throws NotFoundAdministrator вызывается в случае, когда в базе нет ни одного администратора.
     */
    @Scheduled(cron = "0 0 9/24 * * *")
    public void remindAboutReportProblem() {
        List<Probation> probations = probationRepository.getProbationsOnReportProblem();
        if (probations == null) return;
        sendTasksToEmployees(probations, "Требуется выяснить, почему клиент перестал посылать отчеты по питомцу.");
    }

    private void sendTasksToEmployees(List<Probation> probations, String taskString) {
        List<User> freeVolunteers = userRepository.findUsersByVolunteerActiveIsTrue();
        User adminEmployee = userRepository.findFirstByAdministratorIsTrue().get();
        if (adminEmployee == null) {
            throw new NotFoundAdministrator();
        }
        List<User> volunteers = null;
        ListIterator<User> iterator = null;
        if (freeVolunteers != null) {
            volunteers = new LinkedList<>(freeVolunteers);
            iterator = volunteers.listIterator();
        }
        long chatId;
        String message = taskString +
                "\nИмя клиента: %s,\n" +
                "телефоны: %s, \n" +
                "кличка питомца: %s.";
        for (Probation probation : probations) {
            User user = probation.getUser();
            if (volunteers == null) chatId = adminEmployee.getChatId();
            else {
                if (!iterator.hasNext()) iterator = volunteers.listIterator();
                chatId = iterator.next().getChatId();
            }
            sendInfo(String.format(message, user.getName(),
                            userService.getTelephonesByUser(user),
                            probation.getPet().getNickname()),
                    ProbationDataType.TEXT,
                    chatId);
        }
    }

    /**
     * Отправляет волонтерам сообщения с требованием принять решение по испытательному сроку.
     */
    @Scheduled(cron = "0 0 12/24 * * *")
    public void checkFinishProbation() {
        List<Probation> probations = probationRepository.findProbationByDateFinishBeforeAndAndSuccessIsFalseAndResultEquals(LocalDateTime.now(), "");
        if (probations == null) return;
        sendTasksToEmployees(probations, "Следует принять решение по испытательному сроку.");
    }

    /**
     * Отправляет волонтерам напоминание о предстоящем событии, но не более чем за 30 минут.
     * При этом сотрудник переводится в состояние "Занят".
     */
    @Scheduled(cron = "0 0/10 * * * *")
    public void remindAboutEvent() {
        List<Support> records = supportRepository.findAllByFinishIsFalseAndBeginDateTimeAfter(LocalDateTime.now());
        if (records == null) return;
        for (Support support : records) {
            long minutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), support.getBeginDateTime());
            if (minutes > 30) continue;
            String supportType = "";
            switch (support.getType()) {
                case CALL -> supportType = "телефонный звонок";
                case MEETING -> supportType = "встреча";
                case CHAT -> supportType = "чат";
            }
            String message = String.format("У вас назначен(-а) %s через %d минут с пользователем %s.",
                    supportType, minutes, support.getUser().getName()) +
                    (support.getType() == SupportType.CALL ? "Телефоны: " + userService.getTelephonesByUser(support.getUser()) : "");
            sendInfo(message, ProbationDataType.TEXT, support.getVolunteer().getChatId());
            if (support.getVolunteer().isVolunteerActive()) {
                User volunteer = support.getVolunteer();
                volunteer.setVolunteerActive(false);
                userRepository.save(volunteer);
                sendInfo("Вы переведены в состояние 'занят'!", ProbationDataType.TEXT, volunteer.getChatId());
            }
        }
    }

}
