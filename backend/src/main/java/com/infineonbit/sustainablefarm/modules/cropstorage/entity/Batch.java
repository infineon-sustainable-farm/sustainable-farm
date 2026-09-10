package com.infineonbit.sustainablefarm.modules.cropstorage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

    @NotBlank
    @Column(nullable = false, unique = true)
    private String batchCode;

    @NotBlank
    @Column(nullable = false)
    private String productType;

    private String productStage;

    private String origin;

    private String mangoVariety;

    private LocalDate harvestDate;

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

      @ManyToOne
    @JoinColumn(name = "storage_zone_id")
    private StorageZone storageZone;

    private LocalDate expectedDispatchDate;

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