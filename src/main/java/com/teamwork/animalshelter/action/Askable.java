package com.teamwork.animalshelter.action;

import java.util.Map;

/**
 * Интерфейс Askable предназначен для обеспечения диалога с клиентом
 * посредством подготовки вопросов перед отправкой и анализом полученных ответов от пользователя
 *
 * @author  Artemiev Stanislav
 * @see  Questionnaire
 * @see  Menu
 */
public interface Askable {
    void init();
    boolean empty();
    String nextAction();
    void setResponse(String response);
    boolean checkResponse(String response);
    Map<String, String> getResult();
    boolean verificationRequired();
    void setWaitingResponse(boolean waitingResponse);
    boolean getWaitingResponse();
    String getLastError();
    boolean intervalExceeded(int minutes);
    String getName();
}
