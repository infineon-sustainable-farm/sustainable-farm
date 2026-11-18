package com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.model;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;

import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;
import java.math.BigDecimal;

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
@Table(name = "raw_intake",
        uniqueConstraints = @UniqueConstraint(name = "uk_raw_intake_batch_id", columnNames = "batch_id"),
        indexes = {
                @Index(name = "idx_raw_intake_received_variety_key", columnList = "received_variety_key"),
                @Index(name = "idx_raw_intake_source_block_id", columnList = "source_block_id")
        })
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RawIntake {

    @Id
    @GeneratedValue(generator = "assigned")
    @GenericGenerator(name = "assigned", strategy = "assigned")
    @Column(name = "intake_id", length = 50)
    private String intakeId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", foreignKey = @ForeignKey(name = "fk_raw_intake_batch"), nullable = false, referencedColumnName = "batch_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Batch batch;

    /**
     * Block of the farm the fruit comes from: foreign key to {@code farm.block} (ADR 0004). The API
     * shows it as {@code sourceBlock} (the block code) and {@code sourceFarm} (the name of its farm).
     */
    @Column(name = "source_block_id", nullable = false)
    private Long sourceBlockId;

    @Column(name = "intake_date", nullable = false)
    private LocalDate intakeDate;

    @Column(name = "received_quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal receivedQuantityKg;

    /** Catalogue key of the variety received, for example {@code KENT}: foreign key to plants {@code variety_reference}. */
    @Column(name = "received_variety_key", nullable = false, length = VarietyCatalog.KEY_LENGTH)
    private String receivedVarietyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "received_grade", nullable = false, length = 1)
    private HarvestEvent.QualityGrade receivedGrade;

    /** The intake operator, a person of the staff referential ({@code person.id}); the API shows the name. */
    @Column(name = "intake_operator_id")
    private Long intakeOperatorId;

    @Column(name = "created_at", updatable = false)
    private java.sql.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.sql.Timestamp updatedAt;

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