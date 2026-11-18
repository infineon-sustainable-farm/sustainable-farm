package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.service;

import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentType;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.MaintenanceStatus;

import java.util.List;

/**
 * Service Interface for the product transformation equipment: the processing and drying
 * machines of the farm register owned by machinery, with their product transformation
 * attributes. {@code equipmentId} is the register business code.
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
     * Find equipment by ID (business code), empty when no processing or drying machine has it
     */
    java.util.Optional<Equipment> findEquipmentById(String equipmentId);

    /**
     * Find equipment by its register id, empty when no processing or drying machine has it
     */
    java.util.Optional<Equipment> findEquipmentByRegisterId(Long id);

    /**
     * Get equipment by its register id (the Long id stored in the equipment_id columns)
     */
    Equipment getEquipmentByRegisterId(Long id);

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
