package com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.repository;

import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.model.DryingRun;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    @EntityGraph(attributePaths = {"batch", "equipment", "operator"})
    List<DryingRun> findByBatchBatchId(String batchId);

    /**
     * Find drying runs by date range
     */
    @EntityGraph(attributePaths = {"batch", "equipment", "operator"})
    List<DryingRun> findByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find drying runs by equipment
     */
    @EntityGraph(attributePaths = {"batch", "equipment", "operator"})
    List<DryingRun> findByEquipmentEquipmentId(String equipmentId);

    /**
     * Find drying runs by operator
     */
    @EntityGraph(attributePaths = {"batch", "equipment", "operator"})
    List<DryingRun> findByOperatorOperatorId(String operatorId);

    /**
     * Find drying runs by batch and date range
     */
    @Query("SELECT d FROM DryingRun d WHERE d.batch.batchId = :batchId AND d.startTime BETWEEN :startDate AND :endDate")
    List<DryingRun> findByBatchAndDateRange(@Param("batchId") String batchId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Read endpoints map the entity to a DTO outside the transaction (open-in-view
     * is disabled), and DryingRunResponse computes energy per kg from the batch
     * quantity: the associations must be fetched with the entity.
     */
    @EntityGraph(attributePaths = {"batch", "equipment", "operator"})
    List<DryingRun> findAll();

    @EntityGraph(attributePaths = {"batch", "equipment", "operator"})
    Optional<DryingRun> findById(String runId);

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
     * Aggregate energy consumption for a period (SQL-side aggregation).
     * Returns 0 for empty periods instead of null.
     */
    @Query("SELECT COALESCE(SUM(d.energyUsageKwh), 0) FROM DryingRun d WHERE d.startTime BETWEEN :startDate AND :endDate")
    BigDecimal getTotalEnergyUsageByDateRangeOrDefault(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Count drying runs by date range (for KPI counts)
     */
    long countByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Aggregate total energy usage across all runs (SQL-side aggregation).
     */
    @Query("SELECT COALESCE(SUM(d.energyUsageKwh), 0) FROM DryingRun d")
    BigDecimal getTotalEnergyUsage();

    /**
     * Aggregate production output proxy for a period (duration * target temperature sum).
     * Used by the kWh/kg energy KPI.
     */
    @Query("SELECT COALESCE(SUM(d.durationHours * d.targetTemperatureC), 0) FROM DryingRun d WHERE d.startTime BETWEEN :startDate AND :endDate")
    BigDecimal getTotalOutputProxyByDateRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

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