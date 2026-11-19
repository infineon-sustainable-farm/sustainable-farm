package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "rainwater_harvests", indexes = {
        @Index(name = "idx_rainwater_harvests_source_id", columnList = "source_id"),
        @Index(name = "idx_rainwater_harvests_capture_date", columnList = "capture_date")
})
public class RainwaterHarvest extends BaseEntity {
    @NotNull
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    /** Read-only link that declares the foreign key fk_rainwater_harvests_source; the code writes the source_id column. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_rainwater_harvests_source"))
    private WaterSource source;

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

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
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