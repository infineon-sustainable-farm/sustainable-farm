package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.SensorReadingResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.CropStorageMapper;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.SensorReadingCreateRequest;
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

/**
 * REST endpoints for sensor readings: the monitoring screen reads them and
 * the future IoT push will POST one measure per sensor every hour.
 */
@RestController
@RequestMapping("/api/crop-storage/sensor-readings")
@RequiredArgsConstructor
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;
    private final CropStorageMapper mapper;

    /**
     * Records one measure; an automatic alert may be raised as a side effect
     * when a zone threshold is crossed (see SensorReadingService).
     *
     * @param request measure to save; the nested sensor id must exist
     * @return the saved reading
     */
    @PostMapping
    public SensorReadingResponse create(@Valid @RequestBody SensorReadingCreateRequest request) {
        return mapper.reading(sensorReadingService.create(request.toEntity()));
    }

    /**
     * Returns the full reading history, all sensors mixed.
     *
     * @return every stored reading
     */
    @GetMapping
    public List<SensorReadingResponse> findAll() {
        return CropStorageMapper.all(sensorReadingService.findAll(), mapper::reading);
    }

    /**
     * Returns the history of one sensor, most recent first.
     *
     * @param sensorId sensor to filter on
     * @return readings of that sensor
     */
    @GetMapping("/sensor/{sensorId}")
    public List<SensorReadingResponse> findBySensor(@PathVariable Long sensorId) {
        return CropStorageMapper.all(sensorReadingService.findBySensor(sensorId), mapper::reading);
    }
}