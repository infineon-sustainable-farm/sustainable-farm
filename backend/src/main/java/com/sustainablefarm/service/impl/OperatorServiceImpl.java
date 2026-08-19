package com.sustainablefarm.service.impl;

import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.Operator.ActiveStatus;
import com.sustainablefarm.model.Operator.Role;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.OperatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Implementation for Operator Entity
 * Personnel data - Supporting Entity
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

    @Autowired
    public OperatorServiceImpl(OperatorRepository operatorRepository) {
        this.operatorRepository = operatorRepository;
    }

    @Override
    public Operator createOperator(Operator operator) {
        // Business Rule: Default to ACTIVE status if not set
        if (operator.getActiveStatus() == null) {
            operator.setActiveStatus(ActiveStatus.ACTIVE);
        }
        return operatorRepository.save(operator);
    }

    @Override
    public Operator updateOperator(String operatorId, Operator operator) {
        Operator existingOperator = getOperatorById(operatorId);
        
        // Update fields
        existingOperator.setOperatorName(operator.getOperatorName());
        existingOperator.setRole(operator.getRole());
        existingOperator.setCertifications(operator.getCertifications());
        existingOperator.setActiveStatus(operator.getActiveStatus());
        existingOperator.setHireDate(operator.getHireDate());
        
        return operatorRepository.save(existingOperator);
    }

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
                .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + operatorId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getAllOperators() {
        return operatorRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getOperatorsByRole(Role role) {
        return operatorRepository.findByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getOperatorsByStatus(ActiveStatus status) {
        return operatorRepository.findByActiveStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getActiveOperatorsByRole(Role role) {
        // Business Rule: Only ACTIVE operators can be assigned
        return operatorRepository.findActiveByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getActiveQcInspectors() {
        return operatorRepository.findActiveQcInspectors();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getActiveAuditors() {
        return operatorRepository.findActiveAuditors();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOperatorActive(String operatorId) {
        return operatorRepository.isOperatorActive(operatorId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(String operatorId, Role role) {
        return operatorRepository.hasRole(operatorId, role);
    }

    @Override
    public Operator updateActiveStatus(String operatorId, ActiveStatus status) {
        Operator operator = getOperatorById(operatorId);
        operator.setActiveStatus(status);
        return operatorRepository.save(operator);
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
        
        return operatorRepository.save(operator);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getOperatorsByCertification(String certification) {
        return operatorRepository.findByCertification(certification);
    }
}