package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentType;
import jakarta.persistence.Column;
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
 * The product transformation attributes of a processing or drying machine: a 1:1 extension of
 * the {@code equipment} register owned by machinery (ADR 0004).
 *
 * <p>Its foreign key to {@code equipment} is declared in
 * {@code ProductTransformationEquipmentForeignKeys}: deleting the equipment deletes its
 * attributes ({@code on delete cascade}).</p>
 */
@Entity
@Table(name = "pt_equipment_spec", uniqueConstraints =
        @UniqueConstraint(name = "pt_equipment_spec_equipment_id_key", columnNames = "equipment_id"))
@Getter
@Setter
@NoArgsConstructor
public class EquipmentSpec extends BaseEntity {

    /** The Long id of the equipment in the register. */
    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", nullable = false, length = 50)
    private EquipmentType equipmentType;

    @Column(name = "capacity_kg_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacityKgPerHour;

    @Column(name = "energy_consumption_kwh_per_kg", nullable = false, precision = 10, scale = 4)
    private BigDecimal energyConsumptionKwhPerKg;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;
}
