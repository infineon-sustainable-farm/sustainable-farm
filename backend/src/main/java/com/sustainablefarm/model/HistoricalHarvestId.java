package com.sustainablefarm.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for HistoricalHarvest Entity
 * 
 * Database Primary Key: (year, month, week, mango_variety)
 * 
 * Architecture Note:
 * - This class implements Serializable as required by JPA for composite keys
 * - equals() and hashCode() are implemented on all four key fields
 * - mangoVariety is an enum, ensuring type safety
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public class HistoricalHarvestId implements Serializable {

    private Integer year;
    private Integer month;
    private Integer week;
    private HarvestEvent.MangoVariety mangoVariety;

    public HistoricalHarvestId() {
    }

    public HistoricalHarvestId(Integer year, Integer month, Integer week, HarvestEvent.MangoVariety mangoVariety) {
        this.year = year;
        this.month = month;
        this.week = week;
        this.mangoVariety = mangoVariety;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoricalHarvestId that = (HistoricalHarvestId) o;
        return Objects.equals(year, that.year) &&
               Objects.equals(month, that.month) &&
               Objects.equals(week, that.week) &&
               mangoVariety == that.mangoVariety;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, week, mangoVariety);
    }

    @Override
    public String toString() {
        return "HistoricalHarvestId{" +
                "year=" + year +
                ", month=" + month +
                ", week=" + week +
                ", mangoVariety=" + mangoVariety +
                '}';
    }
}