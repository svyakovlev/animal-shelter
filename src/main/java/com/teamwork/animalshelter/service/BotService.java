package com.teamwork.animalshelter.service;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.stereotype.Service;

@Service
public class BotService {
    private final TelegramBot telegramBot;

    public BotService(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }


}
