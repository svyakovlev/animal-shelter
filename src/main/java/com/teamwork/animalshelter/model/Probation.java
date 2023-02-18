package com.teamwork.animalshelter.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * Этот класс хранит в себе информацию об испытательном сроке усыновителя:
 *
 * @user_id - номер усыновителя;
 * @pet_id - номер усыновляемого питомца;
 * @date_begin - дата начала испытательного срока;
 * @date_finish - дата окончания испытательного срока;
 * @success - статус успешного завершения испытательного срока;
 * @text - комментарий об испытательном сроке.
 */

@Entity
@Table(name = "probation")
public class Probation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "date_begin")
    private LocalDateTime dateBegin;

    @Column(name = "date_finish")
    private LocalDateTime dateFinish;

    private boolean success;

    private String result;
    private String message;

    @ManyToOne()
    @JoinColumn(name = "client_id")
    private User user;

    @OneToOne()
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "probation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProbationJournal> probationJournalRecords;

    public Probation() {}

    public Probation(LocalDateTime dateBegin, LocalDateTime dateFinish, boolean success, User user, Pet pet) {
        this.dateBegin = dateBegin;
        this.dateFinish = dateFinish;
        this.success = success;
        this.user = user;
        this.pet = pet;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDateBegin() {
        return dateBegin;
    }

    public void setDateBegin(LocalDateTime dateBegin) {
        this.dateBegin = dateBegin;
    }

    public LocalDateTime getDateFinish() {
        return dateFinish;
    }

    public void setDateFinish(LocalDateTime dateFinish) {
        this.dateFinish = dateFinish;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Pet getPet() {
        return pet;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPet(Pet pet) {
        this.pet = pet;
    }

    public Set<ProbationJournal> getProbationJournalRecords() {
        return probationJournalRecords;
    }

    public void setProbationJournalRecords(Set<ProbationJournal> probationJournal) {
        this.probationJournalRecords = probationJournal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Probation probation = (Probation) o;
        return id == probation.id && success == probation.success && Objects.equals(dateBegin, probation.getDateBegin()) && Objects.equals(dateFinish, probation.getDateFinish()) && Objects.equals(result, probation.getResult()) && Objects.equals(user, probation.getUser()) && Objects.equals(pet, probation.getPet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dateBegin, dateFinish, success, result);
    }

    @Override
    public String toString() {
        return "Probation{" +
                "id=" + id +
                ", dateBegin='" + dateBegin + '\'' +
                ", dateFinish='" + dateFinish + '\'' +
                ", success=" + success +
                ", result='" + result + '\'' +
                ", userId=" + user.getId() +
                ", petId=" + pet.getId() +
                '}';
    }
}
