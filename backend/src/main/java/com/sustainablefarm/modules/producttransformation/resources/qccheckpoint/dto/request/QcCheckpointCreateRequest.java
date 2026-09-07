package com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.dto.request;

import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint.QcResult;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint.QcStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for creating a new QcCheckpoint
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QcCheckpointCreateRequest {

    @NotBlank(message = "Checkpoint ID is required")
    private String checkpointId;

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "QC stage is required")
    private QcStage stage;

    @NotNull(message = "QC result is required")
    private QcResult result;

    private String defects;

    @NotNull(message = "Defects count is required")
    @PositiveOrZero(message = "Defects count cannot be negative")
    private Integer defectsCount;

    private String inspectorId;

    @NotNull(message = "Checkpoint time is required")
    private LocalDateTime checkpointTime;

    private String notes;
}
