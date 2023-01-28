package com.teamwork.animalshelter.action;

import java.util.*;

public class Questionnaire implements Askable{
    private List<Node> questions;
    private Map<String, String> answers;
    private Map<String, String> checks;
    private Map<String, String> hints;
    private ListIterator<Node> listIterator;
    private Node currentQuestion;
    private String name;
    private String error;
    private int interval;
    private boolean waitingResponse;
    class Node {
        private String label;
        private String question;

        public Node(String label, String question) {
            this.label = label;
            this.question = question;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }

    private Questionnaire(String name, int interval) {
        this.name = name;
        this.interval = interval;
        this.error = "";
        this.waitingResponse = false;
        this.questions = new LinkedList<>();
        this.answers = new HashMap<>();
        this.checks = new HashMap<>();
        this.hints = new HashMap<>();
    }

    public static Questionnaire load(String filePath) {

    }

    public Questionnaire dublicate() {

    }

    public int getInterval() {
        return interval;
    }

    private String getError() {
        return error;
    }

    private void setError(String error) {
        this.error = error;
    }

    @Override
    public void init() {
        setError("");
        listIterator = questions.listIterator(0);
        currentQuestion = questions.get(0);

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            entry.setValue("");
        }
    }

    @Override
    public boolean empty() {
        String currentLabel = currentQuestion.getLabel();
        if (answers.get(currentLabel).isBlank()) return false;
        if (listIterator.hasNext()) return false;
        return true;
    }

    @Override
    public String nextAction() {
        return currentQuestion.getQuestion();
    }

    @Override
    public void setResponse(String response) {
        String currentLabel = currentQuestion.getLabel();
        if (!answers.containsKey(currentLabel)) {
            // исключение
            // не найден ключ для записи ответа пользователя
        }
        answers.put(currentLabel, response);
        if (listIterator.hasNext()) currentQuestion = listIterator.next();
    }

    @Override
    public boolean checkResponse(String response) {
        String currentLabel = currentQuestion.getLabel();
        if (!checks.containsKey(currentLabel)) return true;
        String checkValue = checks.get(currentLabel);
        if (response.matches(checkValue)) return true;
        StringBuilder err = new StringBuilder("Ответ имеет неверный формат.");
        if (hints.containsKey(currentLabel)) {
            err.append(" Подсказка: ");
            err.append(hints.get(currentLabel));
        }
        setError(err.toString());
        return false;
    }

    @Override
    public Map<String, String> getResult() {
        return Map.copyOf(answers);
    }

    @Override
    public boolean verificationRequired() {
        String currentLabel = currentQuestion.getLabel();
        return checks.containsKey(currentLabel);
    }

    @Override
    public void setWaitingResponse(boolean waitingResponse) {
        this.waitingResponse = waitingResponse;
    }

    @Override
    public boolean isWaitingResponse() {
        return this.waitingResponse;
    }

    @Override
    public String getLastError() {
        return getError();
    }

    @Override
    public boolean intervalExceeded(int minutes) {
        return minutes > getInterval();
    }

    @Override
    public String getName() {
        return this.name;
    }
}
