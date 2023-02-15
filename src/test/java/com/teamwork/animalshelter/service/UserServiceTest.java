package com.teamwork.animalshelter.service;

import com.teamwork.animalshelter.model.ProbationDataType;
import com.teamwork.animalshelter.model.User;
import com.teamwork.animalshelter.repository.UserRepository;
import net.bytebuddy.asm.Advice;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class UserServiceTest {
    private UserRepository userRepository;

    @MockBean
    private BotService botService;

    @InjectMocks
    private UserService userService;

    public UserServiceTest(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //@ParameterizedTest()
    @Test
    public void sendGreeting() {
        User user = new User("Bob", false, false, false, 10, LocalDateTime.now().minusDays(2), "");
        user.setId(1);
        userRepository.save(user);

        System.out.println(user);

        LocalDateTime expected = LocalDateTime.now();
        doNothing().when(botService).sendInfo(any(Object.class), any(ProbationDataType.class), anyLong());
        userService.sendGreeting(10, expected);

        user = userRepository.findUserById(1).get();
        Assertions.assertThat(user).isNotNull();
        Assertions.assertThat(user.getLastVisit()).isEqualTo(expected);

    }
}
