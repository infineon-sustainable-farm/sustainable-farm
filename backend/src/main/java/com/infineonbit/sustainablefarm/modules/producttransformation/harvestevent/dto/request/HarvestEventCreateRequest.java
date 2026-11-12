package com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.MangoVariety;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.QualityGrade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for creating a new HarvestEvent (from Plants workstream)
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HarvestEventCreateRequest {

    @NotBlank(message = "Harvest ID is required")
    private String harvestId;

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "Harvest date is required")
    private LocalDate harvestDate;

    private LocalTime harvestTime;

    @NotNull(message = "Mango variety is required")
    private MangoVariety mangoVariety;

    @NotBlank(message = "Farm ID is required")
    private String farmId;

    @NotBlank(message = "Block ID is required")
    private String blockId;

    @NotNull(message = "Harvest quantity is required")
    @Positive(message = "Harvest quantity must be positive")
    private BigDecimal harvestQuantityKg;

    @NotNull(message = "Quality grade is required")
    private QualityGrade qualityGrade;

    private String qualityGradeDescription;

    private String harvestTeamId;

    private String harvestSupervisor;

    private String weatherConditions;

    private String storageLocation;

    /**
     * Plants lot number this harvest belongs to (for example {@code LOT-2026-A-001}).
     * Optional: a harvest created outside an import may not have one yet.
     */
    private String lotNumber;

    /**
     * Identifier of the Plants harvest this event comes from, when it was imported.
     * Kept so the event can be traced back to the farm harvest it was created from.
     */
    private Long plantsHarvestId;
}
