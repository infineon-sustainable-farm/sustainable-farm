package com.infineonbit.sustainablefarm.modules.producttransformation.operator.repository;

import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Operator entity
 * Personnel data - Supporting Entity
 *
 * <p>The active status belongs to the person of the staff referential: the queries on it are
 * in {@code OperatorServiceImpl}.</p>
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
     * Whether a person already holds a product transformation operator role
     */
    boolean existsByPersonId(Long personId);

    /**
     * Find operators by certification
     */
    @Query("SELECT o FROM Operator o WHERE o.certifications LIKE %:certification%")
    List<Operator> findByCertification(@Param("certification") String certification);

    /**
     * Check if operator has specific role
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Operator o WHERE o.operatorId = :id AND o.role = :role")
    boolean hasRole(@Param("id") String operatorId, @Param("role") Role role);
}
