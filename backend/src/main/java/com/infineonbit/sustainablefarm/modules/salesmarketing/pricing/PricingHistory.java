package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "pricing_history", indexes = @Index(name = "idx_pricing_product_date", columnList = "product_id, price_date"))
public class PricingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pricing_id")
    private Integer pricingId;

    @NotNull(message = "productId is required")
    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @NotNull(message = "priceDate is required")
    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @NotNull(message = "priceEurPerKg is required")
    @Positive(message = "priceEurPerKg must be greater than zero")
    @Column(name = "price_eur_per_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal priceEurPerKg;

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

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
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