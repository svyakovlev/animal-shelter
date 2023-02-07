package com.teamwork.animalshelter.configuration;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.DeleteMyCommands;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.parser.ParserXML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfiguration {

    @Value("${telegram.bot.token}")
    private String token;

    @Bean
    public TelegramBot telegramBot() {
        TelegramBot bot = new TelegramBot(token);
        bot.execute(new DeleteMyCommands());
        return bot;
    }

    @Bean
    public AskableServiceObjects getAskableServiceObjects() {
        AskableServiceObjects askableServiceObjects = new AskableServiceObjects();
        return askableServiceObjects;
    }

    @Bean
    public ParserXML getParserXML() {
        ParserXML parserXML = new ParserXML();
        return parserXML;
    }


}
