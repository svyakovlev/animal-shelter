package com.teamwork.animalshelter.repository;

import com.teamwork.animalshelter.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Integer> {

    Contact findContactByValueAndType(String phoneNumber, int type);

}
