package com.teamwork.animalshelter.configuration;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.DeleteMyCommands;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.parser.ParserXML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

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

    @Bean(name = "volunteerCommands")
    public Map<String, String> getVolunteerCommands() {
        Map<String, String> commands = new LinkedHashMap<>();
        commands.put("/state", "Текущее состояние активности");
        commands.put("/active", "Перевести в активное состояние");
        commands.put("/busy", "Перевести в занятое состояние");
        commands.put("/get-client", "Получить информацию о клиенте по id");
        commands.put("/find-client", "Получить информацию о клиенте по телефону");
        commands.put("/get-client-probation", "Найти записи об испытательном сроке по id");
        commands.put("/message", "Отправить предупреждение пользователю");
        commands.put("/transfer", "Назначить испытательный срок");
        commands.put("/prolongation", "Продлить испытательный срок");
        commands.put("/finish-probation", "Закончить испытательный срок");
        return commands;
    }

    @Bean(name = "administratorCommands")
    public Map<String, String> getAdministratorCommands() {
        Map<String, String> commands = new LinkedHashMap<>();
        commands.put("/write-chat-id", "Инициализация администратора по номеру телефона");
        commands.put("/set-volunteer", "Сделать пользователя волонтером");
        commands.put("/reset-volunteer", "Убрать пользователя из группы волонтеров");
        commands.put("/add-pet", "Добавить питомца");
        commands.put("/add-photo-pet", "Добавить фотографию питомца");
        return commands;
    }

    @Bean(name = "mainMenuCommands")
    public Map<String, String> getMainMenuCommands() {
        Map<String, String> commands = new LinkedHashMap<>();
        commands.put("/info", "Общая информация о приюте");
        commands.put("/consultation", "Как взять питомца из приюта");
        commands.put("/pet", "Выбор питомца");
        commands.put("/keeping", "Прохождение испытательного срока");
        commands.put("/call", "Заказать обратный звонок");
        commands.put("/chat", "Позвать волонтера в чат");
        commands.put("/volunteer", "Хочу стать волонтером");
        commands.put("/show", "Для сотрудников");
        return commands;
    }


}
