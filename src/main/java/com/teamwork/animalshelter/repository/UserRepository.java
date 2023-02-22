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

    List<User> findUsersByVolunteerIsTrue();

    Optional<User> findFirstByAdministratorIsTrueAndChatIdGreaterThan(long zero);

    @Query(value = "select count(*) from client as c where c.chat_id = :chat and c.volunteer = true", nativeQuery = true)
    Optional<Integer> isVolunteer(@Param("chat") long chatId);

    @Query(value = "select count(*) from client as c where c.chat_id = :chat and c.administrator = true", nativeQuery = true)
    Optional<Integer> isAdministrator(@Param("chat") long chatId);

    @Query(value = "select count(*) from client as c where c.chat_id = :chat " +
            "and (c.administrator = true or c.volunteer = true)", nativeQuery = true)
    Optional<Integer> isEmployee(@Param("chat") long chatId);

    @Query(value = "select count(*) from client as c where c.chat_id = :chat and c.volunteer_active = true", nativeQuery = true)
    Optional<Integer> isActive(@Param("chat") long chatId);
}
