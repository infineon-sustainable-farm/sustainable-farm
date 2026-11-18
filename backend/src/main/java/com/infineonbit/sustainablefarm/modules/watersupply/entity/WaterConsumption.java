package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "water_consumption", indexes = {
        @Index(name = "idx_water_consumption_farm_id", columnList = "farm_id"),
        @Index(name = "idx_water_consumption_source_id", columnList = "source_id"),
        @Index(name = "idx_water_consumption_date", columnList = "consumption_date"),
        @Index(name = "idx_water_consumption_zone_date", columnList = "zone_id, consumption_date"),
        @Index(name = "idx_water_consumption_lot_id", columnList = "lot_id")
})
public class WaterConsumption extends BaseEntity {
    @NotNull
    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    @NotNull
    @Column(nullable = false)
    private Long sourceId;

    @NotNull
    @Column(nullable = false)
    private Double consumptionLiters;

    @NotNull
    @Column(nullable = false)
    private Instant consumptionDate;

    private UUID irrigationId;

    /**
     * Zone concerned by the measurement, when the flow meter is installed on a
     * specific zone: this link allows comparing the actually measured volume to the theoretical
     * volume of the drip network (clogging / leak detection).
     */
    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "lot_id", length = 128)
    private String lotId;

    @Column(name = "activity", length = 32)
    private String activity;

    public Long getFarmId() {
        return farmId;
    }

    public void setFarmId(Long farmId) {
        this.farmId = farmId;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
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

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getLotId() {
        return lotId;
    }

    public void setLotId(String lotId) {
        this.lotId = lotId;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }
}
