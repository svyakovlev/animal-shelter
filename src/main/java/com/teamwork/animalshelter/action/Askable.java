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
    /**
     * Функция делает первичную подготовку объекта для использования.
     * Каждая реализация имеет свой тип инициализации в зависимости от
     * внутренней структуры данных.
     */
    void init();

    /**
     * @return Возвращает {@code true}, когда по текущему вопросу получен ответ пользователя и больше нет новых вопросов.
     */
    boolean empty();

    /**
     * @return Возвращается строковое представление очередного вопроса пользователю.
     * Если ответ пользователя был с ошибкой, то возвращается повторное строковое представление этого же вопроса.
     * Переход к новому вопросу осуществляется в функции {@link #setResponse(String)} после фиксации ответа пользователю.
     */
    String nextAction();

    /**
     * Функция фиксирует ответ пользователя и делает переход к новому вопросу (если он существует).
     * @param response ответ пользователя
     */
    void setResponse(String response);

    /**
     * Функция проверяет ответ пользователя на ошибки. Проверка ответа зависит от реализации интерфейса.
     * @param response ответ пользователя.
     * @return результат проверки ответа пользователя.
     */
    boolean checkResponse(String response);
    Map<String, String> getResult();
    boolean verificationRequired();
    void setWaitingResponse(boolean waitingResponse);
    boolean isWaitingResponse();
    String getLastError();
    boolean intervalExceeded(int minutes);

    /**
     * Название созданного объекта {@code Askable} используется в кэше шаблонов и в кэше объектов для
     * быстрого доступа при очередном обращении пользователя.
     * @return возвращает название созданного объекта.
     * @see AskableServiceObjects
     */
    String getName();
}
