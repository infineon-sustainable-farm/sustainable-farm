package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "water_level_history")
public class WaterLevelHistory extends BaseEntity {
    @Column(nullable = false)
    private UUID sourceId;
    @Column(nullable = false)
    private Double levelLiters;
    @Column(nullable = false)
    private Instant measuredAt;

    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
    public Double getLevelLiters() { return levelLiters; }
    public void setLevelLiters(Double levelLiters) { this.levelLiters = levelLiters; }
    public Instant getMeasuredAt() { return measuredAt; }
    public void setMeasuredAt(Instant measuredAt) { this.measuredAt = measuredAt; }
}
