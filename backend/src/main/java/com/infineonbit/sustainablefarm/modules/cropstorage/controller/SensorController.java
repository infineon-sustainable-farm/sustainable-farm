package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.SensorResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.CropStorageMapper;
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
@RequestMapping("/api/crop-storage/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;
    private final CropStorageMapper mapper;

    @PostMapping
    public SensorResponse create(@Valid @RequestBody SensorCreateRequest request) {
        return mapper.sensor(sensorService.create(request.toEntity()));
    }

    @GetMapping
    public List<SensorResponse> findAll() {
        return CropStorageMapper.all(sensorService.findAll(), mapper::sensor);
    }
}