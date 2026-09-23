package com.sustainablefarm.modules.producttransformation.resources.operator.repository;

import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator;
import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator.ActiveStatus;
import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Operator entity
 * Personnel data - Supporting Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface OperatorRepository extends JpaRepository<Operator, String> {

    /**
     * Find operators by role
     */
    List<Operator> findByRole(Role role);

    /**
     * Find operators by active status
     */
    List<Operator> findByActiveStatus(ActiveStatus activeStatus);

    /**
     * Find active operators by role
     * Business Rule: Only ACTIVE operators can be assigned
     */
    @Query("SELECT o FROM Operator o WHERE o.role = :role AND o.activeStatus = 'ACTIVE'")
    List<Operator> findActiveByRole(@Param("role") Role role);

    /**
     * Find active QC inspectors
     */
    @Query("SELECT o FROM Operator o WHERE o.role = 'QC_INSPECTOR' AND o.activeStatus = 'ACTIVE'")
    List<Operator> findActiveQcInspectors();

    /**
     * Find active auditors
     */
    @Query("SELECT o FROM Operator o WHERE o.role = 'AUDITOR' AND o.activeStatus = 'ACTIVE'")
    List<Operator> findActiveAuditors();

    /**
     * Find operators by certification
     */
    @Query("SELECT o FROM Operator o WHERE o.certifications LIKE %:certification%")
    List<Operator> findByCertification(@Param("certification") String certification);

    /**
     * Check if operator is active
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Operator o WHERE o.operatorId = :id AND o.activeStatus = 'ACTIVE'")
    boolean isOperatorActive(@Param("id") String operatorId);

    /**
     * Check if operator has specific role
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Operator o WHERE o.operatorId = :id AND o.role = :role")
    boolean hasRole(@Param("id") String operatorId, @Param("role") Role role);
}