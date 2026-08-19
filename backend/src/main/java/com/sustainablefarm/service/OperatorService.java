package com.sustainablefarm.service;

import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.Operator.ActiveStatus;
import com.sustainablefarm.model.Operator.Role;

import java.util.List;

/**
 * Service Interface for Operator Entity
 * Personnel data - Supporting Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface OperatorService {

    /**
     * Create new operator
     */
    Operator createOperator(Operator operator);

    /**
     * Update operator
     */
    Operator updateOperator(String operatorId, Operator operator);

    /**
     * Delete operator
     */
    void deleteOperator(String operatorId);

    /**
     * Get operator by ID
     */
    Operator getOperatorById(String operatorId);

    /**
     * Get all operators
     */
    List<Operator> getAllOperators();

    /**
     * Get operators by role
     */
    List<Operator> getOperatorsByRole(Role role);

    /**
     * Get operators by active status
     */
    List<Operator> getOperatorsByStatus(ActiveStatus status);

    /**
     * Get active operators by role
     * Business Rule: Only ACTIVE operators can be assigned
     */
    List<Operator> getActiveOperatorsByRole(Role role);

    /**
     * Get active QC inspectors
     */
    List<Operator> getActiveQcInspectors();

    /**
     * Get active auditors
     */
    List<Operator> getActiveAuditors();

    /**
     * Check if operator is active
     */
    boolean isOperatorActive(String operatorId);

    /**
     * Check if operator has specific role
     */
    boolean hasRole(String operatorId, Role role);

    /**
     * Update operator active status
     */
    Operator updateActiveStatus(String operatorId, ActiveStatus status);

    /**
     * Add certification to operator
     */
    Operator addCertification(String operatorId, String certification);

    /**
     * Get operators by certification
     */
    List<Operator> getOperatorsByCertification(String certification);
}