package com.teamwork.animalshelter;

import com.pengrad.telegrambot.TelegramBot;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.configuration.AnimalShetlerProperties;
import com.teamwork.animalshelter.model.ProbationDataType;
import com.teamwork.animalshelter.model.User;
import com.teamwork.animalshelter.repository.*;
import com.teamwork.animalshelter.service.AnimalShetlerInfoService;
import com.teamwork.animalshelter.service.BotService;
import com.teamwork.animalshelter.service.UserService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserServiceTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AskableServiceObjects askableServiceObjects;

    private UserService userService;

    @MockBean
    private BotService botService;

    @MockBean
    private TelegramBot telegramBot;

    @MockBean
    private ContactRepository contactRepository;

    @MockBean
    private ProbationRepository probationRepository;

    @MockBean
    private SupportRepository supportRepository;

    @MockBean
    private AnimalShetlerInfoService animalShetlerInfoService;

    @Autowired
    private AnimalShetlerProperties animalShetlerProperties;

    @MockBean
    private ProbationJournalRepository probationJournalRepository;

    @MockBean(name = "volunteerCommands")
    private Map<String, String> volunteerCommands;

    @MockBean(name = "administratorCommands")
    private Map<String, String> administratorCommands;


    @MockBean
    private PetRepository petRepository;

    @BeforeAll
    public void init() {
        userService = new UserService(botService,
                askableServiceObjects,
                userRepository,
                contactRepository,
                probationRepository,
                supportRepository,
                animalShetlerInfoService,
                animalShetlerProperties,
                probationJournalRepository,
                petRepository,
                telegramBot,
                volunteerCommands,
                administratorCommands);
    }

    @Test
    public void sendGreeting() {
        LocalDateTime expected = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        User user = new User("Bob", false, false, false, 10, expected.minusDays(2), "");
        user.setId(1);
        userRepository.save(user);
        doNothing().when(botService).sendInfo(any(Object.class), any(ProbationDataType.class), anyLong());
        userService.sendGreeting(10, expected);
        user = userRepository.findUserById(1).get();
        Assertions.assertThat(user).isNotNull();
        LocalDateTime actual = user.getLastVisit();
        Assertions.assertThat(actual).isEqualTo(expected);
    }
}
