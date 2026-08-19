package com.sustainablefarm.dto.request;

import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating a new HistoricalHarvest (for forecasting)
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalHarvestCreateRequest {

    @NotNull(message = "Year is required")
    private Integer year;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "Week is required")
    @Min(value = 1, message = "Week must be between 1 and 53")
    @Max(value = 53, message = "Week must be between 1 and 53")
    private Integer week;

    @NotNull(message = "Mango variety is required")
    private MangoVariety mangoVariety;

    @NotNull(message = "Harvest quantity is required")
    @Positive(message = "Harvest quantity must be positive")
    private BigDecimal harvestQuantityKg;

    @Min(value = 0, message = "Grade A percentage must be between 0 and 100")
    @Max(value = 100, message = "Grade A percentage must be between 0 and 100")
    private BigDecimal qualityGradeAPct;

    @Min(value = 0, message = "Grade B percentage must be between 0 and 100")
    @Max(value = 100, message = "Grade B percentage must be between 0 and 100")
    private BigDecimal qualityGradeBPct;

    @Min(value = 0, message = "Grade C percentage must be between 0 and 100")
    @Max(value = 100, message = "Grade C percentage must be between 0 and 100")
    private BigDecimal qualityGradeCPct;

    private String weatherCondition;

    private BigDecimal rainfallMm;

    private BigDecimal temperatureAvgC;
}
