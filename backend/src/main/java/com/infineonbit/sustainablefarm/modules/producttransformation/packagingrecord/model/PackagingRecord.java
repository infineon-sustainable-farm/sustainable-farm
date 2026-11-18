package com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.model;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import jakarta.persistence.Convert;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
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
        uniqueConstraints = @UniqueConstraint(name = "uk_packaging_record_lot_code", columnNames = "lot_code"),
        indexes = @Index(name = "idx_packaging_record_equipment_id", columnList = "equipment_id"))
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PackagingRecord {

    @Id
    @GeneratedValue(generator = "assigned")
    @GenericGenerator(name = "assigned", strategy = "assigned")
    @Column(name = "record_id", length = 50)
    private String recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", foreignKey = @ForeignKey(name = "fk_packaging_record_batch"), nullable = false, referencedColumnName = "batch_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", foreignKey = @ForeignKey(name = "fk_packaging_record_operator"), referencedColumnName = "operator_id")
    private Operator operator;

    @Column(name = "created_at", updatable = false)
    private java.sql.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.sql.Timestamp updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.sql.Timestamp(System.currentTimeMillis());
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
        validateLotCode();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
        validateLotCode();
    }

    /**
     * Validate lot code format
     * Business Rule: Lot codes mandatory for traceability compliance
     */
    private void validateLotCode() {
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

    /**
     * Package Type Enum
     * Business Rule: Matches database CHECK constraint with custom converter
     */
    public enum PackageType {
        ONE_KG_BAG,  // Maps to "1KG_BAG" in database via PackageTypeConverter
        TWO_KG_BAG,  // Maps to "2KG_BAG" in database via PackageTypeConverter
        BULK
    }
}