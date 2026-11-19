package com.infineonbit.sustainablefarm.modules.energysupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "solar_generation_log", indexes = {
        @Index(name = "idx_solar_generation_log_component_id", columnList = "component_id"),
        @Index(name = "idx_solar_generation_log_date", columnList = "date")
})
public class SolarGenerationLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_solar_generation_log_component"))
    private EnergyComponent component;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "kwh_produced", nullable = false, precision = 10, scale = 2)
    private BigDecimal kwhProduced;

    @Column(name = "efficiency_pct", precision = 5, scale = 2)
    private BigDecimal efficiencyPct;
}
