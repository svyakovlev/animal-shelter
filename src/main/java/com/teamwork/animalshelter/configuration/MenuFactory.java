package com.teamwork.animalshelter.configuration;

import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.action.Menu;
import com.teamwork.animalshelter.exception.ErrorMenu;
import com.teamwork.animalshelter.parser.ParserXML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class MenuFactory {
    @Value("${shetler.actions.menu-folder}")
    private String nameMenuFolder;

    private final ParserXML parserXML;
    private final AskableServiceObjects askableServiceObjects;

    public MenuFactory(ParserXML parserXML, AskableServiceObjects askableServiceObjects) {
        this.parserXML = parserXML;
        this.askableServiceObjects = askableServiceObjects;
        runMenuFactory();
    }

    private void runMenuFactory() {
        File dir = new File(nameMenuFolder);
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
