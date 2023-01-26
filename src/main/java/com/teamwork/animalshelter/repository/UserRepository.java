package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.Column;
import java.time.LocalDateTime;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    @Query(value = "SELECT lastVisit FROM client WHERE chat_id=:chat_user ",nativeQuery = true)
 LocalDateTime lastVisit(@Param("chat_user") long chatId);
}
