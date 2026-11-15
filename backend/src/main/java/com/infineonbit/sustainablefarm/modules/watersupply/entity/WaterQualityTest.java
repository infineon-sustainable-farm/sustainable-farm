package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "water_quality_tests")
public class WaterQualityTest extends BaseEntity {
    @NotNull
    @Column(nullable = false)
    private UUID sourceId;

    private Double ph;
    private Double temperatureCelsius;
    private Double turbidityNtu;
    private Double conductivityUsCm;
    private Double salinityPpt;
    private Instant testDate;

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
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