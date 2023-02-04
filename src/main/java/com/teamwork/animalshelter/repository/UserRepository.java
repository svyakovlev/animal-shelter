package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findUserByChatId(long chatId);

    List<User> findUsersByAdministratorIsTrueAndVolunteerActiveIsTrue();

}
