package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.ProbationJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProbationJournalRepository extends JpaRepository<ProbationJournal, Integer> {
}
