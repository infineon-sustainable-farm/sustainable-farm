package com.sustainablefarm.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for scheduling equipment maintenance.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentScheduleMaintenanceRequest {

    @NotNull(message = "Maintenance date is required")
    private LocalDate maintenanceDate;
}
