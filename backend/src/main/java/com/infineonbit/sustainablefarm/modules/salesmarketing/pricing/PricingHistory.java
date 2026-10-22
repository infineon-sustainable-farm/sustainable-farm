package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_history")
public class PricingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pricing_id")
    private Integer pricingId;

    @NotNull(message = "productId is required")
    @Column(name = "product_id")
    private Integer productId;

    @NotNull(message = "priceDate is required")
    @Column(name = "price_date")
    private LocalDate priceDate;

    @NotNull(message = "priceEurPerKg is required")
    @Positive(message = "priceEurPerKg must be greater than zero")
    @Column(name = "price_eur_per_kg")
    private BigDecimal priceEurPerKg;

    @Column(name = "harvest_season_factor")
    private BigDecimal harvestSeasonFactor;

    @Column(name = "season_label")
    private String seasonLabel;

    @Column(name = "stock_level_tons")
    private BigDecimal stockLevelTons;

    @Column(name = "stock_target_tons")
    private BigDecimal stockTargetTons;

    @Column(name = "demand_factor")
    private BigDecimal demandFactor;

    @Column(name = "demand_index_label")
    private String demandIndexLabel;

    @Column(name = "is_applied")
    private Boolean isApplied;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getPricingId() {
        return pricingId;
    }

    public void setPricingId(Integer pricingId) {
        this.pricingId = pricingId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public BigDecimal getPriceEurPerKg() {
        return priceEurPerKg;
    }

    public void setPriceEurPerKg(BigDecimal priceEurPerKg) {
        this.priceEurPerKg = priceEurPerKg;
    }

    public BigDecimal getHarvestSeasonFactor() {
        return harvestSeasonFactor;
    }

    public void setHarvestSeasonFactor(BigDecimal harvestSeasonFactor) {
        this.harvestSeasonFactor = harvestSeasonFactor;
    }

    public String getSeasonLabel() {
        return seasonLabel;
    }

    public void setSeasonLabel(String seasonLabel) {
        this.seasonLabel = seasonLabel;
    }

    public BigDecimal getStockLevelTons() {
        return stockLevelTons;
    }

    public void setStockLevelTons(BigDecimal stockLevelTons) {
        this.stockLevelTons = stockLevelTons;
    }

    public BigDecimal getStockTargetTons() {
        return stockTargetTons;
    }

    public void setStockTargetTons(BigDecimal stockTargetTons) {
        this.stockTargetTons = stockTargetTons;
    }

    public BigDecimal getDemandFactor() {
        return demandFactor;
    }

    public void setDemandFactor(BigDecimal demandFactor) {
        this.demandFactor = demandFactor;
    }

    public String getDemandIndexLabel() {
        return demandIndexLabel;
    }

    public void setDemandIndexLabel(String demandIndexLabel) {
        this.demandIndexLabel = demandIndexLabel;
    }

    public Boolean getIsApplied() {
        return isApplied;
    }

    public void setIsApplied(Boolean isApplied) {
        this.isApplied = isApplied;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}