package com.sustainablefarm.modules.producttransformation.resources.equipment.dto.request;

import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating equipment maintenance status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentMaintenanceStatusRequest {

    @NotNull(message = "Maintenance status is required")
    private MaintenanceStatus maintenanceStatus;
}
