package com.sustainablefarm.modules.producttransformation.resources.dryingrun.repository;

import com.sustainablefarm.modules.producttransformation.resources.dryingrun.model.DryingRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for DryingRun entity
 * Drying process data (core transformation) - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface DryingRunRepository extends JpaRepository<DryingRun, String> {

    /**
     * Find drying runs by batch
     */
    List<DryingRun> findByBatchBatchId(String batchId);

    /**
     * Find drying runs by date range
     */
    List<DryingRun> findByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find drying runs by equipment
     */
    List<DryingRun> findByEquipmentEquipmentId(String equipmentId);

    /**
     * Find drying runs by operator
     */
    List<DryingRun> findByOperatorOperatorId(String operatorId);

    /**
     * Find drying runs by batch and date range
     */
    @Query("SELECT d FROM DryingRun d WHERE d.batch.batchId = :batchId AND d.startTime BETWEEN :startDate AND :endDate")
    List<DryingRun> findByBatchAndDateRange(@Param("batchId") String batchId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Find drying runs with moisture content within target range
     * Business Rule: Target moisture 12-18% for EU compliance
     */
    @Query("SELECT d FROM DryingRun d WHERE d.endMoisturePct BETWEEN 12 AND 18")
    List<DryingRun> findWithinTargetMoistureRange();

    /**
     * Find drying runs with moisture content below minimum
     */
    @Query("SELECT d FROM DryingRun d WHERE d.endMoisturePct < 6")
    List<DryingRun> findBelowMinimumMoisture();

    /**
     * Find drying runs with moisture content above maximum
     */
    @Query("SELECT d FROM DryingRun d WHERE d.endMoisturePct > 18")
    List<DryingRun> findAboveMaximumMoisture();

    /**
     * Get total energy usage by date range
     */
    @Query("SELECT SUM(d.energyUsageKwh) FROM DryingRun d WHERE d.startTime BETWEEN :startDate AND :endDate")
    Double getTotalEnergyUsageByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Get average drying duration by batch
     */
    @Query("SELECT AVG(d.durationHours) FROM DryingRun d WHERE d.batch.batchId = :batchId")
    Double getAverageDurationByBatch(@Param("batchId") String batchId);

    /**
     * Get average moisture reduction by batch
     */
    @Query("SELECT AVG(d.startMoisturePct - d.endMoisturePct) FROM DryingRun d WHERE d.batch.batchId = :batchId")
    Double getAverageMoistureReductionByBatch(@Param("batchId") String batchId);

    /**
     * Find drying runs by drying method (equipment type)
     */
    @Query("SELECT d FROM DryingRun d WHERE d.equipment.equipmentType = :equipmentType")
    List<DryingRun> findByEquipmentType(@Param("equipmentType") String equipmentType);
}