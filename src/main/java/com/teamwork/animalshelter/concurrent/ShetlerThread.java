package com.teamwork.animalshelter.concurrent;

import com.teamwork.animalshelter.action.AskableServiceObjects;

/**
 * Класс обслуживает создание пользовательских потоков.
 */
public class ShetlerThread extends Thread {
    private long chatId;
    private AskableServiceObjects askableServiceObjects;
    public ShetlerThread(long chatId, AskableServiceObjects askableServiceObjects, Runnable target) {
        super(target);
        this.chatId = chatId;
        askableServiceObjects.updateUserThread(chatId, this);
        askableServiceObjects.removeResponse(chatId);
        askableServiceObjects.resetConcurrentQuery(chatId);
    }

    public void abort() {
        askableServiceObjects.removeResponse(chatId);
        askableServiceObjects.resetConcurrentQuery(chatId);
        this.interrupt();
    }

}
