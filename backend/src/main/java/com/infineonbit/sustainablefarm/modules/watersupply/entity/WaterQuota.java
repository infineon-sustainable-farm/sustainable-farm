package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "water_quotas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_water_quotas_target_month", columnNames = {"target_type", "target_id", "quota_month"})
})
public class WaterQuota extends BaseEntity {
    @NotBlank
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @NotNull
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @NotNull
    @Column(name = "quota_month", nullable = false)
    private LocalDate quotaMonth;

    @Positive
    @Column(name = "quota_liters", nullable = false)
    private Double quotaLiters;

    @Column(length = 120)
    private String label;

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public LocalDate getQuotaMonth() {
        return quotaMonth;
    }

    public void setQuotaMonth(LocalDate quotaMonth) {
        this.quotaMonth = quotaMonth;
    }

    public Double getQuotaLiters() {
        return quotaLiters;
    }

    public void setQuotaLiters(Double quotaLiters) {
        this.quotaLiters = quotaLiters;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
