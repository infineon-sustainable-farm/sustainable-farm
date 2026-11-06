package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.SensorReading;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SensorReadingCreateRequest(
        @Valid EntityReferenceRequest sensor,
        @DecimalMin("-50.0") @DecimalMax("100.0") BigDecimal temperature,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal humidity,
        @NotNull LocalDateTime measuredAt) {

    public SensorReading toEntity() {
        SensorReading reading = new SensorReading();
        reading.setSensor(sensor == null ? null : sensor.toSensor());
        reading.setTemperature(temperature);
        reading.setHumidity(humidity);
        reading.setMeasuredAt(measuredAt);
        return reading;
    }
}
