package model;

import javax.persistence.*;
import java.util.Objects;

/**
 * Этот класс хранит в себе данные которые содержатся в журнале (отчете):
 */

@Entity
public class ProbationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private ProbationDataType type;

    private String link;

    @ManyToOne
    @JoinColumn(name = "probation_journal_id")
    private ProbationJournal probationJournal;

    public ProbationData(int id, ProbationDataType type, String link, ProbationJournal probationJournal) {
        this.id = id;
        this.type = type;
        this.link = link;
        this.probationJournal = probationJournal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ProbationDataType getType() {
        return type;
    }

    public void setType(ProbationDataType type) {
        this.type = type;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public ProbationJournal getProbationJournal() {
        return probationJournal;
    }

    public void setProbationJournal(ProbationJournal probationJournal) {
        this.probationJournal = probationJournal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProbationData that = (ProbationData) o;
        return id == that.id && type == that.type && Objects.equals(link, that.link) && Objects.equals(probationJournal, that.probationJournal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, link, probationJournal);
    }

    @Override
    public String toString() {
        return "ProbationData{" +
                "id=" + id +
                ", type=" + type +
                ", link='" + link + '\'' +
                ", probationJournal=" + probationJournal +
                '}';
    }
}
