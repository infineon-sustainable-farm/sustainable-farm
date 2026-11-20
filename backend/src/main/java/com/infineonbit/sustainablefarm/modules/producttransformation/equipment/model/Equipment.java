package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * A processing or drying machine as product transformation sees it: the equipment of the farm
 * register owned by machinery ({@code modules.machinery.api}) joined with its product
 * transformation attributes ({@link EquipmentSpec}). Not an entity: there is a single
 * {@code equipment} table (ADR 0004).
 *
 * <p>{@code equipmentId} is the business code of the register, {@code id} its Long id, the
 * value stored in the {@code equipment_id} columns of the product transformation tables.
 * {@code maintenanceStatus} is the register status read in product transformation terms. A
 * machine registered from the machinery screens has no attributes yet: type, capacity, energy,
 * location and last maintenance date are {@code null} until it is edited here.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {

    private Long id;
    private String equipmentId;
    private String equipmentName;
    private EquipmentType equipmentType;
    private BigDecimal capacityKgPerHour;
    private BigDecimal energyConsumptionKwhPerKg;
    private String location;
    private MaintenanceStatus maintenanceStatus;
    private LocalDate lastMaintenanceDate;
    private Timestamp createdAt;
    private Timestamp updatedAt;


}
