package model;


import java.util.Set;
import javax.persistence.*;
import java.util.Objects;
@Entity
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nickname;
    private String breed;
    private Integer age;
    private String character;
    private Boolean lookingForOwner;


    @OneToMany(mappedBy = "pet")
    private Set<PhotoPets> photoPets;

    public Pet() {
    }

    public Pet(Long id, String nickname, String breed, Integer age, String character, Boolean lookingForOwner, PhotoPets photoPets) {
        this.id = id;
        this.nickname = nickname;
        this.breed = breed;
        this.age = age;
        this.character = character;
        this.lookingForOwner = lookingForOwner;
    }

    public Long getId() {
        return id;
    }


    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public Boolean getLookingForOwner() {
        return lookingForOwner;
    }

    public void setLookingForOwner(Boolean lookingForOwner) {
        this.lookingForOwner = lookingForOwner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pet pet)) return false;
        return Objects.equals(getId(), pet.getId()) && Objects.equals(getNickname(), pet.getNickname()) && Objects.equals(getBreed(), pet.getBreed()) && Objects.equals(getAge(), pet.getAge()) && Objects.equals(getCharacter(), pet.getCharacter()) && Objects.equals(getLookingForOwner(), pet.getLookingForOwner());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getNickname(), getBreed(), getAge(), getCharacter(), getLookingForOwner());
    }

    @Override
    public String toString() {
        return "Pet{" +
                "id=" + id +
                ", nickname='" + nickname + '\'' +
                ", breed='" + breed + '\'' +
                ", age=" + age +
                ", character='" + character + '\'' +
                ", lookingForOwner=" + lookingForOwner +
                '}';
    }




}
