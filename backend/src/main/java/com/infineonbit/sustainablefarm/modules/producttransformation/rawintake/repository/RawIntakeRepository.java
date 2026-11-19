package com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.repository;

import com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.model.RawIntake;
import org.springframework.data.jpa.repository.EntityGraph;
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
public interface RawIntakeRepository extends JpaRepository<RawIntake, Long> {

    /** The RawIntake of a business code, shown as {@code intakeId}. */
    @EntityGraph(attributePaths = {"batch"})
    Optional<RawIntake> findByIntakeId(String intakeId);

    /** Whether a RawIntake has this business code. */
    boolean existsByIntakeId(String intakeId);

    /** Deletes the row of a business code, with what it cascades to. */
    void deleteByIntakeId(String intakeId);


    /**
     * Find raw intake by batch ID
     * Business Rule: One intake initializes exactly one batch
     */
    @EntityGraph(attributePaths = {"batch"})
    Optional<RawIntake> findByBatchBatchId(String batchId);

    /**
     * Find raw intake by date range
     */
    @EntityGraph(attributePaths = {"batch"})
    List<RawIntake> findByIntakeDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find raw intake of several source blocks (the blocks of a farm)
     */
    @EntityGraph(attributePaths = {"batch"})
    List<RawIntake> findBySourceBlockIdIn(java.util.Collection<Long> sourceBlockIds);

    /**
     * Find raw intake by variety
     */
    @EntityGraph(attributePaths = {"batch"})
    List<RawIntake> findByReceivedVarietyKey(String receivedVarietyKey);

    /**
     * Find raw intake by quality grade
     */
    @EntityGraph(attributePaths = {"batch"})
    List<RawIntake> findByReceivedGrade(com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.QualityGrade receivedGrade);

    /**
     * Find raw intake by farm and date range
     */
    @EntityGraph(attributePaths = {"batch"})
    List<RawIntake> findBySourceBlockIdInAndIntakeDateBetween(java.util.Collection<Long> sourceBlockIds,
                                                              LocalDate startDate, LocalDate endDate);

    /**
     * Get total received quantity by variety
     */
    @Query("SELECT SUM(r.receivedQuantityKg) FROM RawIntake r WHERE r.receivedVarietyKey = :variety")
    Double getTotalReceivedQuantityByVariety(@Param("variety") String variety);

    /**
     * Get average quality grade distribution
     */
    @Query("SELECT COUNT(r) FROM RawIntake r WHERE r.receivedGrade = :grade")
    long countByReceivedGrade(@Param("grade") com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.QualityGrade grade);

    /** Every row, with what the responses show fetched in the same query (no N+1). */
    @Override
    @EntityGraph(attributePaths = {"batch"})
    List<RawIntake> findAll();
}