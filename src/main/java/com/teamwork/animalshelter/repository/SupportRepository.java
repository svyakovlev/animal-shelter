package com.teamwork.animalshelter.repository;


import com.teamwork.animalshelter.model.Support;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportRepository extends JpaRepository<Support, Integer> {

}
