package com.teamwork.animalshelter.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "support")
public class Support {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "type")
    private SupportType type;

    @Column(name = "datetime_begin")
    private LocalDateTime beginDateTime;

    @Column(name = "datetime_finish")
    private LocalDateTime finishDateTime;

    @Column(name = "finish")
    private boolean finish;

    @ManyToOne()
    @JoinColumn(name = "client_id_client")
    private User user;

    @ManyToOne()
    @JoinColumn(name = "client_id_volunteer")
    private User volunteer;

    public Support() {}

    public Support(SupportType type, LocalDateTime beginDateTime, User user, User volunteer) {
        this.type = type;
        this.beginDateTime = beginDateTime;
        this.user = user;
        this.volunteer = volunteer;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public SupportType getType() {
        return type;
    }

    public void setType(SupportType type) {
        this.type = type;
    }

    public LocalDateTime getBeginDateTime() {
        return beginDateTime;
    }

    public void setBeginDateTime(LocalDateTime beginDateTime) {
        this.beginDateTime = beginDateTime;
    }

    public LocalDateTime getFinishDateTime() {
        return finishDateTime;
    }

    public void setFinishDateTime(LocalDateTime finishDateTime) {
        this.finishDateTime = finishDateTime;
    }

    public boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(User volunteer) {
        this.volunteer = volunteer;
    }

    @Override
    public String toString() {
        return "Support{" +
                "id=" + id +
                ", type=" + type +
                ", beginDateTime=" + beginDateTime +
                ", finishDateTime=" + finishDateTime +
                ", finish=" + finish +
                ", user=" + user.getId() +
                ", volunteer=" + volunteer.getId() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Support support = (Support) o;
        return finish == support.finish && type == support.type && user.equals(support.user) && Objects.equals(volunteer, support.volunteer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, finish, user, volunteer);
    }
}
