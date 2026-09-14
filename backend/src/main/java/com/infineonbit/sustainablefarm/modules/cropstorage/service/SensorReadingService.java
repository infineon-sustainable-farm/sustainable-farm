package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.SensorReading;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;

    public SensorReading create(SensorReading reading) {
        return sensorReadingRepository.save(reading);
    }

    public List<SensorReading> findAll() {
        return sensorReadingRepository.findAll();
    }

    public List<SensorReading> findBySensor(Long sensorId) {
        return sensorReadingRepository.findBySensorIdOrderByMeasuredAtDesc(sensorId);
    }
}