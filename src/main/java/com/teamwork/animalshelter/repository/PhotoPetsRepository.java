package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.PhotoPets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoPetsRepository extends JpaRepository<PhotoPets,Integer> {
}
