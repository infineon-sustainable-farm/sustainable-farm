package com.infineonbit.sustainablefarm.modules.energysupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "solar_generation_log")
public class SolarGenerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private String logId;

    @ManyToOne
    @JoinColumn(name = "component_id", nullable = false)
    private EnergyComponent component;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "kwh_produced", precision = 10, scale = 2)
    private BigDecimal kwhProduced;

    @Column(name = "efficiency_pct", precision = 5, scale = 2)
    private BigDecimal efficiencyPct;
}
