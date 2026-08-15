package com.sustainablefarm.repository;

import com.sustainablefarm.model.RawIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RawIntake entity
 * Raw material intake from Plants - Supporting Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface RawIntakeRepository extends JpaRepository<RawIntake, String> {

    /**
     * Find raw intake by batch ID
     * Business Rule: One intake initializes exactly one batch
     */
    Optional<RawIntake> findByBatchBatchId(String batchId);

    /**
     * Find raw intake by date range
     */
    List<RawIntake> findByIntakeDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find raw intake by source farm
     */
    List<RawIntake> findBySourceFarm(String sourceFarm);

    /**
     * Find raw intake by variety
     */
    List<RawIntake> findByReceivedVariety(com.sustainablefarm.model.HarvestEvent.MangoVariety receivedVariety);

    /**
     * Find raw intake by quality grade
     */
    List<RawIntake> findByReceivedGrade(com.sustainablefarm.model.HarvestEvent.QualityGrade receivedGrade);

    /**
     * Find raw intake by farm and date range
     */
    @Query("SELECT r FROM RawIntake r WHERE r.sourceFarm = :farm AND r.intakeDate BETWEEN :startDate AND :endDate")
    List<RawIntake> findBySourceFarmAndDateRange(@Param("farm") String farm,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * Get total received quantity by variety
     */
    @Query("SELECT SUM(r.receivedQuantityKg) FROM RawIntake r WHERE r.receivedVariety = :variety")
    Double getTotalReceivedQuantityByVariety(@Param("variety") com.sustainablefarm.model.HarvestEvent.MangoVariety variety);

    /**
     * Get average quality grade distribution
     */
    @Query("SELECT COUNT(r) FROM RawIntake r WHERE r.receivedGrade = :grade")
    long countByReceivedGrade(@Param("grade") com.sustainablefarm.model.HarvestEvent.QualityGrade grade);
}