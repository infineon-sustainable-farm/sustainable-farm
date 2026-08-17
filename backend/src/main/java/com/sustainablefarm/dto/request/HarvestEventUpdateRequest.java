package com.sustainablefarm.dto.request;

import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.model.HarvestEvent.QualityGrade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for updating an existing HarvestEvent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HarvestEventUpdateRequest {

    private LocalDate harvestDate;
    private LocalTime harvestTime;
    private MangoVariety mangoVariety;
    private String farmId;
    private String blockId;

    @Positive(message = "Harvest quantity must be positive")
    private Double harvestQuantityKg;

    private QualityGrade qualityGrade;
    private String qualityGradeDescription;
    private String harvestTeamId;
    private String harvestSupervisor;
    private String weatherConditions;
    private String storageLocation;
}
