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

import java.time.LocalDate;

/** The price of a competitor for a product on a day, in euros. */
@Entity
@Table(name = "competitor_prices", indexes = @Index(name = "idx_competitor_prices_product", columnList = "product_id, price_date"))
@Getter
@Setter
public class CompetitorPrice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "competitor_prices_product_id_fkey"))
    private Product product;

    @Column(name = "competitor_name", nullable = false, length = 50)
    private String competitorName;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price_amount", nullable = false, precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "price_currency", nullable = false, length = 3))
    private Money price;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    /** {@code false} when none is sent. */
    @ColumnDefault("false")
    @Column(name = "is_target", nullable = false)
    private Boolean isTarget;
}
