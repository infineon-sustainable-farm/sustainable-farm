package com.sustainablefarm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;

/**
 * RawIntake Entity
 * Raw material intake from Plants - Supporting Entity
 * 
 * Business Rule: One intake initializes exactly one batch
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "raw_intake")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RawIntake {

    @Id
    @Column(name = "intake_id", length = 50)
    private String intakeId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false, unique = true, referencedColumnName = "batch_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Batch batch;

    @Column(name = "source_farm", nullable = false, length = 100)
    private String sourceFarm;

    @Column(name = "source_block", nullable = false, length = 50)
    private String sourceBlock;

    @Column(name = "intake_date", nullable = false)
    private LocalDate intakeDate;

    @Column(name = "received_quantity_kg", nullable = false, precision = 10, scale = 2)
    private Double receivedQuantityKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "received_variety", nullable = false, length = 20)
    private HarvestEvent.MangoVariety receivedVariety;

    @Enumerated(EnumType.STRING)
    @Column(name = "received_grade", nullable = false, length = 1)
    private HarvestEvent.QualityGrade receivedGrade;

    @Column(name = "intake_operator", length = 100)
    private String intakeOperator;

    @Column(name = "created_at", updatable = false)
    private java.time.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.time.Timestamp updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.sql.Timestamp(System.currentTimeMillis());
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
    }
}