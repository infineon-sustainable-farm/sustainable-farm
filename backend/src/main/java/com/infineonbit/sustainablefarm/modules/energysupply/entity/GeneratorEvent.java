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

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "generator_event")
public class GeneratorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id")
    private String eventId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "trigger_reason", length = 500)
    private String triggerReason;

    @Column(name = "duration_hours", precision = 6, scale = 2)
    private BigDecimal durationHours;

    @Column(name = "fuel_liters", precision = 8, scale = 2)
    private BigDecimal fuelLiters;

    @Column(name = "cost_eur", precision = 10, scale = 2)
    private BigDecimal costEur;

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setTriggerReason(String triggerReason) {
        this.triggerReason = triggerReason;
    }

    public void setDurationHours(BigDecimal durationHours) {
        this.durationHours = durationHours;
    }

    public void setFuelLiters(BigDecimal fuelLiters) {
        this.fuelLiters = fuelLiters;
    }

    public void setCostEur(BigDecimal costEur) {
        this.costEur = costEur;
    }
}
