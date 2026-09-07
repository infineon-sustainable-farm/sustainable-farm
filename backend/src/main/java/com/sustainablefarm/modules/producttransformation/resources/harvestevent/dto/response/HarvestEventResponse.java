package com.sustainablefarm.modules.producttransformation.resources.harvestevent.dto.response;

import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent.QualityGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for HarvestEvent response (from Plants workstream)
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarvestEventResponse {

    private String harvestId;
    private String batchId;
    private LocalDate harvestDate;
    private LocalTime harvestTime;
    private MangoVariety mangoVariety;
    private String farmId;
    private String blockId;
    private BigDecimal harvestQuantityKg;
    private QualityGrade qualityGrade;
    private String qualityGradeDescription;
    private String harvestTeamId;
    private String harvestSupervisor;
    private String weatherConditions;
    private String storageLocation;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
