package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.MaintenanceScheduleCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.MaintenanceScheduleUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.MaintenanceScheduleObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.MaintenanceSchedule;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.MaintenanceScheduleNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.MaintenanceScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@AllArgsConstructor
public class MaintenanceScheduleService {
    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final EquipmentRepository equipmentRepository;

    /**
     * Creates a maintenance schedule entry for an existing equipment.
     *
     * @param request the creation data (equipmentId, type, frequency, lastCompleted, nextDue, operator)
     * @return the created maintenance schedule, including its generated ID
     * @throws EquipmentNotFoundException if no equipment exists with the given ID
     * @throws IllegalArgumentException  if the next due date is before the last completed date
     */
    public MaintenanceScheduleObtainingResponse addMaintenanceSchedule(MaintenanceScheduleCreationRequest request) {
        Equipment equipment = equipmentRepository.findById(request.equipmentId())
                .orElseThrow(() -> new EquipmentNotFoundException(request.equipmentId()));

        validateDates(request.lastCompleted(), request.nextDue());

        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setEquipment(equipment);
        schedule.setType(request.type());
        schedule.setFrequency(trimToNull(request.frequency()));
        schedule.setLastCompleted(request.lastCompleted());
        schedule.setNextDue(request.nextDue());
        schedule.setOperator(trimToNull(request.operator()));

        try {
            maintenanceScheduleRepository.save(schedule);
        } catch (DataIntegrityViolationException e) {
            if (!equipmentRepository.existsById(request.equipmentId())) {
                throw new EquipmentNotFoundException(request.equipmentId());
            }
            throw e;
        }

        return toResponse(schedule);
    }

    /**
     * Retrieves all maintenance schedules (with pagination).
     *
     * @param pageable the pagination information
     * @return the paginated list of maintenance schedules
     */
    public Page<MaintenanceScheduleObtainingResponse> obtainAllMaintenanceSchedules(Pageable pageable) {
        return maintenanceScheduleRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Retrieves a single maintenance schedule by its ID.
     *
     * @param id the maintenance schedule identifier
     * @return the maintenance schedule details
     * @throws MaintenanceScheduleNotFoundException if no schedule exists with this ID
     */
    public MaintenanceScheduleObtainingResponse obtainMaintenanceScheduleById(Long id) {
        MaintenanceSchedule schedule = maintenanceScheduleRepository.findById(id)
                .orElseThrow(() -> new MaintenanceScheduleNotFoundException(id));

        return toResponse(schedule);
    }

    /**
     * Updates a maintenance schedule with new values. Only non-null fields are modified.
     *
     * @param id      the maintenance schedule identifier
     * @param request the update data
     * @return the complete state of the maintenance schedule after the update
     * @throws MaintenanceScheduleNotFoundException if no schedule exists with this ID
     * @throws EquipmentNotFoundException           if the new equipment does not exist
     * @throws IllegalArgumentException             if the resulting dates are incoherent
     */
    @Transactional
    public MaintenanceScheduleObtainingResponse updateMaintenanceSchedule(Long id, MaintenanceScheduleUpdateRequest request) {
        MaintenanceSchedule schedule = maintenanceScheduleRepository.findById(id)
                .orElseThrow(() -> new MaintenanceScheduleNotFoundException(id));

        LocalDate effectiveLastCompleted = request.lastCompleted() != null ? request.lastCompleted() : schedule.getLastCompleted();
        LocalDate effectiveNextDue = request.nextDue() != null ? request.nextDue() : schedule.getNextDue();
        validateDates(effectiveLastCompleted, effectiveNextDue);

        if (request.equipmentId() != null) {
            Equipment equipment = equipmentRepository.findById(request.equipmentId())
                    .orElseThrow(() -> new EquipmentNotFoundException(request.equipmentId()));
            schedule.setEquipment(equipment);
        }
        if (request.type() != null) {
            schedule.setType(request.type());
        }
        if (request.frequency() != null) {
            schedule.setFrequency(trimToNull(request.frequency()));
        }
        if (request.lastCompleted() != null) {
            schedule.setLastCompleted(request.lastCompleted());
        }
        if (request.nextDue() != null) {
            schedule.setNextDue(request.nextDue());
        }
        if (request.operator() != null) {
            schedule.setOperator(trimToNull(request.operator()));
        }

        return toResponse(maintenanceScheduleRepository.saveAndFlush(schedule));
    }

    /**
     * Permanently deletes a maintenance schedule.
     *
     * @param id the maintenance schedule identifier
     * @throws MaintenanceScheduleNotFoundException if no schedule exists with this ID
     */
    @Transactional
    public void deleteMaintenanceSchedule(Long id) {
        int affectedRows = maintenanceScheduleRepository.deleteMaintenanceScheduleById(id);
        if (affectedRows == 0) {
            throw new MaintenanceScheduleNotFoundException(id);
        }
    }

    private void validateDates(LocalDate lastCompleted, LocalDate nextDue) {
        if (lastCompleted != null && nextDue != null && nextDue.isBefore(lastCompleted)) {
            throw new IllegalArgumentException("Next due date cannot be before the last completed date.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private MaintenanceScheduleObtainingResponse toResponse(MaintenanceSchedule schedule) {
        return new MaintenanceScheduleObtainingResponse(
                schedule.getId(),
                schedule.getEquipment() != null ? schedule.getEquipment().getId() : null,
                schedule.getType(),
                schedule.getFrequency(),
                schedule.getLastCompleted(),
                schedule.getNextDue(),
                schedule.getOperator()
        );
    }
}
