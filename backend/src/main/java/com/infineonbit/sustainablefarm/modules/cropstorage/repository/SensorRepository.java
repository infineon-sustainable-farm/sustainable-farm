package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Sensor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    @Override
    @EntityGraph(attributePaths = "storageZone")
    List<Sensor> findAll();

    @EntityGraph(attributePaths = "storageZone")
    @Query("select s from Sensor s where s.id = :id")
    Optional<Sensor> findWithStorageZoneById(@Param("id") Long id);
}