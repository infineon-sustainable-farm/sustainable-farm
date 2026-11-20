package com.infineonbit.sustainablefarm.modules.energysupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A component of the energy system. Its key is the Long id of {@link BaseEntity}; the business
 * code (ENG-001...) chosen by the user is unique and is the {@code componentId} of the API.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "energy_component", uniqueConstraints =
        @UniqueConstraint(name = "uk_energy_component_code", columnNames = "code"))
public class EnergyComponent extends BaseEntity {

    /** Business code assigned by the user (ENG-001...), validated by EnergyComponentRequest. */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComponentCategory category;

    @Column(name = "capacity_value", precision = 10, scale = 2)
    private BigDecimal capacityValue;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    /** Unit cost, in euros. */
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_cost_amount", precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "unit_cost_currency", length = 3))
    private Money unitCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComponentStatus status;

    private String location;
}
