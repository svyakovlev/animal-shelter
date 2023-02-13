package com.teamwork.animalshelter.repository;
import com.teamwork.animalshelter.model.Pet;
import com.teamwork.animalshelter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet,Integer> {
    @Override
    Optional<Pet> findById(Integer integer);
}
