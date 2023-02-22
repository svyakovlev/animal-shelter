package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.Contact;
import com.teamwork.animalshelter.model.ContactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Integer> {

    Optional<Contact> findContactByValueEqualsAndTypeEquals(String phoneNumber, ContactType contactType);

}
