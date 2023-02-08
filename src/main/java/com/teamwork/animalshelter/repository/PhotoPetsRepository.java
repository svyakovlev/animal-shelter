package com.teamwork.animalshelter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoPetsRepository extends JpaRepository<PhotoPetsRepository,Integer> {
}
