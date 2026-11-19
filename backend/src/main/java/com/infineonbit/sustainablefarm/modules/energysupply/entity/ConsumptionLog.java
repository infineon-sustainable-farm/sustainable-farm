package com.infineonbit.sustainablefarm.modules.energysupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "consumption_log", indexes = {
        @Index(name = "idx_consumption_log_date", columnList = "date"),
        @Index(name = "idx_consumption_log_load_category", columnList = "load_category")
})
public class ConsumptionLog extends BaseEntity {

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "load_category", nullable = false, length = 20)
    private LoadCategory loadCategory;

    @Column(name = "kwh_consumed", nullable = false, precision = 10, scale = 2)
    private BigDecimal kwhConsumed;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EnergySource source;
}
