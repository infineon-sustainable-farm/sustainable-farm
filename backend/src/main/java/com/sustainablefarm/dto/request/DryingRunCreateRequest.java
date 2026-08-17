package com.sustainablefarm.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for creating a new DryingRun
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DryingRunCreateRequest {

    @NotBlank(message = "Run ID is required")
    private String runId;

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Double durationHours;

    @NotNull(message = "Target temperature is required")
    private Double targetTemperatureC;

    @NotNull(message = "Actual temperature is required")
    private Double actualTemperatureC;

    @NotNull(message = "Start moisture is required")
    @Positive(message = "Start moisture must be positive")
    private Double startMoisturePct;

    @NotNull(message = "End moisture is required")
    @DecimalMin(value = "6.0", message = "End moisture must be at least 6% for EU compliance")
    @DecimalMax(value = "18.0", message = "End moisture must be at most 18% for EU compliance")
    private Double endMoisturePct;

    @NotNull(message = "Energy usage is required")
    @PositiveOrZero(message = "Energy usage cannot be negative")
    private Double energyUsageKwh;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private String equipmentId;

    private String operatorId;
}
