package com.sustainablefarm.dto.request;

import com.sustainablefarm.model.QcCheckpoint.QcResult;
import com.sustainablefarm.model.QcCheckpoint.QcStage;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for updating an existing QcCheckpoint
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QcCheckpointUpdateRequest {

    private QcStage stage;

    private QcResult result;

    private String defects;

    @PositiveOrZero(message = "Defects count cannot be negative")
    private Integer defectsCount;

    private String inspectorId;

    private LocalDateTime checkpointTime;

    private String notes;
}
