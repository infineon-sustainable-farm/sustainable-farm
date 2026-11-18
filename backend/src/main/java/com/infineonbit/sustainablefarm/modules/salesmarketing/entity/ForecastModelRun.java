package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

/**
 * A run of the demand forecast model. Its status is the free label the interface sends
 * ({@code Cold-Start Forecast}).
 */
@Entity
@Table(name = "forecast_model_runs")
@Getter
@Setter
public class ForecastModelRun extends BaseEntity {

    @Column(name = "crop_variety", length = 60)
    private String cropVariety;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "confidence_interval_pct", precision = 5, scale = 2)
    private BigDecimal confidenceIntervalPct;

    @ColumnDefault("false")
    @Column(name = "use_weather_data")
    private Boolean useWeatherData;

    @ColumnDefault("false")
    @Column(name = "use_commodity_prices")
    private Boolean useCommodityPrices;

    @ColumnDefault("false")
    @Column(name = "use_geopolitical_index")
    private Boolean useGeopoliticalIndex;

    @ColumnDefault("'cold_start'")
    @Column(name = "model_status", length = 30)
    private String modelStatus;
}
