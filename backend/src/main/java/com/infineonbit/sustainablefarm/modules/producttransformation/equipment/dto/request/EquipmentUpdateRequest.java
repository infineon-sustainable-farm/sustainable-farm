package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentType;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.MaintenanceStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @Size(max = 100, message = "Equipment name must be 100 characters or fewer")
    private String equipmentName;

    private EquipmentType equipmentType;

    @Positive(message = "Capacity must be positive")
    private BigDecimal capacityKgPerHour;

    @Positive(message = "Energy consumption must be positive")
    private BigDecimal energyConsumptionKwhPerKg;

    @Size(max = 100, message = "Location must be 100 characters or fewer")
    private String location;

    private MaintenanceStatus maintenanceStatus;

    private LocalDate lastMaintenanceDate;
}
