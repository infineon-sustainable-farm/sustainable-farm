package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The price of a product per kilogram on a day, in euros, with the factors behind it. */
@Entity
@Table(name = "pricing_history", indexes = @Index(name = "idx_pricing_product_date", columnList = "product_id, price_date"))
@Getter
@Setter
public class PricingHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "pricing_history_product_id_fkey"))
    private Product product;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price_per_kg_amount", nullable = false, precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "price_per_kg_currency", nullable = false, length = 3))
    private Money pricePerKg;

    @Column(name = "harvest_season_factor", precision = 5, scale = 2)
    private BigDecimal harvestSeasonFactor;

    @Column(name = "season_label", length = 30)
    private String seasonLabel;

    @Column(name = "stock_level_tons", precision = 10, scale = 2)
    private BigDecimal stockLevelTons;

    @Column(name = "stock_target_tons", precision = 10, scale = 2)
    private BigDecimal stockTargetTons;

    @Column(name = "demand_factor", precision = 5, scale = 2)
    private BigDecimal demandFactor;

    @Column(name = "demand_index_label", length = 20)
    private String demandIndexLabel;

    @ColumnDefault("false")
    @Column(name = "is_applied")
    private Boolean isApplied;
}
