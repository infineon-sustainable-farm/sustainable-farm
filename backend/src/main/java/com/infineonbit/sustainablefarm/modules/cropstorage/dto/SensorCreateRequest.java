package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.OperationalStatus;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Sensor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SensorCreateRequest(
        @NotBlank String sensorCode,
        String sensorType,
        String name,
        @Valid EntityReferenceRequest storageZone,
        @NotNull OperationalStatus status,
        LocalDate installationDate) {

    public Sensor toEntity() {
        Sensor sensor = new Sensor();
        sensor.setSensorCode(sensorCode);
        sensor.setSensorType(sensorType);
        sensor.setName(name);
        sensor.setStorageZone(storageZone == null ? null : storageZone.toStorageZone());
        sensor.setStatus(status);
        sensor.setInstallationDate(installationDate);
        return sensor;
    }
}
