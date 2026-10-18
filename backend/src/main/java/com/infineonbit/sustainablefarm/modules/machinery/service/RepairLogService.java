package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.RepairLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.RepairLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.RepairLogObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.RepairLog;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.RepairLogNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.RepairLogRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RepairLogService {
    private final RepairLogRepository repairLogRepository;
    private final EquipmentRepository equipmentRepository;

    /**
     * Creates a repair log entry for an existing equipment.
     *
     * @param request the creation data (equipmentId, date, issue, downtime, cost, technician)
     * @return the created repair log, including its generated ID
     * @throws EquipmentNotFoundException if no equipment exists with the given ID
     * @throws IllegalArgumentException  if the issue description is missing
     */
    public RepairLogObtainingResponse addRepairLog(RepairLogCreationRequest request) {
        Equipment equipment = equipmentRepository.findById(request.equipmentId())
                .orElseThrow(() -> new EquipmentNotFoundException(request.equipmentId()));

        RepairLog repairLog = new RepairLog();
        repairLog.setEquipment(equipment);
        repairLog.setDate(request.date());
        repairLog.setIssue(trimRequired(request.issue(), "Issue description is required."));
        repairLog.setDowntime(request.downtime());
        repairLog.setCost(request.cost());
        repairLog.setTechnician(trimToNull(request.technician()));

        try {
            repairLogRepository.save(repairLog);
        } catch (DataIntegrityViolationException e) {
            if (!equipmentRepository.existsById(request.equipmentId())) {
                throw new EquipmentNotFoundException(request.equipmentId());
            }
            throw e;
        }

        return toResponse(repairLog);
    }

    /**
     * Retrieves all repair logs (with pagination).
     *
     * @param pageable the pagination information
     * @return the paginated list of repair logs
     */
    public Page<RepairLogObtainingResponse> obtainAllRepairLogs(Pageable pageable) {
        return repairLogRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Retrieves a single repair log by its ID.
     *
     * @param id the repair log identifier
     * @return the repair log details
     * @throws RepairLogNotFoundException if no repair log exists with this ID
     */
    public RepairLogObtainingResponse obtainRepairLogById(Long id) {
        RepairLog repairLog = repairLogRepository.findById(id)
                .orElseThrow(() -> new RepairLogNotFoundException(id));

        return toResponse(repairLog);
    }

    /**
     * Updates a repair log with new values. Only non-null fields are modified.
     *
     * @param id      the repair log identifier
     * @param request the update data
     * @return the complete state of the repair log after the update
     * @throws RepairLogNotFoundException if no repair log exists with this ID
     * @throws EquipmentNotFoundException if the new equipment does not exist
     * @throws IllegalArgumentException   if the issue description is blank
     */
    @Transactional
    public RepairLogObtainingResponse updateRepairLog(Long id, RepairLogUpdateRequest request) {
        RepairLog repairLog = repairLogRepository.findById(id)
                .orElseThrow(() -> new RepairLogNotFoundException(id));

        if (request.equipmentId() != null) {
            Equipment equipment = equipmentRepository.findById(request.equipmentId())
                    .orElseThrow(() -> new EquipmentNotFoundException(request.equipmentId()));
            repairLog.setEquipment(equipment);
        }
        if (request.date() != null) {
            repairLog.setDate(request.date());
        }
        if (request.issue() != null) {
            repairLog.setIssue(trimRequired(request.issue(), "Issue description cannot be blank."));
        }
        if (request.downtime() != null) {
            repairLog.setDowntime(request.downtime());
        }
        if (request.cost() != null) {
            repairLog.setCost(request.cost());
        }
        if (request.technician() != null) {
            repairLog.setTechnician(trimToNull(request.technician()));
        }

        return toResponse(repairLogRepository.saveAndFlush(repairLog));
    }

    /**
     * Permanently deletes a repair log.
     *
     * @param id the repair log identifier
     * @throws RepairLogNotFoundException if no repair log exists with this ID
     */
    @Transactional
    public void deleteRepairLog(Long id) {
        int affectedRows = repairLogRepository.deleteRepairLogById(id);
        if (affectedRows == 0) {
            throw new RepairLogNotFoundException(id);
        }
    }

    private String trimRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private RepairLogObtainingResponse toResponse(RepairLog repairLog) {
        return new RepairLogObtainingResponse(
                repairLog.getId(),
                repairLog.getEquipment() != null ? repairLog.getEquipment().getId() : null,
                repairLog.getDate(),
                repairLog.getIssue(),
                repairLog.getDowntime(),
                repairLog.getCost(),
                repairLog.getTechnician()
        );
    }
}
