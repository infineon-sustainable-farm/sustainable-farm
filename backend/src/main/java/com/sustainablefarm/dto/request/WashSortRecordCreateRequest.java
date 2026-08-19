package com.sustainablefarm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for creating a new WashSortRecord
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WashSortRecordCreateRequest {

    @NotBlank(message = "Record ID is required")
    private String recordId;

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "Input quantity is required")
    @Positive(message = "Input quantity must be positive")
    private BigDecimal inputQuantityKg;

    @NotNull(message = "Output quantity is required")
    @Positive(message = "Output quantity must be positive")
    private BigDecimal outputQuantityKg;

    @NotNull(message = "Waste quantity is required")
    @PositiveOrZero(message = "Waste quantity cannot be negative")
    private BigDecimal wasteQuantityKg;

    @NotNull(message = "Water usage is required")
    @PositiveOrZero(message = "Water usage cannot be negative")
    private BigDecimal waterUsageLiters;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private String equipmentId;

    private String operatorId;
}
