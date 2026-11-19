package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * A stock batch of a product. The status is the free label the API keeps.
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
                @Index(name = "idx_product_batches_packaging_record_id", columnList = "packaging_record_id"),
                @Index(name = "idx_product_batches_product_id", columnList = "product_id")})
public class ProductBatch extends BaseEntity {

    @Column(name = "batch_code", nullable = false, length = 20)
    private String batchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "product_batches_product_id_fkey"))
    private Product product;

    @Column(name = "stock_t", precision = 10, scale = 2)
    private BigDecimal stockT;

    @Column(name = "status", length = 20)
    private String status;

    /** The time of the last write. */
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    /** Id of the packaged lot whose lot code is {@link #batchCode}, or {@code null}. */
    @JsonIgnore
    @Column(name = "packaging_record_id")
    private Long packagingRecordId;

    // ---- Getters and setters ----

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getStockT() { return stockT; }
    public void setStockT(BigDecimal stockT) { this.stockT = stockT; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }

    @JsonIgnore
    public Long getPackagingRecordId() { return packagingRecordId; }
    @JsonIgnore
    public void setPackagingRecordId(Long packagingRecordId) { this.packagingRecordId = packagingRecordId; }
}