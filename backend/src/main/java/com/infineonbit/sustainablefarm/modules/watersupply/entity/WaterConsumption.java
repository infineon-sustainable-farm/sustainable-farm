package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "water_consumption", indexes = {
        @Index(name = "idx_water_consumption_farm_id", columnList = "farm_id"),
        @Index(name = "idx_water_consumption_source_id", columnList = "source_id"),
        @Index(name = "idx_water_consumption_date", columnList = "consumption_date"),
        @Index(name = "idx_water_consumption_zone_date", columnList = "zone_id, consumption_date"),
        @Index(name = "idx_water_consumption_lot_id", columnList = "lot_id"),
        @Index(name = "idx_water_consumption_processing_batch_id", columnList = "processing_batch_id")
})
public class WaterConsumption extends BaseEntity {
    @NotNull
    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    @NotNull
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    /** Read-only link that declares the foreign key fk_water_consumption_source; the code writes the source_id column. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_water_consumption_source"))
    private WaterSource source;

    @NotNull
    @Column(nullable = false)
    private Double consumptionLiters;

    @NotNull
    @Column(nullable = false)
    private Instant consumptionDate;

    @Column(name = "irrigation_id")
    private Long irrigationId;

    /** Read-only link that declares the foreign key fk_water_consumption_irrigation; the code writes the irrigation_id column. */
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "irrigation_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_water_consumption_irrigation"))
    private IrrigationSchedule irrigation;

    /**
     * Zone concerned by the measurement, when the flow meter is installed on a
     * specific zone: this link allows comparing the actually measured volume to the theoretical
     * volume of the drip network (clogging / leak detection).
     */
    @Column(name = "zone_id")
    private Long zoneId;

    /** Lot the consumption is attributed to, as sent: a product transformation batch id or any other lot. */
    @Column(name = "lot_id", length = 128)
    private String lotId;

    /**
     * Id of the product transformation batch whose code is {@link #lotId}, or {@code null}: foreign key to
     * {@code batch} ({@code fk_water_consumption_processing_batch}), resolved when the lot is
     * written through {@code producttransformation.api.ProcessingBatchLookup}. Not part of the JSON.
     */
    @Column(name = "processing_batch_id")
    private Long processingBatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity", length = 32)
    private ConsumptionActivity activity;

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

    public Long getIrrigationId() {
        return irrigationId;
    }

    public void setIrrigationId(Long irrigationId) {
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

    public Long getProcessingBatchId() {
        return processingBatchId;
    }

    public void setProcessingBatchId(Long processingBatchId) {
        this.processingBatchId = processingBatchId;
    }

    public ConsumptionActivity getActivity() {
        return activity;
    }

    public void setActivity(ConsumptionActivity activity) {
        this.activity = activity;
    }
}
