package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.Probation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProbationRepository extends JpaRepository<Probation, Integer> {
}
