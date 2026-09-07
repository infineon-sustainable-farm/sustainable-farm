package com.sustainablefarm.modules.producttransformation.resources.equipment.dto.request;

import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment.EquipmentType;
import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment.MaintenanceStatus;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for updating an existing Equipment
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUpdateRequest {

    private String equipmentName;

    private EquipmentType equipmentType;

    @Positive(message = "Capacity must be positive")
    private BigDecimal capacityKgPerHour;

    @Positive(message = "Energy consumption must be positive")
    private BigDecimal energyConsumptionKwhPerKg;

    private String location;

    private MaintenanceStatus maintenanceStatus;

    private LocalDate lastMaintenanceDate;
}
