package com.sustainablefarm.service;

import com.sustainablefarm.model.RawIntake;

import java.time.LocalDate;
import java.util.List;

/**
 * Service Interface for RawIntake Entity
 * Raw material intake from Plants - Supporting Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface RawIntakeService {

    /**
     * Create new raw intake
     */
    RawIntake createRawIntake(RawIntake rawIntake);

    /**
     * Update raw intake
     */
    RawIntake updateRawIntake(String intakeId, RawIntake rawIntake);

    /**
     * Delete raw intake
     */
    void deleteRawIntake(String intakeId);

    /**
     * Get raw intake by ID
     */
    RawIntake getRawIntakeById(String intakeId);

    /**
     * Get raw intake by batch ID
     * Business Rule: One intake initializes exactly one batch
     */
    RawIntake getRawIntakeByBatchId(String batchId);

    /**
     * Get all raw intakes
     */
    List<RawIntake> getAllRawIntakes();

    /**
     * Get raw intakes by date range
     */
    List<RawIntake> getRawIntakesByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get raw intakes by source farm
     */
    List<RawIntake> getRawIntakesBySourceFarm(String sourceFarm);

    /**
     * Get raw intakes by variety
     */
    List<RawIntake> getRawIntakesByVariety(com.sustainablefarm.model.HarvestEvent.MangoVariety variety);

    /**
     * Get raw intakes by quality grade
     */
    List<RawIntake> getRawIntakesByGrade(com.sustainablefarm.model.HarvestEvent.QualityGrade grade);

    /**
     * Get raw intakes by farm and date range
     */
    List<RawIntake> getRawIntakesBySourceFarmAndDateRange(String farm, LocalDate startDate, LocalDate endDate);

    /**
     * Get total received quantity by variety
     */
    Double getTotalReceivedQuantityByVariety(com.sustainablefarm.model.HarvestEvent.MangoVariety variety);

    /**
     * Get average quality grade distribution
     */
    long countByReceivedGrade(com.sustainablefarm.model.HarvestEvent.QualityGrade grade);

    /**
     * Initialize batch from raw intake
     * Business Rule: One intake initializes exactly one batch
     */
    RawIntake initializeBatchFromIntake(String intakeId);
}