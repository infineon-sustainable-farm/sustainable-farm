package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record StorageZoneCreateRequest(
        @NotBlank String zoneCode,
        String name,
        String description,
        @NotNull @Positive BigDecimal capacityKg,
        BigDecimal temperatureMinCelsius,
        BigDecimal temperatureMaxCelsius,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal humidityMinPercent,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal humidityMaxPercent,
        Boolean coolingAvailable,
        @NotBlank String status) {

    public StorageZone toEntity() {
        StorageZone zone = new StorageZone();
        zone.setZoneCode(zoneCode);
        zone.setName(name);
        zone.setDescription(description);
        zone.setCapacityKg(capacityKg);
        zone.setTemperatureMinCelsius(temperatureMinCelsius);
        zone.setTemperatureMaxCelsius(temperatureMaxCelsius);
        zone.setHumidityMinPercent(humidityMinPercent);
        zone.setHumidityMaxPercent(humidityMaxPercent);
        zone.setCoolingAvailable(coolingAvailable);
        zone.setStatus(status);
        return zone;
    }
}
