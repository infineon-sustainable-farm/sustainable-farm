package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "water_level_history",
        indexes = @Index(name = "idx_water_level_history_source_date", columnList = "source_id, measured_at"))
public class WaterLevelHistory extends BaseEntity {
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    /** Read-only link that declares the foreign key fk_water_level_history_source; the code writes the source_id column. */
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "source_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_water_level_history_source"))
    private WaterSource source;
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
