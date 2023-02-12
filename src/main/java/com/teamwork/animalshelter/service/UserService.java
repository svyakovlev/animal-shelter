package com.teamwork.animalshelter.service;

import com.fasterxml.jackson.databind.util.JSONPObject;
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
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.aspectj.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final TelegramBot telegramBot;

    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(BotService botService, AskableServiceObjects askableServiceObjects,
                       UserRepository userRepository, ContactRepository contactRepository,
                       ProbationRepository probationRepository, SupportRepository supportRepository,
                       AnimalShetlerInfoService animalShetlerInfoService, AnimalShetlerProperties animalShetlerProperties,
                       ProbationJournalRepository probationJournalRepository, TelegramBot telegramBot) {
        this.botService = botService;
        this.askableServiceObjects = askableServiceObjects;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.probationRepository = probationRepository;
        this.supportRepository = supportRepository;
        this.animalShetlerInfoService = animalShetlerInfoService;
        this.animalShetlerProperties = animalShetlerProperties;
        this.probationJournalRepository = probationJournalRepository;
        this.telegramBot = telegramBot;
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

        Contact contact = new Contact();

        contact.setUser(user);
        contact.setType(ContactType.TELEPHONE);
        contact.setValue(replayMessage.get("telephone"));

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
     * @param userChatId идентификатор пользователя
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
        return userRepository.findUserByChatId(chatId).get();
    }

    private User findUserByPhoneNumber(String phoneNumber) {
        Contact contact = contactRepository.findContactByValueAndType(phoneNumber, 1);
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

        Probation probation = probationRepository.getProbationByClientIdAndPetId(clientId, petId).get();
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
        if (!probation.getMessage().isEmpty()) {
            botService.sendInfo("Для пользователя уже имеется сообщение. Повторите отправку своего предупреждения позднее.",
                    ProbationDataType.TEXT, volunteerChatId);
            return;
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

        Probation probation = probationRepository.getProbationByClientIdAndPetId(clientId, petId).get();
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

        Probation probation = probationRepository.getProbationByClientIdAndPetId(clientId, petId).get();
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
    @Scheduled(cron = "0 0 9/24 * * *")
    public void remindAboutReportProblem() {
        List<Probation> probations = probationRepository.getProbationsOnReportProblem();
        if (probations == null) return;
        sendTasksToEmployees(probations, "Требуется выяснить, почему клиент перестал посылать отчеты по питомцу.");
    }

    /**
     * Отправляет волонтерам сообщения с требованием принять решение по испытательному сроку.
     */
    @Scheduled(cron = "0 0 12/24 * * *")
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
        User user = userRepository.findUserByChatId(chatId).get();
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
                case "/call":

                    break;
                case "/chat":
                    callVolunteer(chatId);
                    break;
                case "/show":   // показать команды волонтера/админа

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
                break;
            case "send_photo":
                break;
            case "form_daily_report":
                sendShetlerInfoByCommand(animalShetlerInfoService.getPetReport(), chatId);
                break;

            case "see_pets":
                break;
            case "choose_pet":
                break;


            case "chat":
                callVolunteer(chatId);
                break;
            case "phone_call":
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

    private boolean downloadFileFromTG(String fileId, String relativePath, long userChatId) {
        try {
            String filePath = animalShetlerProperties.getPathStorage();
            if (filePath.isEmpty()) {
                String message = "Не задан путь к хранилищу. Свяжитесь с администратором";
                logger.error("Не задан путь к хранилищу");
                botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
                return false;
            }
            File dir = new File(filePath);
            if (!dir.exists()) {
                String message = "Путь к хранилищу задан с ошибкой. Свяжитесь с администратором";
                logger.error("Путь к хранилищу не существует");
                botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
                return false;
            }

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
        Map<String, String> questionnaire = botService.startAction("data_report", userChatId);
        if (questionnaire.containsKey("interrupt")) return;
        Integer petId = Integer.parseInt(questionnaire.get("pet-id"));
        String[] fileIdAdnFileName = questionnaire.get("file-id").split("::");
        if (fileIdAdnFileName.length != 2) {
            logger.error("Неверно передан параметр для загрузки файла: " + fileIdAdnFileName);
            botService.sendInfo("Передача файла прервана из-за внутренней ошибки. Сообщите о проблеме администратору.", ProbationDataType.TEXT, userChatId);
            return;
        }

        User user = userRepository.findUserByChatId(userChatId).get();
        if (user == null) {
            String message = String.format("Отчет не может быть принят, так как ваш идентификатор чата %d не зарегистрирован в базе. " +
                    "\nДля решения проблемы закажите обратный звонок с сотрудником", userChatId);
            botService.sendInfo(message, ProbationDataType.TEXT, userChatId);
            return;
        }
        Probation probation = probationRepository.getProbationByClientIdAndPetId(user.getId(), petId).get();
        if (probation == null) {
            String message = String.format("По указанным идентификаторам пользователя %d и питомца %d нет записи по " +
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

        String relativePath = String.valueOf(probation.getId()) + "/" + LocalDate.now().toString() + "/" + fileIdAdnFileName[1];
        if (!downloadFileFromTG(fileIdAdnFileName[0], relativePath, userChatId)) {
            logger.error(String.format("Ошибка загрузки файла из telegram: file_id: %s, relativePath: %s", fileIdAdnFileName[0], relativePath));
            return;
        }

        // подготовка и запись в БД
        boolean photoReceived = type == ProbationDataType.PHOTO;
        boolean documentReceived = type == ProbationDataType.DOCUMENT;

        ProbationJournal probationJournal = probationJournalRepository.findProbationJournalByProbationIs(probation).get();
        if (probationJournal == null) {
            probationJournal = new ProbationJournal(currentDateTime, photoReceived, documentReceived);
            probationJournal.setProbation(probation);
        } else {
            probationJournal.setPhotoReceived(photoReceived);
            probationJournal.setReportReceived(documentReceived);
        }
        ProbationData probationData = new ProbationData(type, relativePath, probationJournal);
        Set<ProbationData> dataSet = probationJournal.getProbationDataSet();
        if (dataSet == null) dataSet = new HashSet<>();
        dataSet.add(probationData);
        probationJournalRepository.save(probationJournal);
    }

}
