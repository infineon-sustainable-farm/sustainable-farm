package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentType;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.MaintenanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @Size(max = 50, message = "Equipment ID must be 50 characters or fewer")
    private String equipmentId;

    @NotBlank(message = "Equipment name is required")
    @Size(max = 100, message = "Equipment name must be 100 characters or fewer")
    private String equipmentName;

    @NotNull(message = "Equipment type is required")
    private EquipmentType equipmentType;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be positive")
    private BigDecimal capacityKgPerHour;

    @NotNull(message = "Energy consumption is required")
    @Positive(message = "Energy consumption must be positive")
    private BigDecimal energyConsumptionKwhPerKg;

    @Size(max = 100, message = "Location must be 100 characters or fewer")
    private String location;

    private MaintenanceStatus maintenanceStatus;

    private LocalDate lastMaintenanceDate;
}
