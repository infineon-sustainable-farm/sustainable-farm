package com.infineonbit.sustainablefarm.modules.energysupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "carbon_metric", indexes = @Index(name = "idx_carbon_metric_period", columnList = "period"))
public class CarbonMetric extends BaseEntity {

    @Column(nullable = false)
    private LocalDate period;

    @Column(name = "co2_avoided_kg", precision = 10, scale = 2)
    private BigDecimal co2AvoidedKg;

    @Column(name = "emission_factor_used", precision = 6, scale = 3)
    private BigDecimal emissionFactorUsed;

    @Column(length = 100)
    private String scenario;
}
