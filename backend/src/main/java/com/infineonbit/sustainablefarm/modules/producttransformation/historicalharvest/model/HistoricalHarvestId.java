package com.infineonbit.sustainablefarm.modules.producttransformation.historicalharvest.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for HistoricalHarvest Entity
 * 
 * Database Primary Key: (year, month, week, variety_key)
 * 
 * Architecture Note:
 * - This class implements Serializable as required by JPA for composite keys
 * - equals() and hashCode() are implemented on all four key fields
 * - varietyKey is the catalogue key of the variety (plants variety_reference)
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public class HistoricalHarvestId implements Serializable {

    private Integer year;
    private Integer month;
    private Integer week;
    private String varietyKey;

    public HistoricalHarvestId() {
    }

    public HistoricalHarvestId(Integer year, Integer month, Integer week, String varietyKey) {
        this.year = year;
        this.month = month;
        this.week = week;
        this.varietyKey = varietyKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoricalHarvestId that = (HistoricalHarvestId) o;
        return Objects.equals(year, that.year) &&
               Objects.equals(month, that.month) &&
               Objects.equals(week, that.week) &&
               Objects.equals(varietyKey, that.varietyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, week, varietyKey);
    }

    @Override
    public String toString() {
        return "HistoricalHarvestId{" +
                "year=" + year +
                ", month=" + month +
                ", week=" + week +
                ", varietyKey=" + varietyKey +
                '}';
    }
}