package com.sustainablefarm.modules.producttransformation.resources.audittrail.repository;

import com.sustainablefarm.modules.producttransformation.resources.audittrail.model.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Audit Trail operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
    
    /**
     * Find audit trail entries by batch
     */
    List<AuditTrail> findByBatchBatchIdOrderByChangeTimestampDesc(String batchId);
    
    /**
     * Find audit trail entries by operator
     */
    List<AuditTrail> findByOperatorOperatorIdOrderByChangeTimestampDesc(String operatorId);
    
    /**
     * Find audit trail entries by date range
     */
    @Query("SELECT a FROM AuditTrail a WHERE a.changeTimestamp BETWEEN :startDate AND :endDate ORDER BY a.changeTimestamp DESC")
    List<AuditTrail> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find audit trail entries by batch and date range
     */
    @Query("SELECT a FROM AuditTrail a WHERE a.batch.batchId = :batchId AND a.changeTimestamp BETWEEN :startDate AND :endDate ORDER BY a.changeTimestamp DESC")
    List<AuditTrail> findByBatchAndDateRange(@Param("batchId") String batchId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count audit trail entries for a batch
     */
    long countByBatchBatchId(String batchId);
}