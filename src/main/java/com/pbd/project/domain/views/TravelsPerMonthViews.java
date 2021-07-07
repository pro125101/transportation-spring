package com.pbd.project.domain.views;


import org.springframework.data.annotation.Immutable;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Immutable
@Table(name="ExportLocationsView")
public class TravelsPerMonthViews implements Serializable {

    @Id
    @Column(name="month")
    private Integer month;

    @Column(name="count")
    private int count;

    @Column(name="current_year")
    private int currentYear;

    @Override
    public String toString() {
        return "TravelsPerMonthViews{" +
                "month=" + month +
                '}';
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(int currentYear) {
        this.currentYear = currentYear;
    }
}
