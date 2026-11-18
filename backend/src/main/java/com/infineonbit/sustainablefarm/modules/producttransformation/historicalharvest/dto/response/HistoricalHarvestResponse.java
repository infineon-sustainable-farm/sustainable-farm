package com.infineonbit.sustainablefarm.modules.producttransformation.historicalharvest.dto.response;

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
    /** Variety: KEITT, KENT, TOMMY, AMÉLIE, OTHER or another key of the plants variety catalogue. */
    private String mangoVariety;
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
