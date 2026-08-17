package com.sustainablefarm.dto.response;

import com.sustainablefarm.model.Equipment.EquipmentType;
import com.sustainablefarm.model.Equipment.MaintenanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * DTO for Equipment response
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentResponse {

    private String equipmentId;
    private String equipmentName;
    private EquipmentType equipmentType;
    private Double capacityKgPerHour;
    private Double energyConsumptionKwhPerKg;
    private String location;
    private MaintenanceStatus maintenanceStatus;
    private LocalDate lastMaintenanceDate;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
