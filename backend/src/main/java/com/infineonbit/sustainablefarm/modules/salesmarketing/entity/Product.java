package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * A product sold by the farm.
 *
 * <p>The variety is a key of the plants variety catalogue ({@link VarietyCatalog}),
 * stored in {@code variety_key} with a foreign key to it. The API still sends and
 * receives the variety as a name, {@code variety}: {@code ProductService} resolves
 * the name to its key on write and gives the catalogue name on read.
 */
@Entity
@Table(name = "products",
        indexes = @Index(name = "idx_products_variety_key", columnList = "variety_key"))
@Getter
@Setter
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Catalogue key of the variety, for example {@code KENT}, or {@code null}. */
    @Column(name = "variety_key", length = VarietyCatalog.KEY_LENGTH)
    private String varietyKey;

    /** {@code kg} when none is sent. */
    @ColumnDefault("'kg'")
    @Column(name = "unit", nullable = false, length = 10)
    private String unit;
}
