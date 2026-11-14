package com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.repository;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.MangoVariety;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.QualityGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for HarvestEvent entity
 * Harvest data from Plants workstream - Integration Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface HarvestEventRepository extends JpaRepository<HarvestEvent, String> {

    /**
     * Find harvest event by batch ID
     * Business Rule: One harvest event creates exactly one batch
     */
    Optional<HarvestEvent> findByBatchId(String batchId);

    /**
     * Find harvest events for a set of batch IDs
     * Business Rule: One harvest event creates exactly one batch
     * Used by list endpoints to avoid N+1 lookups when resolving the
     * Plants identity (lot_number, plants_harvest_id) for many batches.
     */
    List<HarvestEvent> findByBatchIdIn(java.util.Collection<String> batchIds);

    /**
     * Find the harvest event imported from a given Plants harvest.
     *
     * <p>Used to make the import idempotent: re-importing the same harvest must
     * return the existing event instead of creating a duplicate. Backed by the
     * unique index added in V16.
     */
    Optional<HarvestEvent> findByPlantsHarvestId(Long plantsHarvestId);

    /**
     * Find harvest events by date range
     */
    List<HarvestEvent> findByHarvestDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find harvest events by mango variety
     */
    List<HarvestEvent> findByMangoVariety(MangoVariety mangoVariety);

    /**
     * Find harvest events by quality grade
     */
    List<HarvestEvent> findByQualityGrade(QualityGrade qualityGrade);

    /**
     * Find harvest events by farm
     */
    List<HarvestEvent> findByFarmId(String farmId);

    /**
     * Find harvest events by farm and date range
     */
    @Query("SELECT h FROM HarvestEvent h WHERE h.farmId = :farmId AND h.harvestDate BETWEEN :startDate AND :endDate")
    List<HarvestEvent> findByFarmAndDateRange(@Param("farmId") String farmId, 
                                            @Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);

    /**
     * Find harvest events by variety and quality grade
     */
    @Query("SELECT h FROM HarvestEvent h WHERE h.mangoVariety = :variety AND h.qualityGrade = :grade")
    List<HarvestEvent> findByVarietyAndGrade(@Param("variety") MangoVariety variety, 
                                       @Param("grade") QualityGrade grade);

    /**
     * Check if batch ID already exists
     * Business Rule: batch_id must be unique
     */
    boolean existsByBatchId(String batchId);

    /**
     * Get total harvest quantity by variety
     */
    @Query("SELECT SUM(h.harvestQuantityKg) FROM HarvestEvent h WHERE h.mangoVariety = :variety")
    Double getTotalHarvestQuantityByVariety(@Param("variety") MangoVariety variety);

    /**
     * Aggregate harvest totals for date range filtering (SQL-side aggregation).
     * Using COALESCE(...) 0 ensures the query returns 0 instead of null
     * when no rows match, and skips SQL NULL values inside SUM().
     */
    @Query("SELECT COALESCE(SUM(h.harvestQuantityKg), 0) FROM HarvestEvent h WHERE h.harvestDate BETWEEN :start AND :end")
    BigDecimal getTotalHarvestQuantityByHarvestDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Count harvest events by date range without loading the rows
     */
    @Query("SELECT COUNT(h) FROM HarvestEvent h WHERE h.harvestDate BETWEEN :start AND :end")
    long countByHarvestDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Get harvest events by team
     */
    List<HarvestEvent> findByHarvestTeamId(String harvestTeamId);
}