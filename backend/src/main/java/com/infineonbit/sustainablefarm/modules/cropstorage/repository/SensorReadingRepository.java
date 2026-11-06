package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.SensorReading;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    @Override
    @EntityGraph(attributePaths = {"sensor", "sensor.storageZone"})
    List<SensorReading> findAll();

    @EntityGraph(attributePaths = {"sensor", "sensor.storageZone"})
    List<SensorReading> findBySensorIdOrderByMeasuredAtDesc(Long sensorId);
}