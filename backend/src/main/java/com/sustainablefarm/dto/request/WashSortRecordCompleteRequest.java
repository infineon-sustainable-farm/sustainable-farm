package com.sustainablefarm.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for completing a WashSortRecord with final quantities
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WashSortRecordCompleteRequest {

    @NotNull(message = "Output quantity is required")
    @PositiveOrZero(message = "Output quantity cannot be negative")
    private Double outputQuantityKg;

    @NotNull(message = "Waste quantity is required")
    @PositiveOrZero(message = "Waste quantity cannot be negative")
    private Double wasteQuantityKg;
}
