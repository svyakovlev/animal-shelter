package com.teamwork.animalshelter.model;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import javax.persistence.*;
import java.util.Objects;

/**
 * этот класс хранит в себе все данные об питомцах
 *
 * @nicknameимя - имя питомца
 * @breed - порода питомца
 * @age - возраст(в месяцах)
 * @character - характер
 */
@Entity
@Table(name = "pet")
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String nickname;
    private String breed;

    @Column(name = "birthday")
    private LocalDate birthday;
    private String character;
    @Column(name = "looking_for_owner")
    private Boolean lookingForOwner;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "pet")
    private Set<PhotoPets> photoPets;

    public Pet() {}

    public Pet(String nickname, String breed, LocalDate birthday, String character, Boolean lookingForOwner) {
        this.nickname = nickname;
        this.breed = breed;
        this.birthday = birthday;
        this.character = character;
        this.lookingForOwner = lookingForOwner;
    }

    public Integer getId() {
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

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
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
        return Objects.equals(getId(), pet.getId()) && Objects.equals(getNickname(), pet.getNickname()) && Objects.equals(getBreed(), pet.getBreed()) && Objects.equals(getBirthday(), pet.getBirthday()) && Objects.equals(getCharacter(), pet.getCharacter()) && Objects.equals(getLookingForOwner(), pet.getLookingForOwner());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getNickname(), getBreed(), getBirthday(), getCharacter(), getLookingForOwner());
    }

    /**
     * Возвращает возраст питомца в удобном для восприятия формате, если задан день рожления питомца.
     * @return возраст питомца в формате {@code <x> лет <y> месяцев}. Если день рождения не задан,
     * тогда возвращается строка {@code "неизвестно"}.
     */
    public String getAge() {
        if (birthday == null) return "неизвестно";
        long monthsNumberTotal = ChronoUnit.MONTHS.between(birthday, LocalDate.now());
        if (monthsNumberTotal < 12) return getValueSuffix(monthsNumberTotal, false);
        long yearsNumber = monthsNumberTotal / 12;
        long monthsNumber = monthsNumberTotal - 12 * yearsNumber;
        StringBuilder sb = new StringBuilder(getValueSuffix(yearsNumber, true));
        sb.append(" ");
        sb.append(getValueSuffix(monthsNumber, false));
        return sb.toString();
    }

    private String getValueSuffix(long value, boolean isYear) {
        if(isYear) {
            long number = (value / 10) * 10;
            switch ((int) (value - number)) {
                case 1:
                    return "1 год";
                case 2:
                case 3:
                case 4:
                    return String.format("%d года", value);
                default:
                    return String.format("%d лет", value);
            }
        }
        else {
            if (value == 0) return "";
            switch ((int) value) {
                case 1:
                    return "1 месяц";
                case 2:
                case 3:
                case 4:
                    return String.format("%d месяца", value);
                default:
                    return String.format("%d месяцев", value);
            }
        }
    }

    @Override
    public String toString() {
        return "Pet{" +
                "id=" + id +
                ", nickname='" + nickname + '\'' +
                ", breed='" + breed + '\'' +
                ", age=" + getAge() +
                ", character='" + character + '\'' +
                ", lookingForOwner=" + lookingForOwner +
                '}';
    }


}
