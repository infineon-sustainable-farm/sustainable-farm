package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps to the "demand_forecasts" table.
 * Foreign keys are kept as plain Integer id fields.
 */
@Entity
@Table(name = "demand_forecasts")
public class DemandForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forecast_id")
    private Integer forecastId;

    @Column(name = "product_id")
    private Integer productId;

    @NotNull(message = "forecastMonth is required")
    @Column(name = "forecast_month")
    private LocalDate forecastMonth;

    @NotBlank(message = "scenario is required")
    @Column(name = "scenario")
    private String scenario;

    @NotNull(message = "forecastedVolumeT is required")
    @PositiveOrZero
    @Column(name = "forecasted_volume_t")
    private BigDecimal forecastedVolumeT;

    @Column(name = "lower_bound_t")
    private BigDecimal lowerBoundT;

    @Column(name = "upper_bound_t")
    private BigDecimal upperBoundT;

    @Column(name = "capacity_risk")
    private String capacityRisk;

    @Column(name = "is_actual")
    private Boolean isActual;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getForecastId() {
        return forecastId;
    }

    public void setForecastId(Integer forecastId) {
        this.forecastId = forecastId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public LocalDate getForecastMonth() {
        return forecastMonth;
    }

    public void setForecastMonth(LocalDate forecastMonth) {
        this.forecastMonth = forecastMonth;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public BigDecimal getForecastedVolumeT() {
        return forecastedVolumeT;
    }

    public void setForecastedVolumeT(BigDecimal forecastedVolumeT) {
        this.forecastedVolumeT = forecastedVolumeT;
    }

    public BigDecimal getLowerBoundT() {
        return lowerBoundT;
    }

    public void setLowerBoundT(BigDecimal lowerBoundT) {
        this.lowerBoundT = lowerBoundT;
    }

    public BigDecimal getUpperBoundT() {
        return upperBoundT;
    }

    public void setUpperBoundT(BigDecimal upperBoundT) {
        this.upperBoundT = upperBoundT;
    }

    public String getCapacityRisk() {
        return capacityRisk;
    }

    public void setCapacityRisk(String capacityRisk) {
        this.capacityRisk = capacityRisk;
    }

    public Boolean getIsActual() {
        return isActual;
    }

    public void setIsActual(Boolean isActual) {
        this.isActual = isActual;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
