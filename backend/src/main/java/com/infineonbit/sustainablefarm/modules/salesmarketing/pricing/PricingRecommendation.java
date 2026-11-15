package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_recommendations")
public class PricingRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Integer recommendationId;

    @Column(name = "product_id")
    private Integer productId;

    @NotBlank(message = "message is required")
    @Column(name = "message")
    private String message;

    @Column(name = "suggested_price_eur")
    private BigDecimal suggestedPriceEur;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(Integer recommendationId) {
        this.recommendationId = recommendationId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getSuggestedPriceEur() {
        return suggestedPriceEur;
    }

    public void setSuggestedPriceEur(BigDecimal suggestedPriceEur) {
        this.suggestedPriceEur = suggestedPriceEur;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}