package com.infineonbit.sustainablefarm.modules.energysupply.repository;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.Alert;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.AlertStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnergyAlertRepository extends JpaRepository<Alert, String> {

    @Override
    @EntityGraph(attributePaths = "component")
    List<Alert> findAll();

    @EntityGraph(attributePaths = "component")
    List<Alert> findByStatus(AlertStatus status);
}
