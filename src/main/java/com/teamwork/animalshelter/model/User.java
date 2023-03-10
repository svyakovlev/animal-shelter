package com.teamwork.animalshelter.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "client")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private boolean volunteer;

    @Column(name = "volunteer_active")
    private boolean volunteerActive;

    private boolean administrator;
    @Column(name = "chat_id")
    private long chatId;

    @Column(name = "last_visit")
    private LocalDateTime lastVisit;

    @Column(name = "time_zone")
    private String timeZone;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<Contact> contacts;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Support> supportRecordsByUser;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "volunteer", cascade = CascadeType.ALL)
    private Set<Support> supportRecordsByVolunteer;

    public User() {}

    public User(String name, boolean volunteer, boolean volunteerActive, boolean administrator, long chatId, LocalDateTime lastVisit, String timeZone) {
        this.name = name;
        this.volunteer = volunteer;
        this.volunteerActive = volunteerActive;
        this.administrator = administrator;
        this.chatId = chatId;
        this.lastVisit = lastVisit;
        this.timeZone = timeZone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVolunteer() {
        return volunteer;
    }

    public void setVolunteer(boolean volunteer) {
        this.volunteer = volunteer;
    }

    public boolean isVolunteerActive() {
        return volunteerActive;
    }

    public void setVolunteerActive(boolean volunteerActive) {
        this.volunteerActive = volunteerActive;
    }

    public boolean isAdministrator() {
        return administrator;
    }

    public void setAdministrator(boolean administrator) {
        this.administrator = administrator;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public LocalDateTime getLastVisit() {return lastVisit;
    }

    public void setLastVisit(LocalDateTime lastVisit) {
        this.lastVisit = lastVisit;
    }

    public Set<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(Set<Contact> contacts) {
        this.contacts = contacts;
    }

    public Set<Support> getSupportRecordsByUser() {
        return supportRecordsByUser;
    }

    public void setSupportRecordsByUser(Set<Support> supportRecordsByUser) {
        this.supportRecordsByUser = supportRecordsByUser;
    }

    public Set<Support> getSupportRecordsByVolunteer() {
        return supportRecordsByVolunteer;
    }

    public void setSupportRecordsByVolunteer(Set<Support> supportRecordsByVolunteer) {
        this.supportRecordsByVolunteer = supportRecordsByVolunteer;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", volunteer=" + volunteer +
                ", volunteerActive=" + volunteerActive +
                ", administrator=" + administrator +
                ", chat_id=" + chatId +
                ", lastVisit=" + lastVisit +
                ", timeZone='" + timeZone + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return chatId == user.chatId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId);
    }
}
