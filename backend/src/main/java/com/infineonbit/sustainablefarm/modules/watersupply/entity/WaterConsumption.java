package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "water_consumption")
public class WaterConsumption extends BaseEntity {
    @NotNull
    @Column(nullable = false)
    private UUID farmId;

    @NotNull
    @Column(nullable = false)
    private UUID sourceId;

    @NotNull
    @Column(nullable = false)
    private Double consumptionLiters;

    @NotNull
    @Column(nullable = false)
    private Instant consumptionDate;

    private UUID irrigationId;

    /**
     * Zone concernee par la mesure, quand le compteur de debit est installe sur une zone
     * precise : c'est ce lien qui permet de comparer le volume reellement mesure au volume
     * theorique du reseau goutte-a-goutte (detection de colmatage / fuite).
     */
    private UUID zoneId;

    public UUID getFarmId() {
        return farmId;
    }

    public void setFarmId(UUID farmId) {
        this.farmId = farmId;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public Double getConsumptionLiters() {
        return consumptionLiters;
    }

    public void setConsumptionLiters(Double consumptionLiters) {
        this.consumptionLiters = consumptionLiters;
    }

    public Instant getConsumptionDate() {
        return consumptionDate;
    }

    public void setConsumptionDate(Instant consumptionDate) {
        this.consumptionDate = consumptionDate;
    }

    public UUID getIrrigationId() {
        return irrigationId;
    }

    public void setIrrigationId(UUID irrigationId) {
        this.irrigationId = irrigationId;
    }

    public UUID getZoneId() {
        return zoneId;
    }

    public void setZoneId(UUID zoneId) {
        this.zoneId = zoneId;
    }
}