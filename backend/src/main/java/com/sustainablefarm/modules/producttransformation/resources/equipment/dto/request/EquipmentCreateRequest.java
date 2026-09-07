package com.sustainablefarm.modules.producttransformation.resources.equipment.dto.request;

import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment.EquipmentType;
import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment.MaintenanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a new Equipment
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentCreateRequest {

    @NotBlank(message = "Equipment ID is required")
    private String equipmentId;

    @NotBlank(message = "Equipment name is required")
    private String equipmentName;

    @NotNull(message = "Equipment type is required")
    private EquipmentType equipmentType;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be positive")
    private BigDecimal capacityKgPerHour;

    @NotNull(message = "Energy consumption is required")
    @Positive(message = "Energy consumption must be positive")
    private BigDecimal energyConsumptionKwhPerKg;

    private String location;

    private MaintenanceStatus maintenanceStatus;

    private LocalDate lastMaintenanceDate;
}
