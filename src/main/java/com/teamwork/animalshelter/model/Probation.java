package com.teamwork.animalshelter.model;

import model.Pet;

import javax.persistence.*;
import java.util.Objects;

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

    @Column(name = "user_id")
    private int userId;

    @Column(name = "pet_id")
    private int petId;

    @Column(name = "date_begin")
    private String dateBegin;

    @Column(name = "date_finish")
    private String dateFinish;

    private boolean success;

    private String text;

    @ManyToOne()
    @JoinColumn(name = "client_id")
    private User user;

    @OneToOne()
    @JoinColumn(name = "pet_id")
    private Pet pet;

    public Probation(int id, int userId, int petId, String dateBegin, String dateFinish, boolean success, String text, User user, Pet pet) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.dateBegin = dateBegin;
        this.dateFinish = dateFinish;
        this.success = success;
        this.text = text;
        this.user = user;
        this.pet = pet;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPetId() {
        return petId;
    }

    public void setPetId(int petId) {
        this.petId = petId;
    }

    public String getDateBegin() {
        return dateBegin;
    }

    public void setDateBegin(String dateBegin) {
        this.dateBegin = dateBegin;
    }

    public String getDateFinish() {
        return dateFinish;
    }

    public void setDateFinish(String dateFinish) {
        this.dateFinish = dateFinish;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public void setPet(Pet pet) {
        this.pet = pet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Probation probation = (Probation) o;
        return id == probation.id && userId == probation.userId && petId == probation.petId && success == probation.success && Objects.equals(dateBegin, probation.dateBegin) && Objects.equals(dateFinish, probation.dateFinish) && Objects.equals(text, probation.text) && Objects.equals(user, probation.user) && Objects.equals(pet, probation.pet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, petId, dateBegin, dateFinish, success, text, user, pet);
    }

    @Override
    public String toString() {
        return "Probation{" +
                "id=" + id +
                ", userId=" + userId +
                ", petId=" + petId +
                ", dateBegin='" + dateBegin + '\'' +
                ", dateFinish='" + dateFinish + '\'' +
                ", success=" + success +
                ", text='" + text + '\'' +
                ", user=" + user +
                ", pet=" + pet +
                '}';
    }
}
