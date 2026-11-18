package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * Maps to the "products" table.
 * Foreign keys are kept as plain Integer id fields.
 *
 * <p>The variety is a key of the plants variety catalogue ({@link VarietyCatalog}),
 * stored in {@code variety_key} with a foreign key to it. The API still sends and
 * receives the variety as a name, {@code variety}: {@code ProductService} resolves
 * the name to its key on write and fills the catalogue name on read.
 */
@Entity
@Table(name = "products",
        indexes = @Index(name = "idx_products_variety_key", columnList = "variety_key"))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @NotBlank(message = "Product name is required")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Catalogue key of the variety, for example {@code KENT}, or {@code null}. */
    @JsonIgnore
    @Column(name = "variety_key", length = VarietyCatalog.KEY_LENGTH)
    private String varietyKey;

    /** Catalogue name of the variety, for example {@code "Kent"}: the value of the API, not stored. */
    @Transient
    private String variety;

    @ColumnDefault("'kg'")
    @Column(name = "unit", length = 10)
    private String unit;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ---- Getters and setters ----

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @JsonIgnore
    public String getVarietyKey() { return varietyKey; }
    public void setVarietyKey(String varietyKey) { this.varietyKey = varietyKey; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}