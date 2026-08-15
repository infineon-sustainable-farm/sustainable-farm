package com.sustainablefarm.repository;

import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.model.Equipment.EquipmentType;
import com.sustainablefarm.model.Equipment.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Equipment entity
 * Machinery and equipment data - Supporting Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, String> {

    /**
     * Find equipment by type
     */
    List<Equipment> findByEquipmentType(EquipmentType equipmentType);

    /**
     * Find equipment by maintenance status
     */
    List<Equipment> findByMaintenanceStatus(MaintenanceStatus maintenanceStatus);

    /**
     * Find active equipment
     */
    List<Equipment> findByMaintenanceStatusOrderByEquipmentName(MaintenanceStatus maintenanceStatus);

    /**
     * Find equipment by location
     */
    List<Equipment> findByLocation(String location);

    /**
     * Find available equipment for specific type
     * Business Rule: Equipment must be ACTIVE for assignment
     */
    @Query("SELECT e FROM Equipment e WHERE e.equipmentType = :type AND e.maintenanceStatus = 'ACTIVE'")
    List<Equipment> findAvailableByType(@Param("type") EquipmentType type);

    /**
     * Find equipment requiring maintenance
     */
    @Query("SELECT e FROM Equipment e WHERE e.maintenanceStatus = 'MAINTENANCE' OR e.lastMaintenanceDate < :date")
    List<Equipment> findEquipmentNeedingMaintenance(@Param("date") java.time.LocalDate date);

    /**
     * Check if equipment is available
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Equipment e WHERE e.equipmentId = :id AND e.maintenanceStatus = 'ACTIVE'")
    boolean isEquipmentAvailable(@Param("id") String equipmentId);
}