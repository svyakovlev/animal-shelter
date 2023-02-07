package com.teamwork.animalshelter.concurrent;

import com.teamwork.animalshelter.action.AskableServiceObjects;

/**
 * Класс обслуживает создание пользовательских потоков.
 */
public class ShetlerThread extends Thread {
    public ShetlerThread(long chatId, AskableServiceObjects askableServiceObjects, Runnable target) {
        super(target);
        askableServiceObjects.updateUserThread(chatId, this);
        askableServiceObjects.resetServiceObjects(chatId);
    }
}
