package com.teamwork.animalshelter.repository;
import com.teamwork.animalshelter.model.Pet;
import com.teamwork.animalshelter.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet,Integer> {
    @Override
    Optional<Pet> findById(Integer integer);

    Page<Pet> findAllByLookingForOwnerIsTrue(PageRequest pageRequest);

    @Query (value = "select count(*) from pet where pet.looking_for_owner = true", nativeQuery = true)
    Optional<Integer> getTotalPetsWhoLookingForOwner();
}
