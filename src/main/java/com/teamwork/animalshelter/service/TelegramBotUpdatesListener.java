package com.teamwork.animalshelter.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.response.SendResponse;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.concurrent.ShetlerThread;
import com.teamwork.animalshelter.model.ProbationDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
    private final TelegramBot telegramBot;
    private final AskableServiceObjects askableServiceObjects;
    private final BotService botService;
    private final UserService userService;
    private final Map<String, String> volunteerCommands;
    private final Map<String, String> administratorCommands;
    private final Map<String, String> mainMenuCommands;
    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    public TelegramBotUpdatesListener(TelegramBot telegramBot,
                                      AskableServiceObjects askableServiceObjects,
                                      BotService botService,
                                      UserService userService,
                                      Map<String, String> volunteerCommands,
                                      Map<String, String> administratorCommands,
                                      Map<String, String> mainMenuCommands) {
        this.telegramBot = telegramBot;
        this.askableServiceObjects = askableServiceObjects;
        this.botService = botService;
        this.userService = userService;
        this.volunteerCommands = volunteerCommands;
        this.administratorCommands = administratorCommands;
        this.mainMenuCommands = mainMenuCommands;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {

            if (update.message() != null) {
                long chatId = update.message().chat().id();
                if (update.message().text() == null) {
                    String file_id = "";
                    String file_name = "";
                    if (update.message().document() != null) {
                        file_id = update.message().document().fileId();
                        file_name = update.message().document().fileName();
                    } else if (update.message().photo() != null) {
                        PhotoSize[] photoSizes = update.message().photo();
                        file_id = photoSizes[photoSizes.length -1].fileId();
                        file_name = photoSizes[photoSizes.length -1].fileUniqueId() + ".jpg";
                    }
                    if (askableServiceObjects.isChatIdForResponse(chatId)) {
                        askableServiceObjects.addResponse(chatId, file_id + "::" + file_name);
                    } else {
                        botService.sendInfo("???????????????????????? ???????? ?????? ???????? ???? ?????????? ???????? ???????????????????? " +
                                "(???????????????????? ?????????? ?????????????? ???????????? ???? ?????????????? ??????????????. \n???????????????? ?????????????? ???? 'Menu'",
                                ProbationDataType.TEXT, chatId);
                    }

                } else {
                    String message = update.message().text();

                    if (!message.isEmpty()) {
                        if (isCommand(message)) {
                            new ShetlerThread(chatId, askableServiceObjects, () -> userService.processCommand(message, chatId)).start();
                        } else {
                            boolean isConcurrentQuery = false;
                            if (message.equals("/+") || message.equalsIgnoreCase("/y") || message.equalsIgnoreCase("/????")) {
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
                                    botService.sendInfo("?????????????????????? ??????????????. ???????????????? ?????????????? ???? 'Menu'", ProbationDataType.TEXT, chatId);
                                }
                            }
                        }
                    }
                }
            } else if (update.callbackQuery() != null) {
                long chatId = update.callbackQuery().message().chat().id();
                int messageId = update.callbackQuery().message().messageId();
                String buttonId = update.callbackQuery().data();

                String text = "The choice is made";
                EditMessageText message = new EditMessageText(chatId, messageId, text);

                SendResponse response = (SendResponse) telegramBot.execute(message);
                if (!response.isOk()) {
                    logger.error("Error: message (EditMessageText) is not sent into chat <{}>", chatId);
                }

                if (askableServiceObjects.getResponse(chatId) == null) {
                    botService.sendInfo("???????????????? ?????????????? ???? ???????????????? ????????", ProbationDataType.TEXT, chatId);
                    return;
                }
                if (askableServiceObjects.getResponse(chatId).isEmpty()) {
                    askableServiceObjects.addResponse(chatId, buttonId);
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private boolean isCommand(String command) {
        if (mainMenuCommands.containsKey(command)) return true;
        if (volunteerCommands.containsKey(command)) return true;
        if (administratorCommands.containsKey(command)) return true;
        return false;
    }

}
