package com.teamwork.animalshelter.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.concurrent.ShetlerThread;
import com.teamwork.animalshelter.model.ProbationDataType;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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

            if (update.message() != null && !update.message().text().isEmpty()) {
                long chatId = update.message().chat().id();
                String message = update.message().text();

                if (isCommand(message)) {
                    new ShetlerThread(chatId, askableServiceObjects, () -> userService.processCommand(message, chatId)).start();
                } else {
                    boolean isConcurrentQuery = false;
                    if (message.equals("/+") || message.equalsIgnoreCase("/y") || message.equalsIgnoreCase("/да")) {
                        isConcurrentQuery = askableServiceObjects.setPositiveReactionOfConcurrentQuery(chatId);
                    }
                    if (!isConcurrentQuery) {
                        if (askableServiceObjects.isChatIdForResponse(chatId)) {
                            if (askableServiceObjects.getResponse(chatId).equals("chat")) {
                                askableServiceObjects.addMessageIntoQueueChat(chatId, message);
                            } else if (askableServiceObjects.getResponse(chatId).isEmpty()) {
                                askableServiceObjects.addResponse(chatId, message);
                            }
                        } else {
                            botService.sendInfo("Неизвестная команда. Выберите команду из 'Menu'", ProbationDataType.TEXT, chatId);
                        }
                    }
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private boolean isCommand(String command) {
        switch (command) {
            case "/info":
            case "/consultation":
            case "/pet":
            case "/call":
            case "/chat":
            case "/keeping":
            case "/volunteer":
            case "/show":
                return true;
            default:
                return false;
        }
    }

}
