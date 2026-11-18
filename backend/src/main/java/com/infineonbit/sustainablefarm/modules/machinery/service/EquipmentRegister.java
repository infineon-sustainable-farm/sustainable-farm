package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.machinery.api.Category;
import com.infineonbit.sustainablefarm.modules.machinery.api.EquipmentLookup;
import com.infineonbit.sustainablefarm.modules.machinery.api.EquipmentView;
import com.infineonbit.sustainablefarm.modules.machinery.api.ProcessingEquipmentChange;
import com.infineonbit.sustainablefarm.modules.machinery.api.ProcessingEquipmentRegistration;
import com.infineonbit.sustainablefarm.modules.machinery.api.ProcessingEquipmentRegistry;
import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implements the public equipment API of machinery ({@code modules.machinery.api}) on the
 * single {@code equipment} register.
 */
@Service
@Transactional
public class EquipmentRegister implements EquipmentLookup, ProcessingEquipmentRegistry {

    /** The stages other modules may register, change and delete. */
    static final Set<Stage> PROCESSING_STAGES = EnumSet.of(Stage.PROCESSING, Stage.DRYING);

    private final EquipmentRepository equipmentRepository;
    private final EquipmentService equipmentService;

    public EquipmentRegister(EquipmentRepository equipmentRepository, EquipmentService equipmentService) {
        this.equipmentRepository = equipmentRepository;
        this.equipmentService = equipmentService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EquipmentView> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return equipmentRepository.findById(id).map(EquipmentRegister::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EquipmentView> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return equipmentRepository.findByCode(code.trim()).map(EquipmentRegister::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentView> findByStages(Collection<Stage> stages) {
        if (stages == null || stages.isEmpty()) {
            return List.of();
        }
        return equipmentRepository.findByStageInOrderByIdAsc(stages).stream()
                .map(EquipmentRegister::toView)
                .toList();
    }

    @Override
    public EquipmentView register(ProcessingEquipmentRegistration registration) {
        String code = required(registration.code(), "Equipment code", Equipment.CODE_MAX_LENGTH);
        String name = required(registration.name(), "Equipment name", Equipment.NAME_MAX_LENGTH);
        Stage stage = processingStage(registration.stage());
        if (registration.status() == null) {
            throw new BadRequestException("Equipment status is required");
        }
        if (equipmentRepository.existsByCode(code)) {
            throw new ConflictException("Equipment " + code + " already exists.");
        }
        if (equipmentRepository.existsByName(name)) {
            throw new ConflictException(name + " already exists.");
        }
        Equipment equipment = new Equipment();
        equipment.setCode(code);
        equipment.setName(name);
        equipment.setCategory(Category.PROCESSING_MACHINERY);
        equipment.setStage(stage);
        equipment.setStatus(registration.status());
        return toView(equipmentRepository.saveAndFlush(equipment));
    }

    @Override
    public EquipmentView change(Long id, ProcessingEquipmentChange change) {
        Equipment equipment = processingEquipment(id);
        if (change.name() != null) {
            String name = required(change.name(), "Equipment name", Equipment.NAME_MAX_LENGTH);
            if (equipmentRepository.existsByNameAndIdNot(name, id)) {
                throw new ConflictException(name + " already exists.");
            }
            equipment.setName(name);
        }
        if (change.stage() != null) {
            equipment.setStage(processingStage(change.stage()));
        }
        if (change.status() != null) {
            equipment.setStatus(change.status());
        }
        return toView(equipmentRepository.saveAndFlush(equipment));
    }

    @Override
    public void delete(Long id) {
        processingEquipment(id);
        equipmentService.deleteEquipment(id);
    }

    private Equipment processingEquipment(Long id) {
        return equipmentRepository.findById(id)
                .filter(equipment -> PROCESSING_STAGES.contains(equipment.getStage()))
                .orElseThrow(() -> new EquipmentNotFoundException(id));
    }

    private static Stage processingStage(Stage stage) {
        if (stage == null || !PROCESSING_STAGES.contains(stage)) {
            throw new BadRequestException("Processing equipment serves the PROCESSING or DRYING stage");
        }
        return stage;
    }

    private static String required(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(label + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BadRequestException(label + " must be " + maxLength + " characters or fewer");
        }
        return trimmed;
    }

    static EquipmentView toView(Equipment equipment) {
        return new EquipmentView(
                equipment.getId(),
                equipment.getCode(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getStage(),
                equipment.getStatus(),
                equipment.getCreatedAt(),
                equipment.getUpdatedAt());
    }
}
