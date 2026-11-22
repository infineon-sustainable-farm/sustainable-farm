package com.infineonbit.sustainablefarm.modules.cropstorage.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

/**
 * A batch held in the store. Shared concepts are referenced by key only
 * (ADR 0003 and 0004), with the foreign keys of CropStorageForeignKeys: the
 * mango variety by its Plants catalogue key, the harvest by its Plants id.
 */
@Entity(name = "CropStorageBatch")
@Table(name = "crop_storage_batch")
@Getter
@Setter
public class CropStorageBatch extends BaseEntity {

    // Prevent concurrent stock updates from overwriting each other; the losing update fails and can retry.
    @Version
    private Long version;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String batchCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CropProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductStage productStage;

    private String origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** Key of the Plants variety catalogue (plants.api.VarietyCatalog), never a typed name. */
    @Column(name = "variety_key", length = 40)
    private String varietyKey;

    private LocalDate harvestDate;

    /** Optional Plants harvest (plants.api.HarvestLookup); null when the batch does not come from Plants. */
    @Column(name = "harvest_id")
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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchStatus status;

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

    /** Value of one kilogram, in XOF (CFA franc), the currency of the inventory valuation. */
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "value_per_kg_amount", precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "value_per_kg_currency", length = 3))
    private Money valuePerKg;

    /** A new batch holds its whole initial quantity until a movement or a loss changes it. */
    @PrePersist
    protected void defaultCurrentQuantity() {
        if (currentQuantityKg == null) {
            currentQuantityKg = initialQuantityKg;
        }
    }
}
