package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.Probation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProbationRepository extends JpaRepository<Probation, Integer> {
    @Query(value = "select * from probation where client_id = :client and pet_id = :pet", nativeQuery = true)
    Optional<Probation> getProbationByClientIdAndPetId(@Param("client") int clientId, @Param("pet") int petId);

    @Query(value = "select * from probation where (current_timestamp between probation.date_begin and probation.date_finish) " +
            "and probation.message != ''", nativeQuery = true)
    List<Probation> getActiveProbationsWithMessages();

    @Query(value = "" +
            "select distinct * from probation as p " +
            "left join probation_journal as pj " +
                "on p.id = pj.probation_id " +
                    "and pj.date > (current_date - interval '2 day') " +
            "where pj.date is null",
            nativeQuery = true)
    List<Probation> getProbationsOnReportProblem();

    List<Probation> findProbationByDateFinishBeforeAndAndSuccessIsFalseAndResultEquals(LocalDateTime currentDate, String result);}
