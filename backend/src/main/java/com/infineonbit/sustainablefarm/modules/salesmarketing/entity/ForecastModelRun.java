package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps to the "forecast_model_runs" table.
 * Foreign keys are kept as plain Integer id fields.
 */
@Entity
@Table(name = "forecast_model_runs")
public class ForecastModelRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Integer runId;

    @Column(name = "crop_variety")
    private String cropVariety;

    @Column(name = "region")
    private String region;

    @Column(name = "confidence_interval_pct")
    private BigDecimal confidenceIntervalPct;

    @Column(name = "use_weather_data")
    private Boolean useWeatherData;

    @Column(name = "use_commodity_prices")
    private Boolean useCommodityPrices;

    @Column(name = "use_geopolitical_index")
    private Boolean useGeopoliticalIndex;

    @Column(name = "model_status")
    private String modelStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getRunId() {
        return runId;
    }

    public void setRunId(Integer runId) {
        this.runId = runId;
    }

    public String getCropVariety() {
        return cropVariety;
    }

    public void setCropVariety(String cropVariety) {
        this.cropVariety = cropVariety;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public BigDecimal getConfidenceIntervalPct() {
        return confidenceIntervalPct;
    }

    public void setConfidenceIntervalPct(BigDecimal confidenceIntervalPct) {
        this.confidenceIntervalPct = confidenceIntervalPct;
    }

    public Boolean getUseWeatherData() {
        return useWeatherData;
    }

    public void setUseWeatherData(Boolean useWeatherData) {
        this.useWeatherData = useWeatherData;
    }

    public Boolean getUseCommodityPrices() {
        return useCommodityPrices;
    }

    public void setUseCommodityPrices(Boolean useCommodityPrices) {
        this.useCommodityPrices = useCommodityPrices;
    }

    public Boolean getUseGeopoliticalIndex() {
        return useGeopoliticalIndex;
    }

    public void setUseGeopoliticalIndex(Boolean useGeopoliticalIndex) {
        this.useGeopoliticalIndex = useGeopoliticalIndex;
    }

    public String getModelStatus() {
        return modelStatus;
    }

    public void setModelStatus(String modelStatus) {
        this.modelStatus = modelStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
