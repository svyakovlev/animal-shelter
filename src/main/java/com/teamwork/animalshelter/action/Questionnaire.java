package com.teamwork.animalshelter.action;

import com.teamwork.animalshelter.exception.ErrorQuestionnaire;
import com.teamwork.animalshelter.exception.NotFoundAttributeXmlFile;
import com.teamwork.animalshelter.parser.Element;
import com.teamwork.animalshelter.parser.ParserXML;

import java.io.File;
import java.util.*;

/**
 * Класс определяет работу опросника (процедуры, характеризующейся отправкой вопроса пользователю,
 * ожиданием ответа на этот вопрос и обработкой этого ответа).
 * <br>
 * <ul>
 * Данные класса:
 * <br>
 * <li>{@code questions} - коллекция вопросов. Вопрос описывается классом {@link Node};</li>
 * <li>{@code answers} - содержит ответы пользователя. Ключом выступает метка вопроса {@code label} из класса {@code Node};</li>
 * <li>{@code checks} - содержит регулярные выражения для проверки ответов пользователя. Ключом выступает метка вопроса {@code label} из класса {@code Node};</li>
 * <li>{@code hints} - содержит подсказки для правильного ответа на вопрос. Ключом выступает метка вопроса {@code label} из класса {@code Node};</li>
 * <li>{@code listIterator} - текущий итератор (для перемещения по списку вопросов);</li>
 * <li>{@code currentQuestion} - текущий вопрос;</li>
 * <li>{@code name} - название опросника;</li>
 * <li>{@code error} - ошибка, возникшая при проверке последнего ответа пользователя;</li>
 * <li>{@code interval} - задаваемый интервал ожидания (в минутах) ответа от пользователя. При превышении этого интервала происходит сброс команды;</li>
 * <li>{@code waitingResponse} - {@code true}, если режим ожидания включен;</li>
 * </ul>
 * @see Askable
 * @see ParserXML
 * @see Node
 */
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

    /**
     * Класс определяет структуру вопроса
     * <br>
     * <ul>
     * Данные класса:
     * <br>
     * <li>{@code label} - метка вопроса;</li>
     * <li>{@code question} - описание вопроса;</li>
     * </ul>
     * @see Questionnaire
     */
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

        public Node clone() {
            Node node = new Node(this.getLabel(), this.getQuestion());
            return node;
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

    private Node addNode(String label, String question) {
        Node node = new Node(label, question);
        return node;
    }

    /**
     * Создает новый объект {@code Questionnaire} из файла XML
     * @param file файловый объект
     * @return {@code Questionnaire} возвращает новый объект
     * @see ParserXML
     */
    public static Questionnaire load(ParserXML parserXML, File file) {
        Element root = parserXML.parse(file);

        if (!root.getName().equals("Questionnaire")) {
            throw new ErrorQuestionnaire("", String.format("'%s' - неверное имя элемента", root.getName()));
        }

        String intervalString = root.getAttributes().get("interval");
        if (intervalString == null) {
            throw new NotFoundAttributeXmlFile(root.getName(), "interval");
        }

        String name = root.getAttributes().get("name");
        if (name == null) {
            throw new NotFoundAttributeXmlFile(root.getName(), "name");
        }

        Questionnaire questionnaire;
        try {
            questionnaire = new Questionnaire(name, Integer.valueOf(intervalString));
        } catch (NumberFormatException e) {
            throw new ErrorQuestionnaire(name, "Ошибка в указании интервала");
        } catch (Exception e) {
            throw new ErrorQuestionnaire(name, "Ошибка при создании опросника. " + e.getMessage());
        }

        List<Element> childs = root.getChilds();
        String label;

        for (int i=0; i < childs.size(); i++) {
            Element elementQuestion = childs.get(i);
            if (!elementQuestion.getName().equals("Question")) {
                throw new ErrorQuestionnaire(name, String.format("'%s' - Неверное имя элемента. Ожидается элемент 'Question'", elementQuestion.getName()));
            }
            label = elementQuestion.getAttributes().get("label");
            if (label == null) {
                throw new ErrorQuestionnaire(name, String.format("Не найден атрибут 'label'", elementQuestion.getName()));
            }

            List<Element> questionSections = elementQuestion.getChilds();
            boolean contentExists = false;
            for (int j=0; j < questionSections.size(); j++) {
                Element section = questionSections.get(j);
                switch (section.getName()) {
                    case "Content":
                        contentExists = true;
                        Node node = questionnaire.addNode(label, section.getText());
                        questionnaire.addQuestion(node);
                        break;
                    case "Check":
                        questionnaire.addCheck(label, section.getText());
                        break;
                    case "Hint":
                        questionnaire.addHint(label, section.getText());
                        break;
                    default:
                        throw new ErrorQuestionnaire(name, String.format("'%s' - Неверное имя элемента.", section.getName()));
                }
            }
            if (!contentExists) {
                throw new ErrorQuestionnaire(name, String.format("Элемент 'Content' не был найден для метки '%s'", label));
            }
        }
        return questionnaire;
    }

    /**
     * Создает полную копию текущего объекта {@code Questionnaire}
     * @return {@code Questionnaire} возвращает новый объект
     */
    public Questionnaire dublicate() {
        Questionnaire questionnaire = new Questionnaire(this.getName(), this.getInterval());
        questionnaire.checks.putAll(this.checks);
        questionnaire.hints.putAll(this.hints);
        if (this.questions.isEmpty()) {
            throw new ErrorQuestionnaire(this.getName(), String.format("В опроснике не найдено вопросов"));
        }
        ListIterator<Questionnaire.Node> iterator = this.questions.listIterator(0);
        while (iterator.hasNext()) {
            Node node = iterator.next();
            questionnaire.questions.add(node.clone());
        }
        return questionnaire;
    }

    private void addQuestion(Node node) {
        questions.add(node);
    }

    private void addCheck(String key, String value) {
        checks.put(key, value);
    }

    private void addHint(String key, String value) {
        hints.put(key, value);
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
        setWaitingResponse(false);
        listIterator = questions.listIterator(0);
        currentQuestion = listIterator.next();

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            entry.setValue("");
        }
    }

    @Override
    public boolean empty() {
        String currentLabel = currentQuestion.getLabel();
        if (!answers.containsKey(currentLabel) || answers.get(currentLabel).isBlank()) return false;
        if (listIterator.hasNext()) return false;
        return true;
    }

    @Override
    public Object nextAction() {
        if (empty()) return null;
        return currentQuestion.getQuestion();
    }

    @Override
    public void setResponse(String response) {
        String currentLabel = currentQuestion.getLabel();
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
            err.append("\nПодсказка: ");
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
