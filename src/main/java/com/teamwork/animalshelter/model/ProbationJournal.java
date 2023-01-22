package com.teamwork.animalshelter.model;

import javax.persistence.*;
import java.util.Objects;

/**
 * Этот класс хранит в себе данные о журналах (отчетах) усыновителей находящихся на испытательном сроке:
 * @probation_id - id испытательного срока;
 * @date - дата записи в журнале;
 * @photo_recievid - статус наличия фото в журнале;
 * @report_recieved - статус наличия отчета в журнале.
 */

@Entity
public class ProbationJournal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "probation_id")
    private int probationId;

    private String date;

    @Column(name = "photo_recieved")
    private boolean photoRecieved;

    @Column(name = "report_recieved")
    private boolean reportRecieved;

    public ProbationJournal(int id, int probationId, String date, boolean photoRecieved, boolean reportRecieved) {
        this.id = id;
        this.probationId = probationId;
        this.date = date;
        this.photoRecieved = photoRecieved;
        this.reportRecieved = reportRecieved;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProbationId() {
        return probationId;
    }

    public void setProbationId(int probationId) {
        this.probationId = probationId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isPhotoRecieved() {
        return photoRecieved;
    }

    public void setPhotoRecieved(boolean photoRecieved) {
        this.photoRecieved = photoRecieved;
    }

    public boolean isReportRecieved() {
        return reportRecieved;
    }

    public void setReportRecieved(boolean reportRecieved) {
        this.reportRecieved = reportRecieved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProbationJournal that = (ProbationJournal) o;
        return id == that.id && probationId == that.probationId && photoRecieved == that.photoRecieved && reportRecieved == that.reportRecieved && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, probationId, date, photoRecieved, reportRecieved);
    }

    @Override
    public String toString() {
        return "ProbationJournal{" +
                "id=" + id +
                ", probationId=" + probationId +
                ", date='" + date + '\'' +
                ", photoRecieved=" + photoRecieved +
                ", reportRecieved=" + reportRecieved +
                '}';
    }
}
