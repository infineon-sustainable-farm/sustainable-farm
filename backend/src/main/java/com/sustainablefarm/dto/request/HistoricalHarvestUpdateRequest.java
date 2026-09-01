package com.sustainablefarm.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing HistoricalHarvest record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalHarvestUpdateRequest {

    @Positive(message = "Harvest quantity must be positive")
    private Double harvestQuantityKg;

    @Min(value = 0, message = "Grade A percentage must be between 0 and 100")
    @Max(value = 100, message = "Grade A percentage must be between 0 and 100")
    private Double qualityGradeAPct;

    @Min(value = 0, message = "Grade B percentage must be between 0 and 100")
    @Max(value = 100, message = "Grade B percentage must be between 0 and 100")
    private Double qualityGradeBPct;

    @Min(value = 0, message = "Grade C percentage must be between 0 and 100")
    @Max(value = 100, message = "Grade C percentage must be between 0 and 100")
    private Double qualityGradeCPct;

    private String weatherCondition;
    private Double rainfallMm;
    private Double temperatureAvgC;
}
