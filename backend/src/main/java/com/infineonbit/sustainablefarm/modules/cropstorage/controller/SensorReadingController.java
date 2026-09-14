package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.SensorReading;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.SensorReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sensor-readings")
@RequiredArgsConstructor
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    @PostMapping
    public SensorReading create(@Valid @RequestBody SensorReading reading) {
        return sensorReadingService.create(reading);
    }

    @GetMapping
    public List<SensorReading> findAll() {
        return sensorReadingService.findAll();
    }

    @GetMapping("/sensor/{sensorId}")
    public List<SensorReading> findBySensor(@PathVariable Long sensorId) {
        return sensorReadingService.findBySensor(sensorId);
    }
}