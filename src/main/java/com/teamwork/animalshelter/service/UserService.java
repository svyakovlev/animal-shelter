package com.teamwork.animalshelter.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pengrad.telegrambot.TelegramBot;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.configuration.AnimalShetlerProperties;
import com.teamwork.animalshelter.exception.NotFoundAdministrator;
import com.teamwork.animalshelter.exception.NotFoundChatId;
import com.teamwork.animalshelter.exception.NotFoundCommand;
import com.teamwork.animalshelter.exception.UnknownKey;
import com.teamwork.animalshelter.model.*;
import com.teamwork.animalshelter.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

@Service
public class UserService {
    private final BotService botService;
    private final AskableServiceObjects askableServiceObjects;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final ProbationRepository probationRepository;
    private final SupportRepository supportRepository;
    private final AnimalShetlerInfoService animalShetlerInfoService;
    private final AnimalShetlerProperties animalShetlerProperties;
    private final ProbationJournalRepository probationJournalRepository;
    private final PetRepository petRepository;

    private final TelegramBot telegramBot;

    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final Map<String, String> volunteerCommands;
    private final Map<String, String> administratorCommands;

    public UserService(BotService botService, AskableServiceObjects askableServiceObjects,
                       UserRepository userRepository, ContactRepository contactRepository,
                       ProbationRepository probationRepository, SupportRepository supportRepository,
                       AnimalShetlerInfoService animalShetlerInfoService, AnimalShetlerProperties animalShetlerProperties,
                       ProbationJournalRepository probationJournalRepository, PetRepository petRepository,
                       TelegramBot telegramBot, Map<String, String> volunteerCommands, Map<String, String> administratorCommands) {
        this.botService = botService;
        this.askableServiceObjects = askableServiceObjects;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.probationRepository = probationRepository;
        this.supportRepository = supportRepository;
        this.animalShetlerInfoService = animalShetlerInfoService;
        this.animalShetlerProperties = animalShetlerProperties;
        this.probationJournalRepository = probationJournalRepository;
        this.petRepository = petRepository;
        this.telegramBot = telegramBot;
        this.volunteerCommands = volunteerCommands;
        this.administratorCommands = administratorCommands;
    }

    public void wantToBecomeVolunteer(long chatId) throws InterruptedException {
        Map<String, String> replayMessage;
        replayMessage = botService.startAction("data_user", chatId);

        if (replayMessage.containsKey("interrupt")) {
            return;
        }

        User user = findUserByChatId(chatId);

        if (user == null) {
            user = findUserByPhoneNumber(replayMessage.get("telephone"));
        }
        if (user == null) {
            user = new User();
        }
        user.setChatId(chatId);
        user.setName(replayMessage.get("name"));

        String phone = replayMessage.get("telephone");
        if (phone.length() == 10) phone = "7" + phone;
        else if (phone.length() == 12) phone = phone.substring(1);

        Contact contact = new Contact();

        contact.setUser(user);
        contact.setType(ContactType.TELEPHONE);
        contact.setValue(phone);

        userRepository.saveAndFlush(user);
        contactRepository.saveAndFlush(contact);

        List<User> freeAdministrators = userRepository.findUsersByAdministratorIsTrueAndVolunteerActiveIsTrue();

        String message = "";
        if (freeAdministrators == null) {
            message = "Нет свободных администраторов. Закажите обратный звонок.";
            botService.sendInfo(message, ProbationDataType.TEXT, chatId);
            return;
        }

        message = "Идет поиск свободных администраторов. Пожалуйста, подождите... (время ожидания не более 5 минут)";
        botService.sendInfo(message, ProbationDataType.TEXT, chatId);

        message = String.format("Пользователь %s хочеть стать волонтером. Возьмете в работу?", user.getName() );
        Long administratorChatId = startConcurrentQuery(chatId, freeAdministrators, message, 5);

        if (administratorChatId == null) {
            message = "Нет свободных администраторов. Закажите обратный звонок.";
            botService.sendInfo(message, ProbationDataType.TEXT, chatId);
            return;
        }

        // записать в БД, что сотрудник занят
        for (User admin : freeAdministrators) {
            if (admin.getChatId() == administratorChatId) {
                admin.setVolunteerActive(true);
                userRepository.saveAndFlush(admin);
                break;
            }
        }

        botService.createChat(chatId, administratorChatId);
    }

    /**
     * Функция реализует параллельный опрос свободных сотрудников, которые готовы работать с пользователем.
     * Если сотрудник готов взять клиента в работу, то он должен прислать в ответ одно сообщений:
     * <ul>
     *     <li>{@code /+}</li>
     *     <li>{@code /y}</li>
     *     <li>{@code /да}</li>
     * </ul>
     * @param userChatId идентификатор чата пользователя
     * @param employees список свободных сотрудников
     * @param message сообщение, отправляемое сотрудникам
     * @param minutes интервал ожидания (в минутах), в течение которого будет ожидаться ответ от сотрудников
     * @return идентификатор чата сотрудника, который выразил готовность работать с данным пользователем
     */
    private Long startConcurrentQuery (long userChatId, List<User> employees, String message, int minutes) throws InterruptedException {
        for (User employee : employees) {
            if (employee.getChatId() == 0) {
                throw new NotFoundChatId(employee.getId());
            }
            askableServiceObjects.addEmployeeChatConcurrentQuery(userChatId, employee.getChatId());
            botService.sendInfo(message, ProbationDataType.TEXT, employee.getChatId());
        }
        LocalDateTime startTime = LocalDateTime.now();
        long minutesPassed = 0;
        Long employeeChatId = null;
        while (minutesPassed < minutes) {
            if (Thread.currentThread().isInterrupted()) return null;
            employeeChatId = askableServiceObjects.findPositiveReactionOfConcurrentQuery(userChatId);
            if (employeeChatId != null) {
                askableServiceObjects.resetConcurrentQuery(userChatId);
                return employeeChatId;
            }
            Thread.sleep(5000);
            minutesPassed = ChronoUnit.MINUTES.between(startTime, LocalDateTime.now());
        }
        askableServiceObjects.resetConcurrentQuery(userChatId);
        return null;
    }

    private User findUserByChatId(long chatId) {
        return userRepository.findUserByChatId(chatId).orElse(null);
    }

    private User findUserByPhoneNumber(String phoneNumber) {
        if (phoneNumber.length() == 10) phoneNumber = "7" + phoneNumber;
        else if (phoneNumber.length() == 12) phoneNumber = phoneNumber.substring(1);

        Contact contact = contactRepository.findContactByValueEqualsAndTypeEquals(phoneNumber, ContactType.TELEPHONE).orElse(null);
        if (contact == null) {
            return null;
        } else {
            return contact.getUser();
        }
    }

    /**
     * Функция записывает предупреждение волонтера в БД. Бот в дальнейшем отправит это предупреждение пользователю.
     * @param volunteerChatId идентификатор чата волонтера
     * @throws InterruptedException
     */
    public void prepareWarningByVolunteer(long volunteerChatId) throws InterruptedException {
        Map<String, String> questionnaire = botService.startAction("user_message", volunteerChatId);
        if (questionnaire.containsKey("interrupt")) return;
        Integer clientId = Integer.parseInt(questionnaire.get("client-id"));
        Integer petId = Integer.parseInt(questionnaire.get("pet-id"));
        String message = questionnaire.get("message");

        Probation probation = probationRepository.getProbationByClientIdAndPetId(clientId, petId).orElse(null);
        if (probation == null) {
            botService.sendInfo("По указанным идентификаторам пользователя и питомца нет записи по испытательному сроку",
                    ProbationDataType.TEXT, volunteerChatId);
            return;
        }
        if (LocalDateTime.now().isAfter(probation.getDateFinish())) {
            botService.sendInfo("По указанным идентификаторам пользователя и питомца испытательный срок окончен." +
                            "Предупреждение не может быть отправлено.", ProbationDataType.TEXT, volunteerChatId);
            return;
        }
        if (probation.getMessage() != null ) {
            if (!probation.getMessage().isEmpty()) {
                botService.sendInfo("Для пользователя уже имеется сообщение. Повторите отправку своего предупреждения позднее.",
                        ProbationDataType.TEXT, volunteerChatId);
                return;
            }
        }
        probation.setMessage(message);
        probationRepository.saveAndFlush(probation);
        botService.sendInfo("Ваше предупреждение принято для отправки пользователю.",
                ProbationDataType.TEXT, volunteerChatId);
    }

    /**
     * Возвращает все телефоны пользователя в одной строке через запятую.
     * Если нет ни одного телефона, то возвращается пустая строка.
     * @param user пользователь
     * @return телефоны пользователя
     */
    String getTelephonesByUser(User user) {
        Set<Contact> contacts = user.getContacts();
        if (contacts == null) return "";
        StringBuilder result = new StringBuilder();
        for (Contact contact : contacts) {
            if (contact.getType() == ContactType.TELEPHONE) {
                if (!result.isEmpty()) result.append(", ");
                result.append(contact.getValue());
            }
        }
        if (result.isEmpty()) return "";
        return result.toString();
    }

    String getEmailsByUser(User user) {
        Set<Contact> contacts = user.getContacts();
        if (contacts == null) return "";
        StringBuilder result = new StringBuilder();
        for (Contact contact : contacts) {
            if (contact.getType() == ContactType.EMAIL) {
                if (!result.isEmpty()) result.append(", ");
                result.append(contact.getValue());
            }
        }
        if (result.isEmpty()) return "";
        return result.toString();
    }

    String getAddressesByUser(User user) {
        Set<Contact> contacts = user.getContacts();
        if (contacts == null) return "";
        StringBuilder result = new StringBuilder();
        for (Contact contact : contacts) {
            if (contact.getType() == ContactType.EMAIL) {
                if (!result.isEmpty()) result.append(";\n");
                result.append(contact.getValue());
            }
        }
        if (result.isEmpty()) return "";
        return result.toString();
    }

    /**
     * функция реализует решение волонтера окончить испытательный срок.
     * @param volunteerChatId идентификатор чата волонтера
     * @throws InterruptedException
     */
    public void finishProbationByVolunteer(long volunteerChatId) throws InterruptedException {
        Map<String, String> questionnaire = botService.startAction("finish_probation", volunteerChatId);
        if (questionnaire.containsKey("interrupt")) return;
        Integer clientId = Integer.parseInt(questionnaire.get("client-id"));
        Integer petId = Integer.parseInt(questionnaire.get("pet-id"));
        boolean success = questionnaire.get("success").equals("y");
        String message = questionnaire.get("message");

        Probation probation = probationRepository.getProbationByClientIdAndPetId(clientId, petId).orElse(null);
        if (probation == null) {
            botService.sendInfo("По указанным идентификаторам пользователя и питомца нет записи по испытательному сроку",
                    ProbationDataType.TEXT, volunteerChatId);
            return;
        }
        long userChatId = probation.getUser().getChatId();
        if (success) {
            String congratulation = String.format("%s, поздравляем с успешным окончанием испытательного срока!", probation.getUser().getName());
            botService.sendInfo(congratulation, ProbationDataType.TEXT, userChatId);
            probation.setSuccess(true);
        } else {
            String regret = String.format("%s, к сожалению испытательный срок не пройден.\n" + message + "\nПо " +
                            "дальнейшим вашим шагам с Вами свяжутся.", probation.getUser().getName());
            botService.sendInfo(regret, ProbationDataType.TEXT, userChatId);
            probation.setResult(message);
        }
        probationRepository.save(probation);
    }

    /**
     * функция реализует решение волонтера продлить испытательный срок.
     * @param volunteerChatId идентификатор чата волонтера
     * @throws InterruptedException
     */
    public void prolongationByVolunteer(long volunteerChatId) throws InterruptedException {
        Map<String, String> questionnaire = botService.startAction("prolongation", volunteerChatId);
        if (questionnaire.containsKey("interrupt")) return;
        Integer clientId = Integer.parseInt(questionnaire.get("client-id"));
        Integer petId = Integer.parseInt(questionnaire.get("pet-id"));
        Integer number = Integer.parseInt(questionnaire.get("number"));
        String message = questionnaire.get("message");

        Probation probation = probationRepository.getProbationByClientIdAndPetId(clientId, petId).orElse(null);
        if (probation == null) {
            botService.sendInfo("По указанным идентификаторам пользователя и питомца нет записи по испытательному сроку",
                    ProbationDataType.TEXT, volunteerChatId);
            return;
        }
        long userChatId = probation.getUser().getChatId();

        probation.setDateFinish(probation.getDateFinish().plusDays(number));
        probationRepository.save(probation);
        message = String.format("%s, решено продлить ваш испытательный срок на %d дней.\n" + message +
                "\nНе забывайте отправлять отчет по продленным дням испытательного срока", probation.getUser().getName(), number);
        botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
    }

    /**
     * Функция реализует команду "Позвать волонтера". Если находится свободный волонтер, и он готов
     * общаться с пользователем, то запускается чат, в котором бот выступает посредником.
     * @param userChatId идентификатор чата пользователя
     * @throws InterruptedException
     */
    public void callVolunteer(long userChatId) throws InterruptedException {
        List<User> volunteers = userRepository.findUsersByVolunteerActiveIsTrue();
        if (volunteers == null || volunteers.size() == 0) {
            botService.sendInfo("К сожалению свободных волонтеров нет. Попробуйте связаться позднее.", ProbationDataType.TEXT, userChatId);
            return;
        }
        int minutes = 2;
        botService.sendInfo(String.format("Идет поиск свободных волонтеров... пожалуйста, подождите... (не более %d минут)", minutes), ProbationDataType.TEXT, userChatId);
        String message = "Пользователь хочет открыть чат. Кто готов пообщаться?";
        Long volunteerChatId = startConcurrentQuery(userChatId, volunteers, message, minutes);
        if (volunteerChatId == null) {
            botService.sendInfo("К сожалению свободных волонтеров нет. Попробуйте связаться позднее.", ProbationDataType.TEXT, userChatId);
            return;
        }
        botService.createChat(userChatId, volunteerChatId);
    }

    private void sendTasksToEmployees(List<Probation> probations, String taskString) {
        List<User> freeVolunteers = userRepository.findUsersByVolunteerActiveIsTrue();
        User adminEmployee = userRepository.findFirstByAdministratorIsTrueAndChatIdGreaterThan(0L).orElse(null);
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
            botService.sendInfo(String.format(message, user.getName(),
                            getTelephonesByUser(user),
                            probation.getPet().getNickname()),
                    ProbationDataType.TEXT,
                    chatId);
        }
    }

    /**
     * Отправляет волонтерам сообщения: выяснить причины, по которым клиент перестал
     * посылать отчеты по питомцу. Отправка сообщения происходит в том случае, если
     * клиент не посылал отчеты более 2-х суток (расчет интервала идет от полуночи текущего дня
     * в обратную сторону).
     *
     * @throws NotFoundAdministrator вызывается в случае, когда в базе нет ни одного администратора.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void remindAboutReportProblem() {
        List<Probation> probations = probationRepository.getProbationsOnReportProblem();
        if (probations == null) return;
        sendTasksToEmployees(probations, "Требуется выяснить, почему клиент перестал посылать отчеты по питомцу.");
    }

    /**
     * Отправляет волонтерам сообщения с требованием принять решение по испытательному сроку.
     */
    @Scheduled(cron = "0 0 12 * * *")
    public void checkFinishProbation() {
        List<Probation> probations = probationRepository.findProbationByDateFinishBeforeAndSuccessIsFalseAndResultEquals(LocalDateTime.now(), "");
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
                    (support.getType() == SupportType.CALL ? "Телефоны: " + getTelephonesByUser(support.getUser()) : "");
            botService.sendInfo(message, ProbationDataType.TEXT, support.getVolunteer().getChatId());
            if (support.getVolunteer().isVolunteerActive()) {
                User volunteer = support.getVolunteer();
                volunteer.setVolunteerActive(false);
                userRepository.save(volunteer);
                botService.sendInfo("Вы переведены в состояние 'занят'!", ProbationDataType.TEXT, volunteer.getChatId());
            }
        }
    }

    public void sendGreeting(long chatId, LocalDateTime newVisit) {
        User user = userRepository.findUserByChatId(chatId).orElse(null);
        if (user != null) {
            LocalDateTime lastVisit = user.getLastVisit();
            if (lastVisit == null || lastVisit.toLocalDate().atStartOfDay().compareTo(newVisit.toLocalDate().atStartOfDay()) != 0) {
                botService.sendInfo(String.format("Добро пожаловать, %s", user.getName()), ProbationDataType.TEXT, chatId);
            }
            user.setLastVisit(newVisit);
            userRepository.saveAndFlush(user);
        }
    }

    public void processCommand(String message, long chatId) {
        try {
            sendGreeting(chatId, LocalDateTime.now());
            if (!checkAccess(chatId, message)) {
                return;
            }
            Map<String, String> result = null;
            switch (message) {
                case "/info":
                    result = botService.startAction("menu_info", chatId);
                    runMenuCommand(result, chatId);
                    break;
                case "/consultation":
                    result = botService.startAction("menu_consultation", chatId);
                    runMenuCommand(result, chatId);
                    break;
                case "/keeping":
                    result = botService.startAction("menu_keeping_pet", chatId);
                    runMenuCommand(result, chatId);
                    break;
                case "/pet":
                    result = botService.startAction("menu_choose_pet", chatId);
                    runMenuCommand(result, chatId);
                    break;
                case "/volunteer":
                    wantToBecomeVolunteer(chatId);
                case "/call":
                    runMenuCommand(Map.of("command", "empty"), chatId);
                    break;
                case "/chat":
                    callVolunteer(chatId);
                    break;
                case "/show":
                    showSpecialCommands(chatId);
                    break;
                case "/show_chat_id":
                    botService.sendInfo(String.valueOf(chatId), ProbationDataType.TEXT, chatId);
                    break;

                case "/state":
                    getState(chatId);
                    break;
                case "/active":
                    setState(chatId, true);
                    break;
                case "/busy":
                    setState(chatId, false);
                    break;
                case "/get_user":
                    getInfoUserByUserId(chatId);
                    break;
                case "/find_user":
                    getInfoUserByUserTelephone(chatId);
                    break;
                case "/get_user_probation":
                    getInfoByProbationId(chatId);
                    break;
                case "/transfer":
                    transferPet(chatId);
                    break;
                case "/prolongation":
                    prolongationByVolunteer(chatId);
                    break;
                case "/finish_probation":
                    finishProbationByVolunteer(chatId);
                    break;
                case "/message":
                    prepareWarningByVolunteer(chatId);
                    break;

                case "/add_pet":
                    addPet(chatId);
                    break;
                case "/add_photo_pet":
                    addPetPhoto(chatId);
                    break;
                case "/set_volunteer":
                    setVolunteerPosition(chatId, true);
                    break;
                case "/reset_volunteer":
                    setVolunteerPosition(chatId, false);
                    break;

                default:
                    botService.sendInfo("Неизвестная команда. Выберите команду из 'Menu'", ProbationDataType.TEXT, chatId);
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
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
            File file = getFileFromResource((String) entry.getKey());
            if (!file.isFile()) continue;
            Object infoSending = file;
            if ((ProbationDataType) entry.getValue() == ProbationDataType.TEXT) {
                if (!file.getName().matches("^(.+\\.txt)$")) {
                    logger.error("Error: file resource <{}> was specified as text file (function 'sendShetlerInfoByCommand()')", file.getName());
                    continue;
                }
                try {
                    List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                    infoSending = String.join(System.lineSeparator(), lines);
                } catch (IOException e) {
                    logger.error("Read error: <{}> (function 'sendShetlerInfoByCommand()'). <{}>", file.getName(), e.getMessage());
                    continue;
                }
            }
            botService.sendInfo(infoSending, (ProbationDataType) entry.getValue(), chatId);
        }
    }

    private File getFileFromResource(String pathResource) {
        if (pathResource.isEmpty()) return null;
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(pathResource);
        if (resource == null) return null;
        File file = new File(resource.getFile());
        return file;
    }

    /**
     * Осуществляется вызов необходимой функции в зависимости от строкового идентификатора команды.
     *
     * @param command идентификатор команды
     * @param chatId  идентификатор чата
     */
    public void runCommands(String command, Long chatId) throws InterruptedException {
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
            case "paperwork":
                sendShetlerInfoByCommand(animalShetlerInfoService.getPaperwork(), chatId);
                break;
            case "rules_meet":
                sendShetlerInfoByCommand(animalShetlerInfoService.getRulesOfFirstContact(), chatId);
                break;
            case "transportation":
                sendShetlerInfoByCommand(animalShetlerInfoService.getTransportationAnimal(), chatId);
                break;
            case "cynologist_advices":
                sendShetlerInfoByCommand(animalShetlerInfoService.getInitialHandlingWithAnimal(), chatId);
                break;
            case "cynologist_references":
                sendShetlerInfoByCommand(animalShetlerInfoService.getCinologistsRecommendations(), chatId);
                break;
            case "refusal_causes":
                sendShetlerInfoByCommand(animalShetlerInfoService.getRefusingReasons(), chatId);
                break;
            case "puppy":
                sendShetlerInfoByCommand(animalShetlerInfoService.getRecommendationsHomeForPuppy(), chatId);
                break;
            case "adult_dog":
                sendShetlerInfoByCommand(animalShetlerInfoService.getRecommendationsHomeForAdultDog(), chatId);
                break;
            case "handicapped_dog":
                sendShetlerInfoByCommand(animalShetlerInfoService.getRecommendationsHomeForHandicappedDog(), chatId);
                break;

            case "send_report":
                getDataReport(chatId, ProbationDataType.DOCUMENT);
                break;
            case "send_photo":
                getDataReport(chatId, ProbationDataType.PHOTO);
                break;
            case "form_daily_report":
                sendShetlerInfoByCommand(animalShetlerInfoService.getPetReport(), chatId);
                break;
            case "see_pets":
                seePets(chatId);
                break;
            case "choose_pet":
                choosePet(chatId);
                break;
            case "chat":
                callVolunteer(chatId);
                break;
            case "phone_call":
                botService.sendInfo("Эта команда находится в разработке", ProbationDataType.TEXT, chatId);
                break;
            case "empty":
                botService.sendInfo("Эта команда находится в разработке", ProbationDataType.TEXT, chatId);
                break;
            default:
                throw new NotFoundCommand(command);
        }
    }

    private void runMenuCommand(Map<String, String> result, long chatId) {
        if (result.containsKey("interrupt")) return;
        if (result.containsKey("command")) {
            try {
                runCommands(result.get("command"), chatId);
            } catch (InterruptedException e) {
                logger.info(e.getMessage());
            }
        } else {
            callErrorKeyMap(result, "processCommand() -> не найден ожидаемый ключ: " + result.toString());
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

    private String getPathStorage(long chatId) {
        String filePath = animalShetlerProperties.getPathStorage();
        if (filePath.isEmpty()) {
            String message = "Не задан путь к хранилищу. Свяжитесь с администратором";
            logger.error("Не задан путь к хранилищу");
            if (chatId != 0) {
                botService.sendInfo(message, ProbationDataType.TEXT, chatId);
            }
            return null;
        }
        File dir = new File(filePath);
        if (!dir.exists()) {
            String message = "Путь к хранилищу задан с ошибкой. Свяжитесь с администратором";
            logger.error("Путь к хранилищу не существует");
            if (chatId != 0) {
                botService.sendInfo(message, ProbationDataType.TEXT, chatId);
            }
            return null;
        }
        return filePath;
    }

    private boolean downloadFileFromTG(String fileId, String relativePath, long userChatId) {
        try {
            String filePath = getPathStorage(userChatId);
            if (filePath == null) return false;

            Path file = new File(filePath + relativePath).toPath();
            Files.createDirectories(file.getParent());
            Files.deleteIfExists(file);

            URL urlGettingPath = new URL("https://api.telegram.org/bot" + telegramBot.getToken() + "/getFile?file_id=" + fileId);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlGettingPath.openStream()));
            String response = bufferedReader.readLine();

            JsonElement element = JsonParser.parseString(response);
            JsonObject root = element.getAsJsonObject();
            JsonObject result = root.getAsJsonObject("result");
            String file_path = result.get("file_path").getAsString();

            InputStream inputStream = new URL("https://api.telegram.org/file/bot" + telegramBot.getToken() + "/" + file_path).openStream();
            OutputStream outputStream = Files.newOutputStream(file, CREATE_NEW);
            inputStream.transferTo(outputStream);
            inputStream.close();
            outputStream.close();
            return true;
        } catch (IOException e) {
            logger.error("Ошибка выполнения функции downloadFileFromTG():" + e.getMessage() + " (" + e.getClass() + ")");
            botService.sendInfo("Ошибка при загрузке вашего файла. Попробуйте позднее", ProbationDataType.TEXT, userChatId);
            return false;
        }
    }

    public void getDataReport(long userChatId, ProbationDataType type) throws InterruptedException {
        User user = userRepository.findUserByChatId(userChatId).orElse(null);
        if (user == null) {
            String message = String.format("Отчет не может быть принят, так как ваш идентификатор чата %d не зарегистрирован в базе. " +
                    "\nДля решения проблемы закажите обратный звонок с сотрудником", userChatId);
            botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
            return;
        }
        Map<String, String> questionnaire = botService.startAction("data_report", userChatId);
        if (questionnaire.containsKey("interrupt")) return;
        Integer petId = Integer.parseInt(questionnaire.get("pet-id"));
        String[] fileIdAndFileName = questionnaire.get("file-id").split("::");

        Probation probation = probationRepository.getProbationByClientIdAndPetId(user.getId(), petId).orElse(null);
        if (probation == null) {
            String message = String.format("По указанным идентификаторам пользователя '%d' и питомца '%d' нет записи по " +
                    "испытательному сроку. \nДля решения проблемы закажите обратный звонок с сотрудником", user.getId(), petId);
            botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
            return;
        }
        LocalDateTime currentDateTime = LocalDateTime.now();
        if (currentDateTime.isBefore(probation.getDateBegin()) || currentDateTime.isAfter(probation.getDateFinish())) {
            String message = "Текущая дата находится вне интервала испытательного срока. Получение данных прервано.";
            botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
            return;
        }
        if (fileIdAndFileName.length != 2) {
            logger.error("Неверно передан параметр для загрузки файла: " + fileIdAndFileName);
            botService.sendInfo("Передача файла прервана из-за внутренней ошибки. Сообщите о проблеме администратору.", ProbationDataType.TEXT, userChatId);
            return;
        }
        String relativePath = String.valueOf(probation.getId()) + "/" + LocalDate.now().toString() + "/" + fileIdAndFileName[1];
        if (!downloadFileFromTG(fileIdAndFileName[0], relativePath, userChatId)) {
            logger.error(String.format("Ошибка загрузки файла из telegram: file_id: %s, relativePath: %s", fileIdAndFileName[0], relativePath));
            return;
        }

        // подготовка и запись в БД
        boolean photoReceived = type == ProbationDataType.PHOTO;
        boolean documentReceived = type == ProbationDataType.DOCUMENT;

        LocalDateTime beginDay = currentDateTime.toLocalDate().atStartOfDay();
        LocalDateTime endDay = beginDay.plusDays(1).minusSeconds(1);
        ProbationJournal probationJournal = probationJournalRepository.findProbationJournalByProbationEqualsAndDateAfterAndDateBefore(
                probation, beginDay, endDay).orElse(null);
        if (probationJournal == null) {
            probationJournal = new ProbationJournal(currentDateTime, photoReceived, documentReceived);
            probationJournal.setProbation(probation);
        } else {
            probationJournal.setPhotoReceived(photoReceived || probationJournal.isPhotoReceived());
            probationJournal.setReportReceived(documentReceived || probationJournal.isReportReceived());
        }
        ProbationData probationData = new ProbationData(type, relativePath, probationJournal);
        Set<ProbationData> dataSet = probationJournal.getProbationDataSet();
        if (dataSet == null) {
            dataSet = new HashSet<>();
            probationJournal.setProbationDataSet(dataSet);
        }
        dataSet.add(probationData);
        probationJournalRepository.save(probationJournal);
        String message = "";
        if (type == ProbationDataType.PHOTO) {
            message = "Ваша фотография принята.";
        } else if (type == ProbationDataType.DOCUMENT) {
            message = "Ваш отчет принят.";
        }
        if (!message.isEmpty()) {
            botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
        }
    }

    public void choosePet(long userChatId) throws InterruptedException {
        User adminEmployee = userRepository.findFirstByAdministratorIsTrueAndChatIdGreaterThan(0L).orElse(null);
        if (adminEmployee == null) {
            throw new NotFoundAdministrator();
        }
        User user = userRepository.findUserByChatId(userChatId).orElse(null);
        boolean isRequiredDataUser = true;
        if (user != null) {
            String phones = getTelephonesByUser(user);
            if (!phones.isEmpty()) {
                String message = String.format("Найден пользователь с привязкой к данному чату." +
                        "\nИмя: %s \nТелефоны: %s", user.getName(), phones);
                botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
                Map<String, String> result = botService.startAction("verify_data_user", userChatId);
                if (result.containsKey("interrupt")) return;
                if (result.containsKey("answer") && result.get("answer").equals("y")) isRequiredDataUser = false;
            }
        }
        if (isRequiredDataUser) {
            Map<String, String> result = botService.startAction("data_user", userChatId);
            if (result.containsKey("interrupt")) return;
            String name = result.get("name");
            String phone = result.get("telephone");

            if (phone.length() == 10) phone = "7" + phone;
            else if (phone.length() == 12) phone = phone.substring(1);

            if (!replaceUserNameAndPhone(user, name, phone, userChatId)) {
                botService.sendInfo("Произошла ошибка записи данных. Выполнение команды пришлось прервать. " +
                        "Сообщите, пожалуйста, об ошибке в тех.поддержку", ProbationDataType.TEXT, userChatId);
                return;
            }
            user = userRepository.findUserByChatId(userChatId).orElse(null);
            if (user == null) {
                botService.sendInfo("Произошла ошибка записи данных. Выполнение команды пришлось прервать. " +
                        "Сообщите, пожалуйста, об ошибке в тех.поддержку", ProbationDataType.TEXT, userChatId);
                return;
            }
        }
        Map<String, String> result = botService.startAction("ask_pet_id", userChatId);
        if (result.containsKey("interrupt")) return;
        Integer petId = Integer.parseInt(result.get("pet-id"));
        Pet pet = petRepository.findById(petId).orElse(null);
        if (pet == null) {
            String message = "Вы ввели несуществующий идентификатор питомца. " +
                    "Для повторной попытки следует запустить команду заново.";
            botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
            return;
        }
        if (!pet.getLookingForOwner()) {
            String message = "Указанный питомец уже нашел хозяина. " +
                    "Для выбора другого питомца следует запустить команду заново.";
            botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
            return;
        }
        if (!sendDataPet(pet, userChatId, true)) {
            botService.sendInfo("Произошла ошибка отправки данных. Выполнение команды пришлось прервать. " +
                    "Сообщите, пожалуйста, об ошибке в тех.поддержку", ProbationDataType.TEXT, userChatId);
            return;
        }
        result = botService.startAction("verify_pet_id", userChatId);
        if (result.containsKey("interrupt")) return;
        if (result.get("answer").equalsIgnoreCase("n")) {
            String message = "Для выбора другого питомца следует запустить команду заново.";
            botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
            return;
        }
        pet.setLookingForOwner(false);
        petRepository.save(pet);
        botService.sendInfo("Пожалуйста, немного подождите (не более 2 минут)... идет ваша постановка на оформление питомца...", ProbationDataType.TEXT, userChatId);

        List<User> volunteers = userRepository.findUsersByVolunteerActiveIsTrue();
        Long employeeChatId = null;
        if (volunteers != null && volunteers.size() > 0) {
            employeeChatId = startConcurrentQuery(userChatId, volunteers, "Требуется оформить питомца. Кто возьмет работу?", 2);
        }
        if (employeeChatId == null) {
            employeeChatId = adminEmployee.getChatId();
        }
        String message = String.format("Требуется оформить питомца." +
                "\nКлиент: %s (идентификатор: %d)\n" +
                "Телефоны: %s, \n" +
                "Идентификатор питомца: %d, \n" +
                "Кличка питомца: %s.", user.getName(), user.getId(), getTelephonesByUser(user), pet.getId(), pet.getNickname());
        botService.sendInfo(message, ProbationDataType.TEXT, employeeChatId);
        botService.sendInfo("Благодарим за ожидание. С вами свяжутся в течение 1-2 дней", ProbationDataType.TEXT, userChatId);
    }

    private boolean replaceUserNameAndPhone(User user, String name, String phone, long chatId) {
        try {
            if (user == null) {
                user = new User(name, false, false, false, chatId, LocalDateTime.now(), "");
            } else {
                user.setName(name);
            }
            Set<Contact> contacts = user.getContacts();
            if (contacts == null) {
                contacts = new HashSet<>();
            }
            Contact contact = new Contact(ContactType.TELEPHONE, phone, user);
            for (Contact obj : contacts) {
                if (obj.getType() == ContactType.TELEPHONE) contacts.remove(obj);
            }
            contacts.add(contact);
            user.setContacts(contacts);
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            logger.error("В функции replaceUserNameAndPhone() произошло исключение: " + e.getMessage() + " (" + e.getClass() + ")");
            return false;
        }
    }

    private boolean sendDataPet(Pet pet, long chatId, boolean onlyFirstPhoto) {
        try {
            String filePath = getPathStorage(chatId);
            if (filePath == null) return false;

            String petId = "Идентификатор питомца: " + pet.getId();
            String description = String.format("Кличка: %s\nПорода: %s\nВозраст: %s\nХарактер: %s",
                    pet.getNickname(), pet.getBreed(), pet.getAge(), pet.getCharacter());
            Set<PhotoPets> photoPets = pet.getPhotoPets();
            PhotoPets photo = null;
            if (photoPets != null && photoPets.size() > 0) {
                photo = photoPets.stream().toList().get(0);
            }

            botService.sendInfo(petId, ProbationDataType.TEXT, chatId);
            File file = null;
            if (onlyFirstPhoto && photo != null) {
                file = new File(filePath + photo.getPhoto());
                if (file.exists()) {
                    botService.sendInfo(file, ProbationDataType.PHOTO, chatId);
                }
            } else if (photoPets != null) {
                for (PhotoPets photoPet : photoPets) {
                    file = new File(filePath + photoPet.getPhoto());
                    if (file.exists()) {
                        botService.sendInfo(file, ProbationDataType.PHOTO, chatId);
                    }
                }
            }
            botService.sendInfo(description, ProbationDataType.TEXT, chatId);
            return true;
        } catch (Exception e) {
            logger.error("В функции sendDataPet() произошло исключение: " + e.getMessage() + " (" + e.getClass() + ")");
            return false;
        }
    }

    /**
     * Создает испытательный срок для клиента, указанного волонтером.
     * @param volunteerChatId идентификатор чата волонтера
     * @throws InterruptedException
     */
    public void transferPet(long volunteerChatId) throws InterruptedException {
        Map<String, String> result = botService.startAction("setup_probation", volunteerChatId);
        if (result.containsKey("interrupt")) return;
        Integer clientId = Integer.parseInt(result.get("client-id"));
        Integer petId = Integer.parseInt(result.get("pet-id"));
        long number = Integer.parseInt(result.get("number"));

        User user = userRepository.findUserById(clientId).orElse(null);
        if (user == null) {
            botService.sendInfo("По указанному идентификатору пользователь не найден",
                    ProbationDataType.TEXT, volunteerChatId);
            return;
        }
        Pet pet = petRepository.findById(petId).orElse(null);
        if (pet == null) {
            botService.sendInfo("По указанному идентификатору питомец не найден",
                    ProbationDataType.TEXT, volunteerChatId);
            return;
        }
        LocalDateTime beginDate = LocalDateTime.now().toLocalDate().atStartOfDay().plusDays(1);
        Probation probation = probationRepository.getProbationByClientIdAndPetId(clientId, petId).orElse(null);
        if (probation != null && beginDate.plusMinutes(1).isAfter(probation.getDateBegin()) && beginDate.isBefore(probation.getDateFinish())) {
            botService.sendInfo("Испытательный срок уже назначен",
                    ProbationDataType.TEXT, volunteerChatId);
            return;
        }
        probation = new Probation(beginDate, beginDate.plusDays(number), false, user, pet);
        probationRepository.save(probation);

        String beginDateString = beginDate.toLocalDate().toString() + " " + beginDate.toLocalTime().toString();
        String endDateString = probation.getDateFinish().toLocalDate().toString() + " " + probation.getDateFinish().toLocalTime().toString();
        botService.sendInfo(String.format("Испытательный срок (id = %d) успешно назначен.", probation.getId()), ProbationDataType.TEXT, volunteerChatId);
        String message = String.format("%s, вам назначен испытательный срок по питомцу %s с %s по %s.",
                user.getName(), pet.getNickname(), beginDateString, endDateString);
        botService.sendInfo(message, ProbationDataType.TEXT, user.getChatId());
    }

    /**
     * Отображает данные по питомцам, которые еще не нашли хозяев.
     * @param userChatId идентификатор чата пользователя.
     * @throws InterruptedException
     */
    public void seePets(long userChatId) throws InterruptedException {
        final int COUNT_ON_PAGE = 3;
        Integer totalPets = petRepository.getTotalPetsWhoLookingForOwner().orElse(null);
        if (totalPets == null || totalPets == 0) {
            botService.sendInfo("Питомцев для просмотра нет", ProbationDataType.TEXT, userChatId);
            return;
        }
        int pages = totalPets / COUNT_ON_PAGE + totalPets % COUNT_ON_PAGE == 0 ? 0 : 1;
        for (int i = 0; i < pages; i++) {
            if (i > 0) {
                Map<String, String> result = botService.startAction("continue_view", userChatId);
                if (result.containsKey("interrupt")) return;
                if (result.get("answer").equals("n")) return;
            }
            PageRequest pageRequest = PageRequest.of(i, COUNT_ON_PAGE);
            List<Pet> pets = petRepository.findAllByLookingForOwnerIsTrue(pageRequest).getContent();
            for (Pet pet : pets) {
                if (!sendDataPet(pet, userChatId, false)) {
                    botService.sendInfo("Ошибка при передаче данных. Команда прервана.", ProbationDataType.TEXT, userChatId);
                    return;
                }
            }
        }
        botService.sendInfo("Надеемся, что Вам кто-нибудь понравился.", ProbationDataType.TEXT, userChatId);
    }

    public void showSpecialCommands(long chatId) {
        StringBuilder helpVolunteerCommands = new StringBuilder();
        StringBuilder helpAdministratorCommands = new StringBuilder();

        for (Map.Entry<String, String> entry : volunteerCommands.entrySet()) {
            if (!helpVolunteerCommands.isEmpty()) helpVolunteerCommands.append("\n");
            helpVolunteerCommands.append(String.format("%s  (%s)", entry.getKey(), entry.getValue()));
        }

        for (Map.Entry<String, String> entry : administratorCommands.entrySet()) {
            if (!helpAdministratorCommands.isEmpty()) helpAdministratorCommands.append("\n");
            helpAdministratorCommands.append(String.format("%s  (%s)", entry.getKey(), entry.getValue()));
        }

        if (isAdministrator(chatId)) {
            botService.sendInfo(helpAdministratorCommands.toString(), ProbationDataType.TEXT, chatId);
            botService.sendInfo(helpVolunteerCommands.toString(), ProbationDataType.TEXT, chatId);
        } else if (isVolunteer(chatId)) {
            botService.sendInfo(helpVolunteerCommands.toString(), ProbationDataType.TEXT, chatId);
        } else {
            botService.sendInfo("Эта команда доступна только сотрудникам", ProbationDataType.TEXT, chatId);
        }
    }

    private boolean isVolunteer(long userChatId) {
        Integer result = userRepository.isVolunteer(userChatId).orElse(0);
        return result > 0;
    }

    private boolean isAdministrator(long userChatId) {
        Integer result = userRepository.isAdministrator(userChatId).orElse(0);
        return result > 0;
    }

    private boolean isEmployee(long userChatId) {
        Integer result = userRepository.isEmployee(userChatId).orElse(0);
        return result > 0;
    }

    private boolean checkAccess(long chatId, String command) {
        boolean result = false;
        String position;
        if (volunteerCommands.containsKey(command)) position = "volunteer";
        else if (administratorCommands.containsKey(command)) position = "administrator";
        else return true;

        switch (position) {
            case "volunteer":
                result = isVolunteer(chatId);
            case "administrator":
                result = result || isAdministrator(chatId);
        }
        if (!result) {
            botService.sendInfo("Эта команда вам недоступна", ProbationDataType.TEXT, chatId);
        }
        return  result;
    }

    private boolean isActiveState(long chatId) {
        Integer result = userRepository.isActive(chatId).orElse(0);
        return result > 0;
    }

    public void getState(long chatId) {
        String state = isActiveState(chatId) ? "СВОБОДЕН" : "ЗАНЯТ";
        botService.sendInfo("Ваш состояние: " + state, ProbationDataType.TEXT, chatId);
    }

    public void setState(long chatId, boolean active) {
        User user = userRepository.findUserByChatId(chatId).orElse(null);
        if (user == null) {
            botService.sendInfo("Ваш идентификатор чата не зарегистрирован в базе", ProbationDataType.TEXT, chatId);
            return;
        }
        if (active == user.isVolunteerActive()) {
            botService.sendInfo("Ваше состояние осталось без изменений", ProbationDataType.TEXT, chatId);
        }
        user.setVolunteerActive(active);
        userRepository.save(user);
        String state = active ? "СВОБОДЕН" : "ЗАНЯТ";
        botService.sendInfo("Ваше новое состояние: " + state, ProbationDataType.TEXT, chatId);
    }

    public void setVolunteerPosition(long chatId, boolean volunteer) throws InterruptedException {
        try {
            Map<String, String> questionnaire = botService.startAction("ask_user_id", chatId);
            if (questionnaire.containsKey("interrupt")) return;
            Integer userId = Integer.parseInt(questionnaire.get("user-id"));

            User user = userRepository.findUserById(userId).orElse(null);
            if (user == null) {
                botService.sendInfo("Идентификатор пользователя не найден в базе", ProbationDataType.TEXT, chatId);
                return;
            }
            if (user.isVolunteer() == volunteer) return;
            user.setVolunteer(volunteer);
            userRepository.save(user);
            if (volunteer) {
                botService.sendInfo("Поздравляем! Вы приняты в волонтеры.", ProbationDataType.TEXT, user.getChatId());
            } else {
                botService.sendInfo("К сожалению вы больше не являетесь волонтером.", ProbationDataType.TEXT, user.getChatId());
            }
        } catch (Exception e){
            logger.error("В функции setVolunteerPosition() произошло исключение: " + e.getMessage() + " (" + e.getClass() + ")");
            askableServiceObjects.resetServiceObjects(chatId);
            botService.sendInfo("Произошла внутренняя ошибка. Команда будет прервана. " +
                    "Сообщите, пожалуйста, о проблеме администратору", ProbationDataType.TEXT, chatId);
        }
    }

    private String getAllInfoByUser(User user) {
        StringBuilder result = new StringBuilder();
        LocalDateTime dateVisit = user.getLastVisit().truncatedTo(ChronoUnit.SECONDS);
        String dateVisitString = dateVisit.toLocalDate().toString() + " " + dateVisit.toLocalTime().toString();
        result.append("Имя: " + user.getName() +
                "\nТелефоны: " + getTelephonesByUser(user) +
                "\nАдреса: " + getAddressesByUser(user) +
                "\nEmails: " + getEmailsByUser(user) +
                "\nЭто волонтер: " + (user.isVolunteer() ? "ДА" : "НЕТ") +
                "\nПоследнее посещение чата: " + dateVisitString
        );
        return result.toString();
    }

    public void getInfoUserByUserId(long chatId) throws InterruptedException {
        Map<String, String> questionnaire = botService.startAction("ask_user_id", chatId);
        if (questionnaire.containsKey("interrupt")) return;
        Integer userId = Integer.parseInt(questionnaire.get("user-id"));

        User user = userRepository.findUserById(userId).orElse(null);
        if (user == null) {
            botService.sendInfo("Идентификатор пользователя не найден в базе", ProbationDataType.TEXT, chatId);
            return;
        }
        String info = getAllInfoByUser(user);
        botService.sendInfo(info, ProbationDataType.TEXT, chatId);
    }

    public void getInfoUserByUserTelephone(long chatId) throws InterruptedException {
        Map<String, String> questionnaire = botService.startAction("ask_user_telephone", chatId);
        if (questionnaire.containsKey("interrupt")) return;
        String userTelephone = questionnaire.get("user-telephone");

        User user = findUserByPhoneNumber(userTelephone);
        if (user == null) {
            botService.sendInfo("Телефон пользователя не найден в базе", ProbationDataType.TEXT, chatId);
            return;
        }
        String info = getAllInfoByUser(user);
        botService.sendInfo(info, ProbationDataType.TEXT, chatId);
    }

    public void getInfoByProbationId(long chatId) throws InterruptedException {
        Map<String, String> questionnaire = botService.startAction("ask_probation_id", chatId);
        if (questionnaire.containsKey("interrupt")) return;
        Integer probationId = Integer.parseInt(questionnaire.get("probation-id"));
        Integer lastRecordNumber = Integer.parseInt(questionnaire.get("number"));

        Probation probation = probationRepository.findProbationById(probationId).orElse(null);
        if (probation == null) {
            botService.sendInfo("Идентификатор испытательного срока не найден в базе", ProbationDataType.TEXT, chatId);
            return;
        }
        List<ProbationJournal> records = probationJournalRepository.findAllByProbationEqualsOrderByDateDesc(probation);
        if (records == null || records.size() == 0) {
            botService.sendInfo("Записей нет", ProbationDataType.TEXT, chatId);
            return;
        }
        int count = 0;
        StringBuilder portion = new StringBuilder();
        for (ProbationJournal probationJournal : records) {
            if (lastRecordNumber == 0) break;
            if (count == 5) {
                botService.sendInfo(portion.toString(), ProbationDataType.TEXT, chatId);
                count = 0;
                portion = new StringBuilder();
            }
            count++;
            String photoReceived = probationJournal.isPhotoReceived() ? "фото: ДА" : "фото: НЕТ";
            String reportReceived = probationJournal.isReportReceived() ? "отчет: ДА" : "отчет: НЕТ";
            portion.append(probationJournal.getDate().toLocalDate().toString() + "; " + reportReceived + "; " + photoReceived + "\n");
            lastRecordNumber--;
        }
        if (count > 0) {
            botService.sendInfo(portion.toString(), ProbationDataType.TEXT, chatId);
        }
    }

    public void addPet(long chatId) throws InterruptedException {
        Map<String, String> questionnaire = botService.startAction("ask_pet", chatId);
        if (questionnaire.containsKey("interrupt")) return;
        String nickname = questionnaire.get("nickname");
        String breed = questionnaire.get("breed");
        String character = questionnaire.get("character");
        String dateString = questionnaire.get("birthday");

        LocalDate birthday = null;
        if (!dateString.equals("-")) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            try {
                birthday = LocalDate.parse(dateString, formatter);
            } catch (DateTimeParseException e) {
                logger.error(String.format("Ошибка парсинга даты из строки '%s' (addPet(), %s", dateString, e.getMessage()));
                botService.sendInfo("Ошибка при обработке вашего ответа. Команда будет прервана. ",
                        ProbationDataType.TEXT, chatId);
                return;
            }
        }
        Pet pet = new Pet(nickname, breed, birthday, character, true);
        pet = petRepository.save(pet);
        String message = "Данные по питомцу успешно записаны. Идентификатор питомца: " + pet.getId();
        botService.sendInfo(message, ProbationDataType.TEXT, chatId);
    }

    public void addPetPhoto(long chatId) throws InterruptedException {
        Map<String, String> questionnaire = botService.startAction("ask_pet_photo", chatId);
        if (questionnaire.containsKey("interrupt")) return;
        Integer petId = Integer.parseInt(questionnaire.get("pet-id"));
        String[] fileIdAdnFileName = questionnaire.get("file-id").split("::");

        Pet pet = petRepository.findById(petId).orElse(null);
        if (pet == null) {
            botService.sendInfo("Питомца с указанным идентификатором не существует", ProbationDataType.TEXT, chatId);
            return;
        }
        if (fileIdAdnFileName.length != 2) {
            logger.error("Неверно передан параметр для загрузки файла: " + fileIdAdnFileName);
            botService.sendInfo("Передача файла прервана из-за внутренней ошибки.", ProbationDataType.TEXT, chatId);
            return;
        }
        String relativePath = "pets/" + String.valueOf(pet.getId()) + "/" + fileIdAdnFileName[1];
        if (!downloadFileFromTG(fileIdAdnFileName[0], relativePath, chatId)) {
            logger.error(String.format("Ошибка загрузки файла из telegram: file_id: %s, relativePath: %s", fileIdAdnFileName[0], relativePath));
            return;
        }
        Set<PhotoPets> photoPets = pet.getPhotoPets();
        if (photoPets == null) {
            photoPets = new HashSet<>();
            pet.setPhotoPets(photoPets);
        }
        PhotoPets photo = new PhotoPets(relativePath);
        photo.setPet(pet);
        photoPets.add(photo);

        petRepository.save(pet);
        botService.sendInfo("Фото записано в базу", ProbationDataType.TEXT, chatId);
    }
}


