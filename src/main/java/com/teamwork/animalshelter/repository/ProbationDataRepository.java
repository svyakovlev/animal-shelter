package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.ProbationData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProbationDataRepository extends JpaRepository<ProbationData, Integer> {
}
