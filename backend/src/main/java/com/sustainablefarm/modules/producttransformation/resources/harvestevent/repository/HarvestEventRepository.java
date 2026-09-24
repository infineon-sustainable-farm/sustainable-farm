package com.sustainablefarm.modules.producttransformation.resources.harvestevent.repository;

import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent.QualityGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Get harvest events by team
     */
    List<HarvestEvent> findByHarvestTeamId(String harvestTeamId);
}