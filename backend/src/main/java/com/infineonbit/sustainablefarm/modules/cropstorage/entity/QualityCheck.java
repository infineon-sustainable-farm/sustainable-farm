package com.infineonbit.sustainablefarm.modules.cropstorage.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A quality inspection of a batch. The inspector is a person of the shared
 * people directory (staff module, ADR 0004): only the key is stored here, with
 * a foreign key declared in CropStorageForeignKeys.
 */
@Entity
@Table(name = "quality_check")
@Getter
@Setter
public class QualityCheck extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private CropStorageBatch batch;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(precision = 5, scale = 2)
    private BigDecimal moisturePercent;

    private String qualityGrade;

    @NotNull
    @Column(nullable = false)
    private Boolean passed;

    @Column(name = "inspector_person_id")
    private Long inspectorPersonId;

    private String notes;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime checkedAt;
}
