package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

/**
 * Maps to the "demand_forecasts" table.
 * Foreign keys are kept as plain Integer id fields.
 */
@Entity
@Table(name = "demand_forecasts", indexes = @Index(name = "idx_forecasts_month", columnList = "forecast_month, scenario"))
@Check(name = "demand_forecasts_capacity_risk_check", constraints = "capacity_risk IN ('low', 'medium', 'high')")
@Check(name = "demand_forecasts_scenario_check", constraints = "scenario IN ('pessimistic', 'base', 'optimistic')")
public class DemandForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forecast_id")
    private Integer forecastId;

    @Column(name = "product_id")
    private Integer productId;

    @NotNull(message = "forecastMonth is required")
    @Column(name = "forecast_month", nullable = false)
    private LocalDate forecastMonth;

    @NotBlank(message = "scenario is required")
    @ColumnDefault("'base'")
    @Column(name = "scenario", nullable = false, length = 20)
    private String scenario;

    @NotNull(message = "forecastedVolumeT is required")
    @PositiveOrZero
    @Column(name = "forecasted_volume_t", nullable = false, precision = 10, scale = 2)
    private BigDecimal forecastedVolumeT;

    @Column(name = "lower_bound_t", precision = 10, scale = 2)
    private BigDecimal lowerBoundT;

    @Column(name = "upper_bound_t", precision = 10, scale = 2)
    private BigDecimal upperBoundT;

    @Column(name = "capacity_risk", length = 10)
    private String capacityRisk;

    @ColumnDefault("false")
    @Column(name = "is_actual")
    private Boolean isActual;

    @Column(name = "model_version", length = 30)
    private String modelVersion;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
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
