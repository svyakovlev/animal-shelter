package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.Probation;
import com.teamwork.animalshelter.model.ProbationJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProbationJournalRepository extends JpaRepository<ProbationJournal, Integer> {

    @Query(value = "select * from probation_journal pj " +
            "where (pj.date >= current_date) " +
            "and (pj.date <= (current_timestamp - interval '1 hour'))", nativeQuery = true)
    List<ProbationJournal> getJournalRecordsOnIncompleteReport();
    Optional<ProbationJournal> findProbationJournalByProbationEqualsAndDateAfterAndDateBefore(Probation probation, LocalDateTime beginDay, LocalDateTime endDay);
    List<ProbationJournal> findAllByProbationEqualsOrderByDateDesc(Probation probation);
}
