package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infineonbit.sustainablefarm.modules.producttransformation.api.PackagedLotLookup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * Maps to the "product_batches" table.
 * Foreign keys are kept as plain Integer id fields.
 *
 * <p>A sales batch whose code is the lot code of a product transformation packaged lot is linked
 * to it: {@code packaging_record_id}, foreign key to {@code packaging_record}
 * ({@code fk_product_batches_packaging_record}), resolved when the batch is written. The link is
 * not part of the JSON and nothing is synchronised from it (no stock update).
 */
@Entity
@Table(name = "product_batches", uniqueConstraints = @UniqueConstraint(name = "product_batches_batch_code_key", columnNames = "batch_code"),
        indexes = {
                @Index(name = "idx_product_batches_status", columnList = "status"),
                @Index(name = "idx_product_batches_packaging_record_id", columnList = "packaging_record_id")
        })
public class ProductBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Integer batchId;

    @NotBlank(message = "batchCode is required")
    @Column(name = "batch_code", nullable = false, length = 20)
    private String batchCode;

    @Column(name = "product_id")
    private Integer productId;

    @PositiveOrZero(message = "stockT cannot be negative")
    @Column(name = "stock_t", precision = 10, scale = 2)
    private BigDecimal stockT;

    @Column(name = "status", length = 20)
    private String status;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    /** Record id of the packaged lot whose lot code is {@link #batchCode}, or {@code null}. */
    @JsonIgnore
    @Column(name = "packaging_record_id", length = PackagedLotLookup.KEY_LENGTH)
    private String packagingRecordId;

    // ---- Getters and setters ----

    public Integer getBatchId() { return batchId; }
    public void setBatchId(Integer batchId) { this.batchId = batchId; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public BigDecimal getStockT() { return stockT; }
    public void setStockT(BigDecimal stockT) { this.stockT = stockT; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }

    @JsonIgnore
    public String getPackagingRecordId() { return packagingRecordId; }
    @JsonIgnore
    public void setPackagingRecordId(String packagingRecordId) { this.packagingRecordId = packagingRecordId; }
}