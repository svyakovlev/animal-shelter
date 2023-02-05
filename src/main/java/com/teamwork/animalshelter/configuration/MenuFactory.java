package com.teamwork.animalshelter.configuration;

import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.action.Menu;
import com.teamwork.animalshelter.exception.ErrorMenu;
import com.teamwork.animalshelter.exception.NotFoundResource;
import com.teamwork.animalshelter.parser.ParserXML;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;

@Component
@PropertySource("classpath:application.properties")
public class MenuFactory {
    private AnimalShetlerProperties properties;
    private final ParserXML parserXML;
    private final AskableServiceObjects askableServiceObjects;

    public MenuFactory(AnimalShetlerProperties properties, ParserXML parserXML, AskableServiceObjects askableServiceObjects) {
        this.properties = properties;
        this.parserXML = parserXML;
        this.askableServiceObjects = askableServiceObjects;

        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(properties.getNameMenuFolder());
        if (resource == null) throw new NotFoundResource(properties.getNameMenuFolder());
        File dir = new File(resource.getFile());
        runMenuFactory(dir);
    }

    void runMenuFactory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                addMenu(file);
            }
        }
    }

    private void addMenu(File file) {
        Menu menu = Menu.load(parserXML, file);
        if (menu == null) {
            throw new ErrorMenu(file.getName(), "Ошибка создания меню");
        }
        askableServiceObjects.addTemplate(menu.getName(), menu);
    }
}
