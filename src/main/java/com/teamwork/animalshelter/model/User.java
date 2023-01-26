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
    private long chat_id;

    @Column(name = "last_visit")
    private LocalDateTime lastVisit;

    @Column(name = "time_zone")
    private String timeZone;

    @OneToMany(mappedBy = "client")
    private Set<Contact> contacts;

    @OneToMany(mappedBy = "client")
    private Set<Support> supportRecordsByUser;

    @OneToMany(mappedBy = "client")
    private Set<Support> supportRecordsByVolunteer;

    public User() {}

    public User(String name, boolean volunteer, boolean volunteerActive, boolean administrator, long chat_id, LocalDateTime lastVisit, String timeZone) {
        this.name = name;
        this.volunteer = volunteer;
        this.volunteerActive = volunteerActive;
        this.administrator = administrator;
        this.chat_id = chat_id;
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

    public long getChat_id() {
        return chat_id;
    }

    public void setChat_id(long chat_id) {
        this.chat_id = chat_id;
    }

    public LocalDateTime getLastVisit(Integer id) {return lastVisit;
    }

    public void setLastVisit(LocalDateTime lastVisit) {
        this.lastVisit = lastVisit;
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
                ", chat_id=" + chat_id +
                ", lastVisit=" + lastVisit +
                ", timeZone='" + timeZone + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return chat_id == user.chat_id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chat_id);
    }
}
