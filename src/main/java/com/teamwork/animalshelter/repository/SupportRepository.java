package com.teamwork.animalshelter.repository;


import com.teamwork.animalshelter.model.Support;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SupportRepository extends JpaRepository<Support, Integer> {
    List<Support> findAllByFinishIsFalseAndBeginDateTimeAfter(LocalDateTime currentDateTime);
}
