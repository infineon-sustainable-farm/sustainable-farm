package com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.QualityGrade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO for creating a new HarvestEvent from a harvest recorded in plants.
 *
 * <p>Plants owns the harvest lots: the event names one by {@code plantsHarvestId} (the id of
 * {@code GET /api/plants/harvests} or {@code GET /api/product-transformation/harvest-events/plants/all}),
 * and its date, variety, block (with its farm) and quantity are copied from it, read-only. An
 * unknown harvest is refused with 400. Product transformation keeps its own fields: the event and
 * batch codes, the time, the quality grade, the team, the supervisor, the weather and the storage.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HarvestEventCreateRequest {

    @NotBlank(message = "Harvest ID is required")
    private String harvestId;

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    /** The plants harvest (lot) the event comes from: {@code harvest_record.id}. */
    @NotNull(message = "Plants harvest ID is required")
    private Long plantsHarvestId;

    private LocalTime harvestTime;

    @NotNull(message = "Quality grade is required")
    private QualityGrade qualityGrade;

    private String qualityGradeDescription;

    private String harvestTeamId;

    /** Person of the people directory (GET /api/staff/persons) who supervised the harvest, or {@code null}. */
    private Long harvestSupervisorPersonId;

    private String weatherConditions;

    private String storageLocation;
}
