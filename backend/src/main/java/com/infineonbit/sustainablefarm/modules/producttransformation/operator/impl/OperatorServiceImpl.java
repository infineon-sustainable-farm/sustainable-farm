package com.infineonbit.sustainablefarm.modules.producttransformation.operator.impl;

import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator.ActiveStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator.Role;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.service.OperatorService;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDetails;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service Implementation for Operator Entity
 * Personnel data - Supporting Entity
 * 
 * The operator is the product transformation role of a person of the staff referential
 * ({@code modules.staff.api.PersonDirectory}): name and active status are the person's.
 *
 * Business Rules:
 * - Only ACTIVE operators can be assigned
 * - Operators must be certified for assigned role
 * - QC inspectors must have QC_INSPECTOR role
 * - Auditors must have AUDITOR role
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@Transactional
public class OperatorServiceImpl implements OperatorService {

    private final OperatorRepository operatorRepository;
    private final PersonDirectory personDirectory;

    @Autowired
    public OperatorServiceImpl(OperatorRepository operatorRepository, PersonDirectory personDirectory) {
        this.operatorRepository = operatorRepository;
        this.personDirectory = personDirectory;
    }

    /**
     * Creates the operator role of a person: the person carrying the operator name is reused
     * (unless it already holds an operator role), or created.
     */
    @Override
    public Operator createOperator(Operator operator) {
        // Business Rule: Default to ACTIVE status if not set
        boolean statusGiven = operator.getActiveStatus() != null;
        boolean active = !statusGiven || operator.getActiveStatus() == ActiveStatus.ACTIVE;
        PersonDto person = personDirectory.findOrCreate(
                new PersonDetails(operator.getOperatorName(), null, null, active),
                personId -> !operatorRepository.existsByPersonId(personId));
        if (statusGiven && person.active() != active) {
            person = personDirectory.update(person.id(), new PersonDetails(null, null, null, active));
        }
        operator.setPersonId(person.id());
        return withPerson(operatorRepository.save(operator), person);
    }

    /** Updates the role attributes here and the name and active status on the person. */
    @Override
    public Operator updateOperator(String operatorId, Operator operator) {
        Operator existingOperator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + operatorId));

        // Update fields
        existingOperator.setRole(operator.getRole());
        existingOperator.setCertifications(operator.getCertifications());
        existingOperator.setHireDate(operator.getHireDate());
        PersonDto person = personDirectory.update(existingOperator.getPersonId(), new PersonDetails(
                operator.getOperatorName(), null, null,
                operator.getActiveStatus() == null ? null : operator.getActiveStatus() == ActiveStatus.ACTIVE));

        return withPerson(operatorRepository.save(existingOperator), person);
    }

    /** Deletes the operator role; the person stays in the staff referential. */
    @Override
    public void deleteOperator(String operatorId) {
        if (!operatorRepository.existsById(operatorId)) {
            throw new IllegalArgumentException("Operator not found with ID: " + operatorId);
        }
        operatorRepository.deleteById(operatorId);
    }

    @Override
    @Transactional(readOnly = true)
    public Operator getOperatorById(String operatorId) {
        return operatorRepository.findById(operatorId)
                .map(this::withPerson)
                .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + operatorId));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Operator> findOperatorById(String operatorId) {
        return operatorRepository.findById(operatorId).map(this::withPerson);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getAllOperators() {
        return withPeople(operatorRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getOperatorsByRole(Role role) {
        return withPeople(operatorRepository.findByRole(role));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getOperatorsByStatus(ActiveStatus status) {
        return withStatus(operatorRepository.findAll(), status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getActiveOperatorsByRole(Role role) {
        // Business Rule: Only ACTIVE operators can be assigned
        return withStatus(operatorRepository.findByRole(role), ActiveStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getActiveQcInspectors() {
        return getActiveOperatorsByRole(Role.QC_INSPECTOR);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getActiveAuditors() {
        return getActiveOperatorsByRole(Role.AUDITOR);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOperatorActive(String operatorId) {
        return operatorRepository.findById(operatorId)
                .map(this::withPerson)
                .map(operator -> operator.getActiveStatus() == ActiveStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(String operatorId, Role role) {
        return operatorRepository.hasRole(operatorId, role);
    }

    @Override
    public Operator updateActiveStatus(String operatorId, ActiveStatus status) {
        Operator operator = getOperatorById(operatorId);
        PersonDto person = personDirectory.update(operator.getPersonId(),
                new PersonDetails(null, null, null, status == ActiveStatus.ACTIVE));
        return withPerson(operator, person);
    }

    @Override
    public Operator addCertification(String operatorId, String certification) {
        Operator operator = getOperatorById(operatorId);
        String existingCertifications = operator.getCertifications();

        if (existingCertifications == null || existingCertifications.isEmpty()) {
            operator.setCertifications(certification);
        } else {
            operator.setCertifications(existingCertifications + ", " + certification);
        }

        return withPerson(operatorRepository.save(operator));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getOperatorsByCertification(String certification) {
        return withPeople(operatorRepository.findByCertification(certification));
    }

    private List<Operator> withStatus(List<Operator> operators, ActiveStatus status) {
        return withPeople(operators).stream().filter(operator -> operator.getActiveStatus() == status).toList();
    }

    private Operator withPerson(Operator operator) {
        return withPerson(operator, personDirectory.findById(operator.getPersonId()).orElse(null));
    }

    private List<Operator> withPeople(List<Operator> operators) {
        Map<Long, PersonDto> people = personDirectory.findAllById(operators.stream().map(Operator::getPersonId).toList());
        operators.forEach(operator -> withPerson(operator, people.get(operator.getPersonId())));
        return operators;
    }

    /** Fills the name and active status of an operator from its person. */
    private static Operator withPerson(Operator operator, PersonDto person) {
        operator.setOperatorName(person == null ? null : person.fullName());
        operator.setActiveStatus(person == null || !person.active() ? ActiveStatus.INACTIVE : ActiveStatus.ACTIVE);
        return operator;
    }
}
