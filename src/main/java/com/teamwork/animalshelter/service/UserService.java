package com.teamwork.animalshelter.service;

import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.exception.NotFoundChatId;
import com.teamwork.animalshelter.model.*;
import com.teamwork.animalshelter.repository.ContactRepository;
import com.teamwork.animalshelter.repository.ProbationRepository;
import com.teamwork.animalshelter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {

    private BotService botService;
    private AskableServiceObjects askableServiceObjects;

    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final ProbationRepository probationRepository;

    public UserService(BotService botService, AskableServiceObjects askableServiceObjects,
                       UserRepository userRepository, ContactRepository contactRepository,
                       ProbationRepository probationRepository) {
        this.botService = botService;
        this.askableServiceObjects = askableServiceObjects;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.probationRepository = probationRepository;
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

        String message = String.format("Пользователь %s хочеть стать волонтером. Возьмете в работу?", user.getName() );
        if (freeAdministrators == null) {
            message = "Нет свободных администраторов. Закажите обратный звонок.";
            botService.sendInfo(message, ProbationDataType.TEXT, chatId);
            return;
        }

        message = "Идет поиск свободных администраторов. Пожалуйста, подождите... (время ожидания не более 5 минут)";
        botService.sendInfo(message, ProbationDataType.TEXT, chatId);

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
        return userRepository.findUserByChatId(chatId);
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

}
