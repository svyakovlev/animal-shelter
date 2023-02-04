package com.teamwork.animalshelter.service;

import com.teamwork.animalshelter.model.Contact;
import com.teamwork.animalshelter.model.ContactType;
import com.teamwork.animalshelter.model.ProbationDataType;
import com.teamwork.animalshelter.model.User;
import com.teamwork.animalshelter.repository.ContactRepository;
import com.teamwork.animalshelter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private BotService botService;

    private final UserRepository userRepository;
    private final ContactRepository contactRepository;

    public UserService(BotService botService, UserRepository userRepository, ContactRepository contactRepository) {
        this.botService = botService;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
    }

    private void wantToBecomeVolunteer(long chatId) throws InterruptedException {
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
        user.setChat_id(chatId);
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

        Long adminstratorChatId = startConcurrentQuery(chatId, freeAdministrators, message, 5);

        if (adminstratorChatId == null) {
            message = "Нет свободных администраторов. Закажите обратный звонок.";
            botService.sendInfo(message, ProbationDataType.TEXT, chatId);
            return;
        }
        botService.createChat(chatId, adminstratorChatId);
    }

    private Long startConcurrentQuery (long chatId, List<User> chats, String message, int minutes) {
        return null;
    }

    private User findUserByChatId(long chatId) {
        return userRepository.findUserByChat_id(chatId);
    }

    private User findUserByPhoneNumber(String phoneNumber) {
        Contact contact = contactRepository.findContactByValueAndType(phoneNumber, 1);
        if (contact == null) {
            return null;
        } else {
            return contact.getUser();
        }
    }



}
