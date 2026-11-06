package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Sensor;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.SensorCreateRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.SensorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;

    @PostMapping
    public Sensor create(@Valid @RequestBody SensorCreateRequest request) {
        return sensorService.create(request.toEntity());
    }

    @GetMapping
    public List<Sensor> findAll() {
        return sensorService.findAll();
    }
}