package com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.model;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;

import jakarta.persistence.*;
import jakarta.persistence.Convert;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * PackagingRecord Entity
 * Packaging stage data - Core Processing Entity
 * 
 * Business Rule: Lot codes mandatory for traceability compliance
 * Business Rule: Equipment can be assigned to one batch at a time
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "packaging_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_packaging_record_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_packaging_record_lot_code", columnNames = "lot_code")
        },
        indexes = {
                @Index(name = "idx_packaging_record_batch_id", columnList = "batch_id"),
                @Index(name = "idx_packaging_record_equipment_id", columnList = "equipment_id"),
                @Index(name = "idx_packaging_record_operator_id", columnList = "operator_id")
        })
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PackagingRecord extends BaseEntity {

    /** Business code, assigned by the client and shown by the API; the key is {@code id}. */
    @Column(name = "code", nullable = false, length = 50)
    private String recordId;

    // Eager: the responses show its code, read after the transaction (open-in-view is off).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", foreignKey = @ForeignKey(name = "fk_packaging_record_batch"), nullable = false)
    @ToString.Exclude
    private Batch batch;

    @Convert(converter = com.infineonbit.sustainablefarm.modules.producttransformation.config.PackageTypeConverter.class)
    @Column(name = "package_type", nullable = false, length = 20)
    private PackageType packageType;

    @Column(name = "package_quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal packageQuantityKg;

    @Column(name = "lot_code", nullable = false, length = 50)
    private String lotCode;

    @Column(name = "export_ready", nullable = false)
    private Boolean exportReady = false;

    @Column(name = "packaging_date", nullable = false)
    private LocalDate packagingDate;

    /** The register id of the equipment used (machinery {@code equipment}); the equipment cannot be deleted while used. */
    @Column(name = "equipment_id")
    private Long equipmentId;

    // Eager: the responses show its code, read after the transaction (open-in-view is off).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operator_id", foreignKey = @ForeignKey(name = "fk_packaging_record_operator"))
    private Operator operator;

    /**
     * Validate lot code format
     * Business Rule: Lot codes mandatory for traceability compliance
     */
    @PrePersist
    @PreUpdate
    void validateLotCode() {
        if (lotCode == null || lotCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Lot code is mandatory for traceability compliance");
        }
    }

    /**
     * Mark as export ready
     * Business Rule: Requires export_ready flag for EU compliance
     */
    public void markAsExportReady() {
        this.exportReady = true;
    }

}