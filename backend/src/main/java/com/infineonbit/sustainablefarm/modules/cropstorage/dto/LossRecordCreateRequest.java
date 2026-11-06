package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.LossRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LossRecordCreateRequest(
        @NotNull @Valid EntityReferenceRequest batch,
        @NotNull @PositiveOrZero BigDecimal quantityKg,
        String cause,
        @PositiveOrZero BigDecimal valueFcfa,
        @NotNull LocalDateTime recordedAt) {

    public LossRecord toEntity() {
        LossRecord record = new LossRecord();
        record.setBatch(batch == null ? null : batch.toBatch());
        record.setQuantityKg(quantityKg);
        record.setCause(cause);
        record.setValueFcfa(valueFcfa);
        record.setRecordedAt(recordedAt);
        return record;
    }
}
