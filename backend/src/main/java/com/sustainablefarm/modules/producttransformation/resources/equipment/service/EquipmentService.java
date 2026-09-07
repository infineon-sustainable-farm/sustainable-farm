package com.sustainablefarm.modules.producttransformation.resources.equipment.service;

import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment;
import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment.EquipmentType;
import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment.MaintenanceStatus;

import java.util.List;

/**
 * Service Interface for Equipment Entity
 * Machinery and equipment data - Supporting Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface EquipmentService {

    /**
     * Create new equipment
     */
    Equipment createEquipment(Equipment equipment);

    /**
     * Update equipment
     */
    Equipment updateEquipment(String equipmentId, Equipment equipment);

    /**
     * Delete equipment
     */
    void deleteEquipment(String equipmentId);

    /**
     * Get equipment by ID
     */
    Equipment getEquipmentById(String equipmentId);

    /**
     * Get all equipment
     */
    List<Equipment> getAllEquipment();

    /**
     * Get equipment by type
     */
    List<Equipment> getEquipmentByType(EquipmentType type);

    /**
     * Get equipment by maintenance status
     */
    List<Equipment> getEquipmentByStatus(MaintenanceStatus status);

    /**
     * Get available equipment for specific type
     * Business Rule: Equipment must be ACTIVE for assignment
     */
    List<Equipment> getAvailableEquipmentByType(EquipmentType type);

    /**
     * Check if equipment is available
     */
    boolean isEquipmentAvailable(String equipmentId);

    /**
     * Get equipment requiring maintenance
     */
    List<Equipment> getEquipmentNeedingMaintenance(java.time.LocalDate date);

    /**
     * Update equipment maintenance status
     */
    Equipment updateMaintenanceStatus(String equipmentId, MaintenanceStatus status);

    /**
     * Schedule maintenance for equipment
     */
    Equipment scheduleMaintenance(String equipmentId, java.time.LocalDate maintenanceDate);
}
