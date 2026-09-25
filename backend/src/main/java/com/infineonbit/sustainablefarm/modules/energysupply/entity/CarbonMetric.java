package com.infineonbit.sustainablefarm.modules.energysupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "carbon_metric")
public class CarbonMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "metric_id")
    private String metricId;

    @Column(nullable = false)
    private LocalDate period;

    @Column(name = "co2_avoided_kg", precision = 10, scale = 2)
    private BigDecimal co2AvoidedKg;

    @Column(name = "emission_factor_used", precision = 6, scale = 3)
    private BigDecimal emissionFactorUsed;

    private String scenario;
}
