package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * Maps to the "product_batches" table.
 * Foreign keys are kept as plain Integer id fields.
 */
@Entity
@Table(name = "product_batches", uniqueConstraints = @UniqueConstraint(name = "product_batches_batch_code_key", columnNames = "batch_code"),
        indexes = @Index(name = "idx_product_batches_status", columnList = "status"))
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
}