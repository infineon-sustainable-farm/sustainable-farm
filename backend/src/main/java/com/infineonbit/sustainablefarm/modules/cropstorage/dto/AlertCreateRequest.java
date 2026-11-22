package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record AlertCreateRequest(
        @NotBlank String type,
        String severity,
        String message,
        @NotBlank String status,
        @Valid EntityReferenceRequest sensor,
        @Valid EntityReferenceRequest storageZone,
        @Valid EntityReferenceRequest batch,
        String correctiveAction) {

    public Alert toEntity() {
        Alert alert = new Alert();
        alert.setType(type);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setStatus(status);
        alert.setSensor(sensor == null ? null : sensor.toSensor());
        alert.setStorageZone(storageZone == null ? null : storageZone.toStorageZone());
        alert.setBatch(batch == null ? null : batch.toBatch());
        alert.setCorrectiveAction(correctiveAction);
        return alert;
    }
}
