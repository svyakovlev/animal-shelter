package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.Column;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findUserByChatId(Long chatId);

    List<User> findUsersByAdministratorIsTrueAndVolunteerActiveIsTrue();
}
