package com.sustainablefarm.service.impl;

import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.model.Equipment.EquipmentType;
import com.sustainablefarm.model.Equipment.MaintenanceStatus;
import com.sustainablefarm.repository.EquipmentRepository;
import com.sustainablefarm.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service Implementation for Equipment Entity
 * Machinery and equipment data - Supporting Entity
 * 
 * Business Rules:
 * - Equipment must be ACTIVE for assignment
 * - Equipment must be available before use
 * - Maintenance status tracking required
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@Transactional
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository equipmentRepository;

    @Autowired
    public EquipmentServiceImpl(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @Override
    public Equipment createEquipment(Equipment equipment) {
        // Business Rule: Default to ACTIVE status if not set
        if (equipment.getMaintenanceStatus() == null) {
            equipment.setMaintenanceStatus(MaintenanceStatus.ACTIVE);
        }
        return equipmentRepository.save(equipment);
    }

    @Override
    public Equipment updateEquipment(String equipmentId, Equipment equipment) {
        Equipment existingEquipment = getEquipmentById(equipmentId);
        
        // Update fields
        existingEquipment.setEquipmentName(equipment.getEquipmentName());
        existingEquipment.setEquipmentType(equipment.getEquipmentType());
        existingEquipment.setCapacityKgPerHour(equipment.getCapacityKgPerHour());
        existingEquipment.setEnergyConsumptionKwhPerKg(equipment.getEnergyConsumptionKwhPerKg());
        existingEquipment.setLocation(equipment.getLocation());
        existingEquipment.setMaintenanceStatus(equipment.getMaintenanceStatus());
        existingEquipment.setLastMaintenanceDate(equipment.getLastMaintenanceDate());
        
        return equipmentRepository.save(existingEquipment);
    }

    @Override
    public void deleteEquipment(String equipmentId) {
        if (!equipmentRepository.existsById(equipmentId)) {
            throw new IllegalArgumentException("Equipment not found with ID: " + equipmentId);
        }
        equipmentRepository.deleteById(equipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Equipment getEquipmentById(String equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found with ID: " + equipmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getEquipmentByType(EquipmentType type) {
        return equipmentRepository.findByEquipmentType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getEquipmentByStatus(MaintenanceStatus status) {
        return equipmentRepository.findByMaintenanceStatusOrderByEquipmentName(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getAvailableEquipmentByType(EquipmentType type) {
        // Business Rule: Equipment must be ACTIVE for assignment
        return equipmentRepository.findAvailableByType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEquipmentAvailable(String equipmentId) {
        // Business Rule: Equipment must be ACTIVE for assignment
        return equipmentRepository.isEquipmentAvailable(equipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getEquipmentNeedingMaintenance(LocalDate date) {
        return equipmentRepository.findEquipmentNeedingMaintenance(date);
    }

    @Override
    public Equipment updateMaintenanceStatus(String equipmentId, MaintenanceStatus status) {
        Equipment equipment = getEquipmentById(equipmentId);
        equipment.setMaintenanceStatus(status);
        
        // If setting to ACTIVE, update last maintenance date
        if (status == MaintenanceStatus.ACTIVE) {
            equipment.setLastMaintenanceDate(LocalDate.now());
        }
        
        return equipmentRepository.save(equipment);
    }

    @Override
    public Equipment scheduleMaintenance(String equipmentId, LocalDate maintenanceDate) {
        Equipment equipment = getEquipmentById(equipmentId);
        equipment.setMaintenanceStatus(MaintenanceStatus.MAINTENANCE);
        equipment.setLastMaintenanceDate(maintenanceDate);
        return equipmentRepository.save(equipment);
    }
}