package com.teamwork.animalshelter.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class AnimalShetlerProperties {
    @Value("${shetler.actions.menu-folder}")
    private String nameMenuFolder;

    @Value("${shetler.actions.questionnaire-folder}")
    private String nameQuestionnaireFolder;

    @Value("${shetler.storage.full-path}")
    private String pathStorage;

    public String getNameMenuFolder() {
        return nameMenuFolder;
    }

    public String getNameQuestionnaireFolder() {
        return nameQuestionnaireFolder;
    }

    public String getPathStorage() {
        return pathStorage;
    }
}
