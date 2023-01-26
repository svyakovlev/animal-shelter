package com.teamwork.animalshelter.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.teamwork.animalshelter.action.Askable;
import com.teamwork.animalshelter.model.ProbationDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BotService {
    private final TelegramBot telegramBot;
    private final Logger logger = LoggerFactory.getLogger(BotService.class);

    /**
     * Вспомогательная структура.
     * Служит для записи ответов пользователей на заданные им вопросы.
     * <ul>
     * <li> key: идентификатор чата пользователя</li>
     * <li> value: полученный ответ пользователя</li></ul>
     */
    private Map<Long, String> waitingResponses;


    public BotService(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }



    private void verifyResponse(SendResponse response, long chat_id) {
        // в этом методе следует вызвать исключение в случае ошибки доставки
        // и обдумать как это отразится на выполнении алгоритма
        if (response.isOk()) {
            logger.info("Message is sent into chat <{}> successfully", chat_id);
        } else {
            logger.error("Error: message is not sent into chat <{}> (command '/start')", chat_id);
        }
    }

    private void sendTextInfo(Object object, long chat_id) {
        SendMessage sendMessage = new SendMessage(chat_id, (String) object);
        SendResponse response = telegramBot.execute(sendMessage);
        verifyResponse(response, chat_id);
    }

    private void sendPhotoInfo(Object object, long chat_id) {

    }

    private void sendDocumentInfo(Object object, long chat_id) {

    }

    public void sendInfo(Object object, ProbationDataType type, long chat_id) {
        switch (type) {
            case TEXT -> sendTextInfo(object, chat_id);
            case DOCUMENT -> sendDocumentInfo(object, chat_id);
            case PHOTO -> sendPhotoInfo(object, chat_id);
        }
    }

    public void doAction(Askable ask,  long chat_id, String s) {
        String action = ask.nextAction();
        if (action == null) return;
        waitingResponses.put(chat_id, "");
        ask.setWaitingResponse(true);
        sendInfo(action, ProbationDataType.TEXT, chat_id);
    }

}
