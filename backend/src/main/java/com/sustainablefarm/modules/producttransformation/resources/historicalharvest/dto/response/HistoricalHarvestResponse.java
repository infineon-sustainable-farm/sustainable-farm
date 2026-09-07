package com.sustainablefarm.modules.producttransformation.resources.historicalharvest.dto.response;

import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent.MangoVariety;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private BigDecimal harvestQuantityKg;
    private BigDecimal qualityGradeAPct;
    private BigDecimal qualityGradeBPct;
    private BigDecimal qualityGradeCPct;
    private String weatherCondition;
    private BigDecimal rainfallMm;
    private BigDecimal temperatureAvgC;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
