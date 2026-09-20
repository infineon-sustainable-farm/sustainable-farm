package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.UsageLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.UsageLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.UsageLogObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.UsageLog;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.UsageLogNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.UsageLogRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UsageLogService {
    private final UsageLogRepository usageLogRepository;
    private final EquipmentRepository equipmentRepository;

    /**
     * Creates a usage log entry for an existing equipment.
     *
     * @param request the creation data (equipmentId, date, hoursUsed, operator, notes)
     * @return the created usage log, including its generated ID
     * @throws EquipmentNotFoundException if no equipment exists with the given ID
     */
    public UsageLogObtainingResponse addUsageLog(UsageLogCreationRequest request) {
        Equipment equipment = equipmentRepository.findById(request.equipmentId())
                .orElseThrow(() -> new EquipmentNotFoundException(request.equipmentId()));

        UsageLog usageLog = new UsageLog();
        usageLog.setEquipment(equipment);
        usageLog.setDate(request.date());
        usageLog.setHoursUsed(request.hoursUsed());
        usageLog.setOperator(trimToNull(request.operator()));
        usageLog.setNotes(trimToNull(request.notes()));

        try {
            usageLogRepository.save(usageLog);
        } catch (DataIntegrityViolationException e) {
            if (!equipmentRepository.existsById(request.equipmentId())) {
                throw new EquipmentNotFoundException(request.equipmentId());
            }
            throw e;
        }

        return toResponse(usageLog);
    }

    /**
     * Retrieves all usage logs (with pagination).
     *
     * @param pageable the pagination information
     * @return the paginated list of usage logs
     */
    public Page<UsageLogObtainingResponse> obtainAllUsageLogs(Pageable pageable) {
        return usageLogRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Retrieves a single usage log by its ID.
     *
     * @param id the usage log identifier
     * @return the usage log details
     * @throws UsageLogNotFoundException if no usage log exists with this ID
     */
    public UsageLogObtainingResponse obtainUsageLogById(Long id) {
        UsageLog usageLog = usageLogRepository.findById(id)
                .orElseThrow(() -> new UsageLogNotFoundException(id));

        return toResponse(usageLog);
    }

    /**
     * Updates a usage log with new values. Only non-null fields are modified.
     *
     * @param id      the usage log identifier
     * @param request the update data
     * @return the complete state of the usage log after the update
     * @throws UsageLogNotFoundException if no usage log exists with this ID
     * @throws EquipmentNotFoundException if the new equipment does not exist
     */
    @Transactional
    public UsageLogObtainingResponse updateUsageLog(Long id, UsageLogUpdateRequest request) {
        UsageLog usageLog = usageLogRepository.findById(id)
                .orElseThrow(() -> new UsageLogNotFoundException(id));

        if (request.equipmentId() != null) {
            Equipment equipment = equipmentRepository.findById(request.equipmentId())
                    .orElseThrow(() -> new EquipmentNotFoundException(request.equipmentId()));
            usageLog.setEquipment(equipment);
        }
        if (request.date() != null) {
            usageLog.setDate(request.date());
        }
        if (request.hoursUsed() != null) {
            usageLog.setHoursUsed(request.hoursUsed());
        }
        if (request.operator() != null) {
            usageLog.setOperator(trimToNull(request.operator()));
        }
        if (request.notes() != null) {
            usageLog.setNotes(trimToNull(request.notes()));
        }

        return toResponse(usageLogRepository.saveAndFlush(usageLog));
    }

    /**
     * Permanently deletes a usage log.
     *
     * @param id the usage log identifier
     * @throws UsageLogNotFoundException if no usage log exists with this ID
     */
    @Transactional
    public void deleteUsageLog(Long id) {
        int affectedRows = usageLogRepository.deleteUsageLogById(id);
        if (affectedRows == 0) {
            throw new UsageLogNotFoundException(id);
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UsageLogObtainingResponse toResponse(UsageLog usageLog) {
        return new UsageLogObtainingResponse(
                usageLog.getId(),
                usageLog.getEquipment() != null ? usageLog.getEquipment().getId() : null,
                usageLog.getDate(),
                usageLog.getHoursUsed(),
                usageLog.getOperator(),
                usageLog.getNotes()
        );
    }
}
