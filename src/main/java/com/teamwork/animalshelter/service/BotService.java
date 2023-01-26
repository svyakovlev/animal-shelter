package com.teamwork.animalshelter.service;

import com.teamwork.animalshelter.model.User;
import com.teamwork.animalshelter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BotService {
    UserRepository userRepository;

    public BotService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void sendGreeting(long chatId, LocalDateTime newVisit ) {
        LocalDateTime lastVisit = UserRepository.lastVisit(chatId);
    }

}
