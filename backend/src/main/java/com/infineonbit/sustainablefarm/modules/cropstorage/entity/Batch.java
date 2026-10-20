package com.infineonbit.sustainablefarm.modules.cropstorage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch")
@Getter
@Setter
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Prevent concurrent stock updates from overwriting each other; the losing update fails and can retry.
    @Version
    private Long version;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String batchCode;

    @NotBlank
    @Column(nullable = false)
    private String productType;

        // Anar: lifecycle stages FRESH -> DRIED -> PACKAGED -> WASTE / RETURNED
    private String productStage;

    private String origin;
        @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    private String mangoVariety;

    private LocalDate harvestDate;

    // Optional Plants harvest ID (GET /api/plants/harvests) for traceability; null when the batch is not from Plants.
    private Long harvestId;

    private LocalDate processingDate;

    private LocalDate packagingDate;

    private LocalDate storageEntryDate;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal initialQuantityKg;

    @PositiveOrZero
    @Column(precision = 10, scale = 2)
    private BigDecimal currentQuantityKg;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(precision = 5, scale = 2)
    private BigDecimal moisturePercent;

    private String qualityGrade;

    @NotBlank
    private String status;

            @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_zone_id")
    private StorageZone storageZone;

    private LocalDate expectedDispatchDate;

        private LocalDate expiryDate;

    // FIFO sorts higher priorities first; 0 must sort after positive priorities on H2 and PostgreSQL.
    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer customerPriority = 0;

    @PositiveOrZero
    @Column(precision = 12, scale = 2)
    private BigDecimal valuePerKgFcfa;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (currentQuantityKg == null) {
            currentQuantityKg = initialQuantityKg;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}