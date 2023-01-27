package com.teamwork.animalshelter.action;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Questionnaire implements Askable{
    private List<Node> questions;
    private Map<String, String> answers;
    private Map<String, String> checks;
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
    }

    public static Questionnaire load(String filePath) {

    }

    public Questionnaire dublicate() {

    }


}
