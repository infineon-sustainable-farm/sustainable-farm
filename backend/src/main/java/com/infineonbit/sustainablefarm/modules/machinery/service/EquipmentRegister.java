package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.api.EquipmentLookup;
import com.infineonbit.sustainablefarm.modules.machinery.api.EquipmentView;
import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Implements the public equipment API of machinery ({@code modules.machinery.api}) on the
 * single {@code equipment} register.
 *
 * <p>Read only: machinery is the only module that creates, renames, changes the status of and
 * deletes a machine, whatever its stage, from its own screens and routes. The other modules pick
 * a machine by its id and keep their own data about it (resource ownership, 2026-11-19).</p>
 */
@Service
@Transactional(readOnly = true)
public class EquipmentRegister implements EquipmentLookup {

    private final EquipmentRepository equipmentRepository;

    public EquipmentRegister(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @Override
    public Optional<EquipmentView> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return equipmentRepository.findById(id).map(EquipmentRegister::toView);
    }

    @Override
    public Optional<EquipmentView> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return equipmentRepository.findByCode(code.trim()).map(EquipmentRegister::toView);
    }

    @Override
    public List<EquipmentView> findByStages(Collection<Stage> stages) {
        if (stages == null || stages.isEmpty()) {
            return List.of();
        }
        return equipmentRepository.findByStageInOrderByIdAsc(stages).stream()
                .map(EquipmentRegister::toView)
                .toList();
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
