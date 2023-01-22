package com.teamwork.animalshelter.model;

import javax.persistence.*;
import java.util.Objects;

/**
 * класс содержит сылки на фотографии к каждому питомцу отдельно!
 *
 * @petId - номер питомца
 * @photo - ссылка на фотографию
 */
@Entity
@Table(name = "photo_pets")
public class PhotoPets {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "pet_id")
    private Integer petId;
    private String photo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    public Pet getPet() {
        return pet;
    }

    public void setPet(Pet pet) {
        this.pet = pet;
    }


    public PhotoPets(Integer id, Integer petId, String photo) {
        this.id = id;
        this.petId = petId;
        this.photo = photo;

    }

    public PhotoPets() {

    }

    public Integer getId() {
        return id;
    }


    public Integer getPetId() {
        return petId;
    }

    public void setPetId(Integer petId) {
        this.petId = petId;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhotoPets photoPets)) return false;
        return Objects.equals(getId(), photoPets.getId()) && Objects.equals(getPetId(), photoPets.getPetId()) && Objects.equals(getPhoto(), photoPets.getPhoto());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getPetId(), getPhoto());
    }

    @Override
    public String toString() {
        return "PhotoPets{" +
                "id=" + id +
                ", petId=" + petId +
                ", photo='" + photo + '\'' +
                '}';
    }


}
