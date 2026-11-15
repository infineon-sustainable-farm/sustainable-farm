package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "competitor_prices")
public class CompetitorPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "competitor_price_id")
    private Integer competitorPriceId;

    @Column(name = "product_id")
    private Integer productId;

    @NotBlank(message = "competitorName is required")
    @Column(name = "competitor_name")
    private String competitorName;

    @NotNull(message = "priceEur is required")
    @Positive
    @Column(name = "price_eur")
    private BigDecimal priceEur;

    @NotNull(message = "priceDate is required")
    @Column(name = "price_date")
    private LocalDate priceDate;

    @Column(name = "is_target")
    private Boolean isTarget;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getCompetitorPriceId() {
        return competitorPriceId;
    }

    public void setCompetitorPriceId(Integer competitorPriceId) {
        this.competitorPriceId = competitorPriceId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getCompetitorName() {
        return competitorName;
    }

    public void setCompetitorName(String competitorName) {
        this.competitorName = competitorName;
    }

    public BigDecimal getPriceEur() {
        return priceEur;
    }

    public void setPriceEur(BigDecimal priceEur) {
        this.priceEur = priceEur;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public Boolean getIsTarget() {
        return isTarget;
    }

    public void setIsTarget(Boolean isTarget) {
        this.isTarget = isTarget;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}