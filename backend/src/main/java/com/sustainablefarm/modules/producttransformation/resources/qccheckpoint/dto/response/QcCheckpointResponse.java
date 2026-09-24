package com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.dto.response;

import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint.QcResult;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint.QcStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * DTO for QcCheckpoint response
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcCheckpointResponse {

    private String checkpointId;
    private String batchId;
    private QcStage stage;
    private QcResult result;
    private String defects;
    private Integer defectsCount;
    private String inspectorId;
    private LocalDateTime checkpointTime;
    private String notes;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Computed fields
    private Boolean mandatory;
    private Boolean passed;
}
