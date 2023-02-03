package com.teamwork.animalshelter.configuration;

import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.action.Questionnaire;
import com.teamwork.animalshelter.exception.ErrorQuestionnaire;
import com.teamwork.animalshelter.parser.ParserXML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class QuestionnaireFactory {
    @Value("${shetler.actions.questionnaire-folder}")
    private String nameQuestionnaireFolder;

    private final ParserXML parserXML;
    private final AskableServiceObjects askableServiceObjects;

    public QuestionnaireFactory(ParserXML parserXML, AskableServiceObjects askableServiceObjects) {
        this.parserXML = parserXML;
        this.askableServiceObjects = askableServiceObjects;
        runQuestionnaireFactory();
    }

    private void runQuestionnaireFactory() {
        File dir = new File(nameQuestionnaireFolder);
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                addQuestionnaire(file);
            }
        }
    }

    private void addQuestionnaire(File file) {
        Questionnaire questionnaire = Questionnaire.load(parserXML, file);
        if (questionnaire == null) {
            throw new ErrorQuestionnaire(file.getName(), "Ошибка создания опросника");
        }
        askableServiceObjects.addTemplate(questionnaire.getName(), questionnaire);
    }
}
