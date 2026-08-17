package com.sustainablefarm.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for updating an existing DryingRun
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DryingRunUpdateRequest {

    @Positive(message = "Duration must be positive")
    private Double durationHours;

    private Double targetTemperatureC;

    private Double actualTemperatureC;

    @Positive(message = "Start moisture must be positive")
    private Double startMoisturePct;

    @DecimalMin(value = "6.0", message = "End moisture must be at least 6% for EU compliance")
    @DecimalMax(value = "18.0", message = "End moisture must be at most 18% for EU compliance")
    private Double endMoisturePct;

    @PositiveOrZero(message = "Energy usage cannot be negative")
    private Double energyUsageKwh;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String equipmentId;

    private String operatorId;
}
