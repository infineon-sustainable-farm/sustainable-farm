package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "water_quality_tests", indexes = {
        @Index(name = "idx_water_quality_tests_source_id", columnList = "source_id"),
        @Index(name = "idx_water_quality_tests_test_date", columnList = "test_date")
})
public class WaterQualityTest extends BaseEntity {
    @NotNull
    @Column(nullable = false)
    private Long sourceId;

    private Double ph;
    private Double temperatureCelsius;
    private Double turbidityNtu;
    private Double conductivityUsCm;
    private Double salinityPpt;
    private Instant testDate;

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Double getPh() {
        return ph;
    }

    public void setPh(Double ph) {
        this.ph = ph;
    }

    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(Double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public Double getTurbidityNtu() {
        return turbidityNtu;
    }

    public void setTurbidityNtu(Double turbidityNtu) {
        this.turbidityNtu = turbidityNtu;
    }

    public Double getConductivityUsCm() {
        return conductivityUsCm;
    }

    public void setConductivityUsCm(Double conductivityUsCm) {
        this.conductivityUsCm = conductivityUsCm;
    }

    public Double getSalinityPpt() {
        return salinityPpt;
    }

    public void setSalinityPpt(Double salinityPpt) {
        this.salinityPpt = salinityPpt;
    }

    public Instant getTestDate() {
        return testDate;
    }

    public void setTestDate(Instant testDate) {
        this.testDate = testDate;
    }
}