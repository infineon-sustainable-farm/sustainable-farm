package com.infineonbit.sustainablefarm.modules.energysupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "energy_component")
public class EnergyComponent {

    @Id
    @Column(name = "component_id", length = 20)
    private String componentId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentCategory category;

    @Column(name = "capacity_value", precision = 10, scale = 2)
    private BigDecimal capacityValue;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "unit_cost_eur", precision = 10, scale = 2)
    private BigDecimal unitCostEur;

    @Enumerated(EnumType.STRING)
    private ComponentStatus status;

    private String location;
}
