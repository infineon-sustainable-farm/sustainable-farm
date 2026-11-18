package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.math.BigDecimal;
import java.time.LocalDate;

/** The demand forecast of a month for a product and a scenario. */
@Entity
@Table(name = "demand_forecasts", indexes = @Index(name = "idx_forecasts_month", columnList = "forecast_month, scenario"))
@Getter
@Setter
public class DemandForecast extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "demand_forecasts_product_id_fkey"))
    private Product product;

    @Column(name = "forecast_month", nullable = false)
    private LocalDate forecastMonth;

    @ColumnDefault("'base'")
    @Convert(converter = ForecastScenario.Converter.class)
    @Column(name = "scenario", nullable = false, length = 20)
    private ForecastScenario scenario;

    @Column(name = "forecasted_volume_t", nullable = false, precision = 10, scale = 2)
    private BigDecimal forecastedVolumeT;

    @Column(name = "lower_bound_t", precision = 10, scale = 2)
    private BigDecimal lowerBoundT;

    @Column(name = "upper_bound_t", precision = 10, scale = 2)
    private BigDecimal upperBoundT;

    @Convert(converter = CapacityRisk.Converter.class)
    @Column(name = "capacity_risk", length = 10)
    private CapacityRisk capacityRisk;

    /** {@code false} when none is sent. */
    @ColumnDefault("false")
    @Column(name = "is_actual", nullable = false)
    private Boolean isActual;

    @Column(name = "model_version", length = 30)
    private String modelVersion;
}
