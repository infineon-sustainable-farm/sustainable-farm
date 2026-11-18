package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A pricing advice for a product, with a suggested price in euros. */
@Entity
@Table(name = "pricing_recommendations")
@Getter
@Setter
public class PricingRecommendation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "pricing_recommendations_product_id_fkey"))
    private Product product;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "suggested_price_amount", precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "suggested_price_currency", length = 3))
    private Money suggestedPrice;
}
