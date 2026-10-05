package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.OperatorAssignmentCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.OperatorAssignmentUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.OperatorAssignmentObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.OperatorAssignment;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentAlreadyAssignedException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.OperatorAssignmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.OperatorAssignmentRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
@AllArgsConstructor
public class OperatorAssignmentService {
    private final OperatorAssignmentRepository operatorAssignmentRepository;
    private final EquipmentRepository equipmentRepository;

    /**
     * Creates a new operator assignment after checking the equipment exists
     * and is not already assigned.
     *
     * @param request the creation data (fullName, equipmentId, jobTitle, startDate, endDate)
     * @return the representation of the created assignment, including its generated ID
     * @throws EquipmentNotFoundException      if no equipment exists with the given ID
     * @throws EquipmentAlreadyAssignedException if the equipment is already linked to an assignment
     * @throws IllegalArgumentException        if the end date is before the start date
     */
    public OperatorAssignmentObtainingResponse addOperatorAssignment(OperatorAssignmentCreationRequest operatorAssignmentCreationRequest) {
        Equipment equipment = equipmentRepository.findById(operatorAssignmentCreationRequest.equipmentId())
                .orElseThrow(() -> new EquipmentNotFoundException(operatorAssignmentCreationRequest.equipmentId()));

        validateAssignmentDates(operatorAssignmentCreationRequest.startDate(),
                operatorAssignmentCreationRequest.endDate());

        if (operatorAssignmentRepository.existsByEquipment_Id(equipment.getId())) {
            throw new EquipmentAlreadyAssignedException(equipment.getId());
        }

        OperatorAssignment operatorAssignment = new OperatorAssignment();
        operatorAssignment.setFullName(operatorAssignmentCreationRequest.fullName().trim());
        operatorAssignment.setEquipment(equipment);
        operatorAssignment.setJobTitle(operatorAssignmentCreationRequest.jobTitle().trim());
        operatorAssignment.setStartDate(operatorAssignmentCreationRequest.startDate());
        operatorAssignment.setEndDate(operatorAssignmentCreationRequest.endDate());
        try {
            operatorAssignmentRepository.save(operatorAssignment);
        } catch (DataIntegrityViolationException e) {
            if (operatorAssignmentRepository.existsByEquipment_Id(equipment.getId())) {
                throw new EquipmentAlreadyAssignedException(equipment.getId());
            }
            throw e;
        }

        return new OperatorAssignmentObtainingResponse(
                operatorAssignment.getId(),
                operatorAssignment.getFullName(),
                operatorAssignment.getEquipment().getId(),
                operatorAssignment.getJobTitle(),
                operatorAssignment.getStartDate(),
                operatorAssignment.getEndDate());
    }

    /**
     * Checks that the dates sent together by the client are coherent.
     *
     * <p>If no end date is provided, there is nothing to compare and the
     * assignment is considered open-ended.
     *
     * @param startDate the start date sent by the client (never null in the POST flow)
     * @param endDate   the end date sent by the client (may be null)
     * @throws IllegalArgumentException if the end date is before the start date
     */
    private void validateAssignmentDates(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
    }

    /**
     * Checks that an end date sent by the client is coherent with the start
     * date already stored in the database.
     *
     * <p>Intended to be used by the future update flow. If no end date is
     * provided, there is nothing to compare.
     *
     * @param endDate       the end date sent by the client (may be null)
     * @param storedStartDate the start date already registered in the database
     * @throws IllegalArgumentException if the end date is before the stored start date
     */
    private void validateEndDateAgainstStoredStartDate(LocalDate endDate, LocalDate storedStartDate) {
        if (endDate != null && endDate.isBefore(storedStartDate)) {
            throw new IllegalArgumentException("End date cannot be before the stored start date.");
        }
    }

    /**
     * Checks that a start date sent by the client is coherent with the end
     * date already stored in the database.
     *
     * <p>If no end date is stored, the assignment is open-ended and there is
     * nothing to compare.
     *
     * @param startDate     the start date sent by the client (may be null)
     * @param storedEndDate the end date already registered in the database (may be null)
     * @throws IllegalArgumentException if the start date is after the stored end date
     */
    private void validateStartDateAgainstStoredEndDate(LocalDate startDate, LocalDate storedEndDate) {
        if (storedEndDate != null && startDate != null && startDate.isAfter(storedEndDate)) {
            throw new IllegalArgumentException("Start date cannot be after the stored end date.");
        }
    }

    /**
     * Partially updates an existing operator assignment. Only the fields that
     * are explicitly provided (non-null) are modified.
     *
     * @param id      the identifier of the assignment to update
     * @param request the fields to update (null fields are left unchanged)
     * @return the complete state of the assignment after the update
     * @throws OperatorAssignmentNotFoundException if no assignment exists with this ID
     * @throws EquipmentNotFoundException          if the new equipment does not exist
     * @throws EquipmentAlreadyAssignedException   if the new equipment is linked to another assignment
     * @throws IllegalArgumentException            if a provided date is incoherent or a provided text is blank
     */
    public OperatorAssignmentObtainingResponse updateOperatorAssignment(Long id,
            OperatorAssignmentUpdateRequest operatorAssignmentUpdateRequest) {
        OperatorAssignment assignment = operatorAssignmentRepository.findById(id)
                .orElseThrow(() -> new OperatorAssignmentNotFoundException(id));

        Long newEquipmentId = operatorAssignmentUpdateRequest.equipmentId();
        boolean equipmentIdChanged = newEquipmentId != null && !newEquipmentId.equals(assignment.getEquipment().getId());
        if (equipmentIdChanged) {
            Equipment equipment = equipmentRepository.findById(newEquipmentId)
                    .orElseThrow(() -> new EquipmentNotFoundException(newEquipmentId));
            if (operatorAssignmentRepository.existsByEquipment_Id(newEquipmentId)) {
                throw new EquipmentAlreadyAssignedException(newEquipmentId);
            }
            assignment.setEquipment(equipment);
        }

        if (operatorAssignmentUpdateRequest.fullName() != null) {
            String fullName = operatorAssignmentUpdateRequest.fullName().trim();
            if (fullName.isEmpty()) {
                throw new IllegalArgumentException("Full name cannot be blank.");
            }
            assignment.setFullName(fullName);
        }

        if (operatorAssignmentUpdateRequest.jobTitle() != null) {
            String jobTitle = operatorAssignmentUpdateRequest.jobTitle().trim();
            if (jobTitle.isEmpty()) {
                throw new IllegalArgumentException("Job title cannot be blank.");
            }
            assignment.setJobTitle(jobTitle);
        }

        if (operatorAssignmentUpdateRequest.startDate() != null) {
            if (operatorAssignmentUpdateRequest.endDate() != null) {
                validateAssignmentDates(operatorAssignmentUpdateRequest.startDate(),
                        operatorAssignmentUpdateRequest.endDate());
            } else {
                validateStartDateAgainstStoredEndDate(operatorAssignmentUpdateRequest.startDate(),
                        assignment.getEndDate());
            }
            assignment.setStartDate(operatorAssignmentUpdateRequest.startDate());
        }

        if (operatorAssignmentUpdateRequest.endDate() != null) {
            validateEndDateAgainstStoredStartDate(operatorAssignmentUpdateRequest.endDate(),
                    assignment.getStartDate());
            assignment.setEndDate(operatorAssignmentUpdateRequest.endDate());
        }

        try {
            operatorAssignmentRepository.save(assignment);
        } catch (DataIntegrityViolationException e) {
            if (equipmentIdChanged && operatorAssignmentRepository.existsByEquipment_Id(newEquipmentId)) {
                throw new EquipmentAlreadyAssignedException(newEquipmentId);
            }
            throw e;
        }

        return new OperatorAssignmentObtainingResponse(
                assignment.getId(),
                assignment.getFullName(),
                assignment.getEquipment().getId(),
                assignment.getJobTitle(),
                assignment.getStartDate(),
                assignment.getEndDate());
    }

    /**
     * Retrieves all operator assignments (with pagination).
     *
     * <p>Don't returns the entire table.
     * Returns a 10 paginated objects.
     *
     * @return the 10 paginated list of operator assignments
     */
    public Page<OperatorAssignmentObtainingResponse> obtainAllOperatorAssignments(Pageable pageable) {
        Page<OperatorAssignment> pageOfOperatorAssignments = operatorAssignmentRepository.findAll(pageable);
        return pageOfOperatorAssignments.map(operatorAssignment -> new OperatorAssignmentObtainingResponse(
                operatorAssignment.getId(),
                operatorAssignment.getFullName(),
                operatorAssignment.getEquipment().getId(),
                operatorAssignment.getJobTitle(),
                operatorAssignment.getStartDate(),
                operatorAssignment.getEndDate()));
    }

    /**
     * Permanently deletes an operator assignment (hard delete, not soft delete).
     *
     * <p>Only the assignment is removed; the linked equipment is left untouched.
     *
     * @param id the ID of the operator assignment to delete
     * @throws OperatorAssignmentNotFoundException if no assignment exists with this ID
     */
    @Transactional
    public void deleteOperatorAssignment(Long id) {
        if (!operatorAssignmentRepository.existsById(id)) {
            throw new OperatorAssignmentNotFoundException(id);
        }
        operatorAssignmentRepository.deleteById(id);
    }
}