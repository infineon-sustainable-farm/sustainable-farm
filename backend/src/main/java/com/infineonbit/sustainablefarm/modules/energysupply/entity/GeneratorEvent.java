package com.infineonbit.sustainablefarm.modules.energysupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
@Table(name = "generator_event", indexes = @Index(name = "idx_generator_event_date", columnList = "date"))
public class GeneratorEvent extends BaseEntity {

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "trigger_reason", length = 500)
    private String triggerReason;

    @Column(name = "duration_hours", precision = 6, scale = 2)
    private BigDecimal durationHours;

    @Column(name = "fuel_liters", precision = 8, scale = 2)
    private BigDecimal fuelLiters;

    /** Cost of the run, in euros. */
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "cost_amount", precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "cost_currency", length = 3))
    private Money cost;
}
