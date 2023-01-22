package com.teamwork.animalshelter.repository;
import com.teamwork.animalshelter.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet,Long> {
}
