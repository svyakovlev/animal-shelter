package com.teamwork.animalshelter.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.concurrent.ShetlerThread;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
    private final TelegramBot telegramBot;
    private final AskableServiceObjects askableServiceObjects;
    private final BotService botService;
    private final UserService userService;

    public TelegramBotUpdatesListener(TelegramBot telegramBot,
                                      AskableServiceObjects askableServiceObjects,
                                      BotService botService,
                                      UserService userService) {
        this.telegramBot = telegramBot;
        this.askableServiceObjects = askableServiceObjects;
        this.botService = botService;
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {

            long chatId = update.message().chat().id();
            switch (update.message().text()) {
                case "/info":
                    new ShetlerThread(chatId, askableServiceObjects, () -> botService.processMessageText("/info", chatId));
                    break;
                case "/consultation":
                    break;
                case "/pet":
                    break;
                case "/call":
                    break;
                case "/chat":
                    break;
                case "/keeping":
                    break;
                case "/volunteer":
                    new ShetlerThread(chatId, askableServiceObjects, () -> {
                        try {
                            userService.wantToBecomeVolunteer(chatId);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    break;
                default:

            }

        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
