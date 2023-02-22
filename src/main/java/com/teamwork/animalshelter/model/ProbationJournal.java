package com.teamwork.animalshelter.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * Этот класс хранит в себе данные о журналах (отчетах) усыновителей находящихся на испытательном сроке:
 * @probation_id - id испытательного срока;
 * @date - дата записи в журнале;
 * @photo_received - статус наличия фото в журнале;
 * @report_received - статус наличия отчета в журнале.
 */

@Entity
@Table(name = "probation_journal")
public class ProbationJournal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "probation_id")
    private Probation probation;

    private LocalDateTime date;

    @Column(name = "photo_received")
    private boolean photoReceived;

    @Column(name = "report_received")
    private boolean reportReceived;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "probationJournal", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProbationData> probationDataSet;

    public ProbationJournal() {}

    public ProbationJournal(LocalDateTime date, boolean photoReceived, boolean reportReceived) {
        this.date = date;
        this.photoReceived = photoReceived;
        this.reportReceived = reportReceived;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Probation getProbation() {
        return probation;
    }

    public void setProbation(Probation probation) {
        this.probation = probation;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public boolean isPhotoReceived() {
        return photoReceived;
    }

    public void setPhotoReceived(boolean photoReceived) {
        this.photoReceived = photoReceived;
    }

    public boolean isReportReceived() {
        return reportReceived;
    }

    public void setReportReceived(boolean reportReceived) {
        this.reportReceived = reportReceived;
    }

    public Set<ProbationData> getProbationDataSet() {
        return probationDataSet;
    }

    public void setProbationDataSet(Set<ProbationData> probationDataSet) {
        this.probationDataSet = probationDataSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProbationJournal that = (ProbationJournal) o;
        return id == that.id && photoReceived == that.photoReceived && reportReceived == that.reportReceived && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, photoReceived, reportReceived);
    }

    @Override
    public String toString() {
        return "ProbationJournal{" +
                "id=" + id +
                ", probationId=" + probation.getId() +
                ", date='" + date + '\'' +
                ", photoReceived=" + photoReceived +
                ", reportReceived=" + reportReceived +
                '}';
    }
}
