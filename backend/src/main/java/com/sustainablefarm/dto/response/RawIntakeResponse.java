package com.sustainablefarm.dto.response;

import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.model.HarvestEvent.QualityGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * DTO for RawIntake response
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawIntakeResponse {

    private String intakeId;
    private String batchId;
    private String sourceFarm;
    private String sourceBlock;
    private LocalDate intakeDate;
    private BigDecimal receivedQuantityKg;
    private MangoVariety receivedVariety;
    private QualityGrade receivedGrade;
    private String intakeOperator;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
