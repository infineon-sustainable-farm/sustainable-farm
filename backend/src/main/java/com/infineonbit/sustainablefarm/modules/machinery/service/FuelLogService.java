package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.FuelLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.FuelLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.FuelLogObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.FuelLog;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.FuelLogNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.FuelLogRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FuelLogService {
    private final FuelLogRepository fuelLogRepository;
    private final EquipmentRepository equipmentRepository;

    /**
     * Creates a fuel log entry for an existing equipment.
     *
     * @param request the creation data (equipmentId, date, liters, cost)
     * @return the created fuel log, including its generated ID
     * @throws EquipmentNotFoundException if no equipment exists with the given ID
     */
    public FuelLogObtainingResponse addFuelLog(FuelLogCreationRequest request) {
        Equipment equipment = equipmentRepository.findById(request.equipmentId())
                .orElseThrow(() -> new EquipmentNotFoundException(request.equipmentId()));

        FuelLog fuelLog = new FuelLog();
        fuelLog.setEquipment(equipment);
        fuelLog.setDate(request.date());
        fuelLog.setLiters(request.liters());
        fuelLog.setCost(request.cost());

        try {
            fuelLogRepository.save(fuelLog);
        } catch (DataIntegrityViolationException e) {
            if (!equipmentRepository.existsById(request.equipmentId())) {
                throw new EquipmentNotFoundException(request.equipmentId());
            }
            throw e;
        }

        return toResponse(fuelLog);
    }

    /**
     * Retrieves all fuel logs (with pagination).
     *
     * @param pageable the pagination information
     * @return the paginated list of fuel logs
     */
    public Page<FuelLogObtainingResponse> obtainAllFuelLogs(Pageable pageable) {
        return fuelLogRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Retrieves a single fuel log by its ID.
     *
     * @param id the fuel log identifier
     * @return the fuel log details
     * @throws FuelLogNotFoundException if no fuel log exists with this ID
     */
    public FuelLogObtainingResponse obtainFuelLogById(Long id) {
        FuelLog fuelLog = fuelLogRepository.findById(id)
                .orElseThrow(() -> new FuelLogNotFoundException(id));

        return toResponse(fuelLog);
    }

    /**
     * Updates a fuel log with new values. Only non-null fields are modified.
     *
     * @param id      the fuel log identifier
     * @param request the update data
     * @return the complete state of the fuel log after the update
     * @throws FuelLogNotFoundException if no fuel log exists with this ID
     * @throws EquipmentNotFoundException if the new equipment does not exist
     */
    @Transactional
    public FuelLogObtainingResponse updateFuelLog(Long id, FuelLogUpdateRequest request) {
        FuelLog fuelLog = fuelLogRepository.findById(id)
                .orElseThrow(() -> new FuelLogNotFoundException(id));

        if (request.equipmentId() != null) {
            Equipment equipment = equipmentRepository.findById(request.equipmentId())
                    .orElseThrow(() -> new EquipmentNotFoundException(request.equipmentId()));
            fuelLog.setEquipment(equipment);
        }
        if (request.date() != null) {
            fuelLog.setDate(request.date());
        }
        if (request.liters() != null) {
            fuelLog.setLiters(request.liters());
        }
        if (request.cost() != null) {
            fuelLog.setCost(request.cost());
        }

        return toResponse(fuelLogRepository.saveAndFlush(fuelLog));
    }

    /**
     * Permanently deletes a fuel log.
     *
     * @param id the fuel log identifier
     * @throws FuelLogNotFoundException if no fuel log exists with this ID
     */
    @Transactional
    public void deleteFuelLog(Long id) {
        int affectedRows = fuelLogRepository.deleteFuelLogById(id);
        if (affectedRows == 0) {
            throw new FuelLogNotFoundException(id);
        }
    }

    private FuelLogObtainingResponse toResponse(FuelLog fuelLog) {
        return new FuelLogObtainingResponse(
                fuelLog.getId(),
                fuelLog.getEquipment() != null ? fuelLog.getEquipment().getId() : null,
                fuelLog.getDate(),
                fuelLog.getLiters(),
                fuelLog.getCost()
        );
    }
}
