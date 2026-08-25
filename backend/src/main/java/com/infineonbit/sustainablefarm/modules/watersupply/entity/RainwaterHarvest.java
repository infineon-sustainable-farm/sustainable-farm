package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rainwater_harvests")
public class RainwaterHarvest extends BaseEntity {
    @NotNull
    @Column(nullable = false)
    private UUID sourceId;

    @NotNull
    @Column(name = "catchment_area_m2", nullable = false)
    private Double catchmentAreaM2;

    @NotNull
    @Column(name = "rainfall_mm", nullable = false)
    private Double rainfallMm;

    @NotNull
    @Column(name = "runoff_coefficient", nullable = false)
    private Double runoffCoefficient;

    @Column(name = "harvested_liters")
    private Double harvestedLiters;

    @NotNull
    @Column(name = "capture_date", nullable = false)
    private Instant captureDate;

    @PrePersist
    void computeHarvestedLiters() {
        if (harvestedLiters == null) {
            harvestedLiters = catchmentAreaM2 * rainfallMm * runoffCoefficient;
        }
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public Double getCatchmentAreaM2() {
        return catchmentAreaM2;
    }

    public void setCatchmentAreaM2(Double catchmentAreaM2) {
        this.catchmentAreaM2 = catchmentAreaM2;
    }

    public Double getRainfallMm() {
        return rainfallMm;
    }

    public void setRainfallMm(Double rainfallMm) {
        this.rainfallMm = rainfallMm;
    }

    public Double getRunoffCoefficient() {
        return runoffCoefficient;
    }

    public void setRunoffCoefficient(Double runoffCoefficient) {
        this.runoffCoefficient = runoffCoefficient;
    }

    public Double getHarvestedLiters() {
        return harvestedLiters;
    }

    public void setHarvestedLiters(Double harvestedLiters) {
        this.harvestedLiters = harvestedLiters;
    }

    public Instant getCaptureDate() {
        return captureDate;
    }

    public void setCaptureDate(Instant captureDate) {
        this.captureDate = captureDate;
    }
}