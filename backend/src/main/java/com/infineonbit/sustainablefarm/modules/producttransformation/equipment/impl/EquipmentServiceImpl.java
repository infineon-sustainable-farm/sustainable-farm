package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.impl;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.machinery.api.EquipmentLookup;
import com.infineonbit.sustainablefarm.modules.machinery.api.EquipmentView;
import com.infineonbit.sustainablefarm.modules.machinery.api.ProcessingEquipmentChange;
import com.infineonbit.sustainablefarm.modules.machinery.api.ProcessingEquipmentRegistration;
import com.infineonbit.sustainablefarm.modules.machinery.api.ProcessingEquipmentRegistry;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment.EquipmentType;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment.MaintenanceStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentSpec;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.repository.EquipmentSpecRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.service.EquipmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Product transformation equipment on the farm equipment register (ADR 0004): machinery owns
 * the equipment ({@code modules.machinery.api}), product transformation its attributes
 * ({@link EquipmentSpec}). The mapping rules are in {@link EquipmentRegisterMapping}.
 *
 * Business Rules:
 * - Equipment must be ACTIVE for assignment
 * - Equipment must be available before use
 * - Maintenance status tracking required
 */
@Transactional
// Explicit bean name: Machinery also ships an EquipmentService.
@Service("prodTransEquipmentService")
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentLookup equipmentLookup;
    private final ProcessingEquipmentRegistry equipmentRegistry;
    private final EquipmentSpecRepository specRepository;

    public EquipmentServiceImpl(EquipmentLookup equipmentLookup,
                                ProcessingEquipmentRegistry equipmentRegistry,
                                EquipmentSpecRepository specRepository) {
        this.equipmentLookup = equipmentLookup;
        this.equipmentRegistry = equipmentRegistry;
        this.specRepository = specRepository;
    }

    @Override
    public Equipment createEquipment(Equipment equipment) {
        // Business Rule: Default to ACTIVE status if not set
        if (equipment.getMaintenanceStatus() == null) {
            equipment.setMaintenanceStatus(MaintenanceStatus.ACTIVE);
        }
        if (equipment.getEquipmentType() == null) {
            throw new BadRequestException("Equipment type is required");
        }
        EquipmentView registered = equipmentRegistry.register(new ProcessingEquipmentRegistration(
                equipment.getEquipmentId(),
                equipment.getEquipmentName(),
                EquipmentRegisterMapping.stageOf(equipment.getEquipmentType()),
                EquipmentRegisterMapping.statusOf(equipment.getMaintenanceStatus())));
        EquipmentSpec spec = new EquipmentSpec();
        spec.setEquipmentId(registered.id());
        applyAttributes(spec, equipment);
        return toEquipment(registered, specRepository.save(spec));
    }

    @Override
    public Equipment updateEquipment(String equipmentId, Equipment equipment) {
        Equipment existing = getEquipmentById(equipmentId);
        EquipmentView changed = equipmentRegistry.change(existing.getId(), new ProcessingEquipmentChange(
                equipment.getEquipmentName(),
                equipment.getEquipmentType() == null ? null
                        : EquipmentRegisterMapping.stageOf(equipment.getEquipmentType()),
                equipment.getMaintenanceStatus() == null ? null
                        : EquipmentRegisterMapping.statusOf(equipment.getMaintenanceStatus())));

        EquipmentSpec spec = specRepository.findByEquipmentId(existing.getId()).orElse(null);
        if (spec == null && hasAttributes(equipment)) {
            // A machine registered from machinery gets its attributes on its first edit here.
            spec = new EquipmentSpec();
            spec.setEquipmentId(existing.getId());
        }
        if (spec != null) {
            applyAttributes(spec, equipment);
            spec = specRepository.save(spec);
        }
        return toEquipment(changed, spec);
    }

    @Override
    public void deleteEquipment(String equipmentId) {
        Equipment existing = getEquipmentById(equipmentId);
        specRepository.deleteByEquipmentId(existing.getId());
        specRepository.flush();
        equipmentRegistry.delete(existing.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Equipment> findEquipmentById(String equipmentId) {
        return equipmentLookup.findByCode(equipmentId)
                .filter(EquipmentServiceImpl::isProcessing)
                .map(this::withSpec);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Equipment> findEquipmentByRegisterId(Long id) {
        return equipmentLookup.findById(id)
                .filter(EquipmentServiceImpl::isProcessing)
                .map(this::withSpec);
    }

    @Override
    @Transactional(readOnly = true)
    public Equipment getEquipmentById(String equipmentId) {
        return findEquipmentById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found with ID: " + equipmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public Equipment getEquipmentByRegisterId(Long id) {
        return findEquipmentByRegisterId(id)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getAllEquipment() {
        List<EquipmentView> views = equipmentLookup.findByStages(EquipmentRegisterMapping.PROCESSING_STAGES);
        Map<Long, EquipmentSpec> specs = specRepository
                .findByEquipmentIdIn(views.stream().map(EquipmentView::id).toList()).stream()
                .collect(Collectors.toMap(EquipmentSpec::getEquipmentId, Function.identity()));
        return views.stream().map(view -> toEquipment(view, specs.get(view.id()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getEquipmentByType(EquipmentType type) {
        return getAllEquipment().stream().filter(e -> e.getEquipmentType() == type).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getEquipmentByStatus(MaintenanceStatus status) {
        return getAllEquipment().stream()
                .filter(e -> e.getMaintenanceStatus() == status)
                .sorted(Comparator.comparing(Equipment::getEquipmentName))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getAvailableEquipmentByType(EquipmentType type) {
        // Business Rule: Equipment must be ACTIVE for assignment
        return getEquipmentByType(type).stream()
                .filter(e -> e.getMaintenanceStatus() == MaintenanceStatus.ACTIVE)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEquipmentAvailable(String equipmentId) {
        // Business Rule: Equipment must be ACTIVE for assignment
        return equipmentLookup.findByCode(equipmentId)
                .filter(EquipmentServiceImpl::isProcessing)
                .map(view -> EquipmentRegisterMapping.maintenanceStatusOf(view.status()) == MaintenanceStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getEquipmentNeedingMaintenance(LocalDate date) {
        return getAllEquipment().stream()
                .filter(e -> e.getMaintenanceStatus() == MaintenanceStatus.MAINTENANCE
                        || (e.getLastMaintenanceDate() != null && e.getLastMaintenanceDate().isBefore(date)))
                .toList();
    }

    @Override
    public Equipment updateMaintenanceStatus(String equipmentId, MaintenanceStatus status) {
        Equipment equipment = getEquipmentById(equipmentId);
        equipment.setMaintenanceStatus(status);

        // If setting to ACTIVE, update last maintenance date
        if (status == MaintenanceStatus.ACTIVE) {
            equipment.setLastMaintenanceDate(LocalDate.now());
        }

        return updateEquipment(equipmentId, equipment);
    }

    @Override
    public Equipment scheduleMaintenance(String equipmentId, LocalDate maintenanceDate) {
        Equipment equipment = getEquipmentById(equipmentId);
        equipment.setMaintenanceStatus(MaintenanceStatus.MAINTENANCE);
        equipment.setLastMaintenanceDate(maintenanceDate);
        return updateEquipment(equipmentId, equipment);
    }

    private static boolean isProcessing(EquipmentView view) {
        return EquipmentRegisterMapping.PROCESSING_STAGES.contains(view.stage());
    }

    private Equipment withSpec(EquipmentView view) {
        return toEquipment(view, specRepository.findByEquipmentId(view.id()).orElse(null));
    }

    private static boolean hasAttributes(Equipment equipment) {
        return equipment.getEquipmentType() != null
                || equipment.getCapacityKgPerHour() != null
                || equipment.getEnergyConsumptionKwhPerKg() != null
                || equipment.getLocation() != null
                || equipment.getLastMaintenanceDate() != null;
    }

    private static void applyAttributes(EquipmentSpec spec, Equipment equipment) {
        if (equipment.getEquipmentType() == null) {
            throw new BadRequestException("Equipment type is required");
        }
        if (equipment.getCapacityKgPerHour() == null) {
            throw new BadRequestException("Capacity is required");
        }
        if (equipment.getEnergyConsumptionKwhPerKg() == null) {
            throw new BadRequestException("Energy consumption is required");
        }
        spec.setEquipmentType(equipment.getEquipmentType());
        spec.setCapacityKgPerHour(equipment.getCapacityKgPerHour());
        spec.setEnergyConsumptionKwhPerKg(equipment.getEnergyConsumptionKwhPerKg());
        spec.setLocation(equipment.getLocation());
        spec.setLastMaintenanceDate(equipment.getLastMaintenanceDate());
    }

    static Equipment toEquipment(EquipmentView view, EquipmentSpec spec) {
        Equipment equipment = new Equipment();
        equipment.setId(view.id());
        equipment.setEquipmentId(view.code());
        equipment.setEquipmentName(view.name());
        equipment.setMaintenanceStatus(EquipmentRegisterMapping.maintenanceStatusOf(view.status()));
        equipment.setCreatedAt(timestamp(view.createdAt()));
        equipment.setUpdatedAt(timestamp(view.updatedAt()));
        if (spec != null) {
            equipment.setEquipmentType(spec.getEquipmentType());
            equipment.setCapacityKgPerHour(spec.getCapacityKgPerHour());
            equipment.setEnergyConsumptionKwhPerKg(spec.getEnergyConsumptionKwhPerKg());
            equipment.setLocation(spec.getLocation());
            equipment.setLastMaintenanceDate(spec.getLastMaintenanceDate());
            if (spec.getUpdatedAt() != null
                    && (view.updatedAt() == null || spec.getUpdatedAt().isAfter(view.updatedAt()))) {
                equipment.setUpdatedAt(timestamp(spec.getUpdatedAt()));
            }
        }
        return equipment;
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
