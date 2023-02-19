package com.teamwork.animalshelter.service;

import com.pengrad.telegrambot.TelegramBot;
import com.teamwork.animalshelter.action.AskableServiceObjects;
import com.teamwork.animalshelter.configuration.AnimalShetlerProperties;
import com.teamwork.animalshelter.model.Contact;
import com.teamwork.animalshelter.model.Pet;
import com.teamwork.animalshelter.model.ProbationDataType;
import com.teamwork.animalshelter.model.User;
import com.teamwork.animalshelter.repository.*;
import com.teamwork.animalshelter.service.AnimalShetlerInfoService;
import com.teamwork.animalshelter.service.BotService;
import com.teamwork.animalshelter.service.UserService;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserServiceTest {
    //Logger logger = LoggerFactory.getLogger(UserServiceTest.class);
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

    @Autowired
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

    @BeforeEach
    public void initTest() {
        userRepository.deleteAllInBatch();
        petRepository.deleteAllInBatch();
    }

    @Test
    public void sendGreeting() {
        LocalDateTime expected = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        User user = new User("Bob", false, false, false, 10, expected.minusDays(2), "");
        //user.setId(1);
        Set<Contact> contacts = new HashSet<>();
        user.setContacts(contacts);
        userRepository.save(user);
        doNothing().when(botService).sendInfo(any(Object.class), any(ProbationDataType.class), anyLong());
        userService.sendGreeting(10, expected);
        user = userRepository.findUserByChatId(10L).orElse(null);
        Assertions.assertThat(user).isNotNull();
        LocalDateTime actual = user.getLastVisit();
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void addPet() throws InterruptedException {
        doNothing().when(botService).sendInfo(any(Object.class), any(ProbationDataType.class), anyLong());

        Map<String, String> response = Map.of("interrupt", "");
        when(botService.startAction(anyString(), anyLong())).thenReturn(response);

        userService.addPet(10L);
        List<Pet> pets = petRepository.findAll();
        Assertions.assertThat(pets.size()).isEqualTo(0);

        Map<String, String> response2 = Map.of("nickname", "Dick",
                                                "breed", "Овчарка",
                                                "character", "Спокойная, тихая",
                                                "birthday", "15/12/2020");

        when(botService.startAction(anyString(), anyLong())).thenReturn(response2);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate dateTime = LocalDate.parse(response2.get("birthday"), formatter);

        Pet expected = new Pet(response2.get("nickname"), response2.get("breed"), dateTime, response2.get("character"), true);
        userService.addPet(20L);
        pets = petRepository.findAll();
        Assertions.assertThat(pets.size()).isEqualTo(1);
        Pet actual = pets.get(0);
        Assertions.assertThat(actual).isEqualTo(expected);
    }
}
