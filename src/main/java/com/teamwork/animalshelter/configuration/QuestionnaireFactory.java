package com.teamwork.animalshelter.configuration;

import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.action.Questionnaire;
import com.teamwork.animalshelter.exception.ErrorQuestionnaire;
import com.teamwork.animalshelter.exception.NotFoundResource;
import com.teamwork.animalshelter.parser.ParserXML;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;

@Component
@PropertySource("classpath:application.properties")
public class QuestionnaireFactory {
    private final AnimalShetlerProperties properties;
    private final ParserXML parserXML;
    private final AskableServiceObjects askableServiceObjects;

    public QuestionnaireFactory(AnimalShetlerProperties properties, ParserXML parserXML, AskableServiceObjects askableServiceObjects) {
        this.properties = properties;
        this.parserXML = parserXML;
        this.askableServiceObjects = askableServiceObjects;

        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(properties.getNameQuestionnaireFolder());
        if (resource == null) throw new NotFoundResource(properties.getNameQuestionnaireFolder());
        File dir = new File(resource.getFile());
        runQuestionnaireFactory(dir);
    }

    private void runQuestionnaireFactory(File dir) {
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
