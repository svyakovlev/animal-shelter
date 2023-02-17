package com.teamwork.animalshelter.configuration;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.DeleteMyCommands;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.parser.ParserXML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class TelegramBotConfiguration {

    @Value("${telegram.bot.token}")
    private String token;

    @Bean
    public TelegramBot telegramBot() {
        TelegramBot bot = new TelegramBot(token);
        bot.execute(new DeleteMyCommands());

        Map<String, String> menuCommands = getMainMenuCommands();
        List<BotCommand> commands = new ArrayList<>();
        for (Map.Entry<String, String> entry : menuCommands.entrySet()) {
            commands.add(new BotCommand(entry.getKey(), entry.getValue()));
        }
        BotCommand[] botCommands = commands.toArray(new BotCommand[commands.size()]);
        bot.execute(new SetMyCommands(botCommands));
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
        commands.put("/get_user", "Получить информацию о клиенте по id");
        commands.put("/find_user", "Получить информацию о клиенте по телефону");
        commands.put("/get_user_probation", "Найти записи об испытательном сроке по id");
        commands.put("/message", "Отправить предупреждение пользователю");
        commands.put("/transfer", "Назначить испытательный срок");
        commands.put("/prolongation", "Продлить испытательный срок");
        commands.put("/finish_probation", "Закончить испытательный срок");
        return commands;
    }

    @Bean(name = "administratorCommands")
    public Map<String, String> getAdministratorCommands() {
        Map<String, String> commands = new LinkedHashMap<>();
        commands.put("/set_volunteer", "Сделать пользователя волонтером");
        commands.put("/reset_volunteer", "Убрать пользователя из группы волонтеров");
        commands.put("/add_pet", "Добавить питомца");
        commands.put("/add_photo_pet", "Добавить фотографию питомца");
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
        commands.put("/show_chat_id", "-");
        return commands;
    }


}
