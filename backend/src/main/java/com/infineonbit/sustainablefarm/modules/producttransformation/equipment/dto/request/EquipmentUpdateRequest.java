package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for updating the product transformation attributes of a machine ({@code pt_equipment_spec}).
 *
 * <p>The machine itself (code, name, stage, status) belongs to machinery, which alone creates,
 * renames, changes the status of and deletes it: those fields are not part of this request, and
 * a name or a status sent anyway is ignored. A {@code null} field is left unchanged.</p>
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUpdateRequest {

    private EquipmentType equipmentType;

    @Positive(message = "Capacity must be positive")
    private BigDecimal capacityKgPerHour;

    @Positive(message = "Energy consumption must be positive")
    private BigDecimal energyConsumptionKwhPerKg;

    @Size(max = 100, message = "Location must be 100 characters or fewer")
    private String location;

    private LocalDate lastMaintenanceDate;
}
