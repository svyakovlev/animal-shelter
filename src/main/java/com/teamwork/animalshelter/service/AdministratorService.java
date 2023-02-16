package com.teamwork.animalshelter.service;

import com.teamwork.animalshelter.exception.WrongPhoneNumber;
import com.teamwork.animalshelter.model.Contact;
import com.teamwork.animalshelter.model.ContactType;
import com.teamwork.animalshelter.model.User;
import com.teamwork.animalshelter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdministratorService {
    private final UserRepository userRepository;

    public AdministratorService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User addAdministrator(String name, String phone, long chatId) {
        String regex = "^(7|(\\+7))?9[\\d]{9}$";
        if (!phone.matches(regex)) {
            throw new WrongPhoneNumber();
        }
        if (phone.length() == 10) phone = "7" + phone;
        else if (phone.length() == 12) phone = phone.substring(1);
        User user = new User(name, false, false, true, chatId, null, "");
        Set<Contact> contacts = new HashSet<>();
        Contact contact = new Contact(ContactType.TELEPHONE, phone);
        contacts.add(contact);
        user.setContacts(contacts);
        return userRepository.save(user);
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }
}

