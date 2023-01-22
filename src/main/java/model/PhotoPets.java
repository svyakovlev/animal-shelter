package model;

import javax.persistence.*;
import java.util.Objects;
@Entity
public class PhotoPets {
    @Id
   private Long id;
   private Long petId;
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


    public PhotoPets(Long id, Long petId, String photo) {
        this.id = id;
        this.petId = petId;
        this.photo = photo;

    }

    public PhotoPets() {

    }

    public Long getId() {
        return id;
    }


    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
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
