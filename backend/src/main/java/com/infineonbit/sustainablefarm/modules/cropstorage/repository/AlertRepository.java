package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Explicit bean name: sales-marketing also declares an AlertRepository; two
 * beans named alertRepository would fail merged application runs.
 */
@Repository("cropStorageAlertRepository")
public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "sensor", "sensor.storageZone", "storageZone",
            "batch", "batch.supplier", "batch.storageZone"
    })
    List<Alert> findAll();

    @EntityGraph(attributePaths = {
            "sensor", "sensor.storageZone", "storageZone",
            "batch", "batch.supplier", "batch.storageZone"
    })
    @Query("select a from CropStorageAlert a where a.id = :id")
    Optional<Alert> findWithRelationshipsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "sensor", "sensor.storageZone", "storageZone",
            "batch", "batch.supplier", "batch.storageZone"
    })
    List<Alert> findByStatus(String status);

    boolean existsBySensorIdAndTypeAndStatus(Long sensorId, String type, String status);
}