package com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.dto.request;

import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint.QcStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a mandatory QC checkpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QcCheckpointMandatoryCreateRequest {

    @NotNull(message = "QC stage is required")
    private QcStage stage;

    @NotBlank(message = "Inspector ID is required")
    private String inspectorId;
}
