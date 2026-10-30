package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Explicit bean name: sales-marketing also declares an AlertRepository; two
 * beans named alertRepository would fail merged application runs.
 */
@Repository("cropStorageAlertRepository")
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByStatus(String status);

    boolean existsBySensorIdAndTypeAndStatus(Long sensorId, String type, String status);
}