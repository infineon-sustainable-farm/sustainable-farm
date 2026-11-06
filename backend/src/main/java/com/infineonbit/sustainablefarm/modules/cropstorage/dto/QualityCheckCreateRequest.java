package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.QualityCheck;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QualityCheckCreateRequest(
        @NotNull @Valid EntityReferenceRequest batch,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal moisturePercent,
        String qualityGrade,
        @NotNull Boolean passed,
        String inspector,
        String notes,
        @NotNull LocalDateTime checkedAt) {

    public QualityCheck toEntity() {
        QualityCheck check = new QualityCheck();
        check.setBatch(batch == null ? null : batch.toBatch());
        check.setMoisturePercent(moisturePercent);
        check.setQualityGrade(qualityGrade);
        check.setPassed(passed);
        check.setInspector(inspector);
        check.setNotes(notes);
        check.setCheckedAt(checkedAt);
        return check;
    }
}
