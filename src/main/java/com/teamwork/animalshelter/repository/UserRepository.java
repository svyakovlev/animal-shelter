package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.Column;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findUserByChatId(Long chatId);

    Optional<User> findUserById(Integer id);

    List<User> findUsersByAdministratorIsTrueAndVolunteerActiveIsTrue();

    List<User> findUsersByVolunteerActiveIsTrue();
    Optional<User> findFirstByAdministratorIsTrueAndChatIdGreaterThan(long zero);

}
