package com.sustainablefarm.dto.response;

import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * DTO for HistoricalHarvest response (for forecasting)
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalHarvestResponse {

    private Integer year;
    private Integer month;
    private Integer week;
    private MangoVariety mangoVariety;
    private Double harvestQuantityKg;
    private Double qualityGradeAPct;
    private Double qualityGradeBPct;
    private Double qualityGradeCPct;
    private String weatherCondition;
    private Double rainfallMm;
    private Double temperatureAvgC;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
