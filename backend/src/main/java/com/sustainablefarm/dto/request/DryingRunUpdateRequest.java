package com.sustainablefarm.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private BigDecimal durationHours;

    private BigDecimal targetTemperatureC;

    private BigDecimal actualTemperatureC;

    @Positive(message = "Start moisture must be positive")
    private BigDecimal startMoisturePct;

    @DecimalMin(value = "6.0", message = "End moisture must be at least 6% for EU compliance")
    @DecimalMax(value = "18.0", message = "End moisture must be at most 18% for EU compliance")
    private BigDecimal endMoisturePct;

    @PositiveOrZero(message = "Energy usage cannot be negative")
    private BigDecimal energyUsageKwh;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String equipmentId;

    private String operatorId;
}
