package com.sustainablefarm.modules.producttransformation.resources.washsortrecord.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for updating an existing WashSortRecord
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WashSortRecordUpdateRequest {

    @Positive(message = "Input quantity must be positive")
    private BigDecimal inputQuantityKg;

    @Positive(message = "Output quantity must be positive")
    private BigDecimal outputQuantityKg;

    @PositiveOrZero(message = "Waste quantity cannot be negative")
    private BigDecimal wasteQuantityKg;

    @PositiveOrZero(message = "Water usage cannot be negative")
    private BigDecimal waterUsageLiters;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String equipmentId;

    private String operatorId;
}
