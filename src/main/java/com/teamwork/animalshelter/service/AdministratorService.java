package com.teamwork.animalshelter.service;

import com.teamwork.animalshelter.exception.UserExists;
import com.teamwork.animalshelter.exception.WrongPhoneNumber;
import com.teamwork.animalshelter.model.Contact;
import com.teamwork.animalshelter.model.ContactType;
import com.teamwork.animalshelter.model.User;
import com.teamwork.animalshelter.repository.ContactRepository;
import com.teamwork.animalshelter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdministratorService {
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;

    public AdministratorService(UserRepository userRepository, ContactRepository contactRepository) {
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
    }

    public User addAdministrator(String name, String phone, long chatId) {
        String regex = "^(7|(\\+7))?9[\\d]{9}$";
        if (!phone.matches(regex)) {
            throw new WrongPhoneNumber();
        }
        if (phone.length() == 10) phone = "7" + phone;
        else if (phone.length() == 12) phone = phone.substring(1);

        User user = userRepository.findUserByChatId(chatId).orElse(null);
        if (user != null) throw new UserExists(chatId);

        Contact contact = contactRepository.findContactByValueEqualsAndTypeEquals(phone, ContactType.TELEPHONE).orElse(null);
        if (contact != null) throw new UserExists(phone);

        user = new User(name, false, false, true, chatId, null, "");
        Set<Contact> contacts = new HashSet<>();
        contact = new Contact(ContactType.TELEPHONE, phone, user);
        contacts.add(contact);
        user.setContacts(contacts);
        return userRepository.save(user);
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }
}

