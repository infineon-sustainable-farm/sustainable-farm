package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "water_level_history",
        indexes = @Index(name = "idx_water_level_history_source_date", columnList = "source_id, measured_at"))
public class WaterLevelHistory extends BaseEntity {
    @Column(nullable = false)
    private Long sourceId;
    @Column(nullable = false)
    private Double levelLiters;
    @Column(nullable = false)
    private Instant measuredAt;

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public Double getLevelLiters() { return levelLiters; }
    public void setLevelLiters(Double levelLiters) { this.levelLiters = levelLiters; }
    public Instant getMeasuredAt() { return measuredAt; }
    public void setMeasuredAt(Instant measuredAt) { this.measuredAt = measuredAt; }
}
