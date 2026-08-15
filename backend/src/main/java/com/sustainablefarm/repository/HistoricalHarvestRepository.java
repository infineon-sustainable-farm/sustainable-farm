package com.sustainablefarm.repository;

import com.sustainablefarm.model.HistoricalHarvest;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for HistoricalHarvest entity
 * Aggregated historical harvest data for forecasting - Integration Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface HistoricalHarvestRepository extends JpaRepository<HistoricalHarvest, com.sustainablefarm.model.HistoricalHarvestId> {

    /**
     * Find historical harvest by year
     */
    List<HistoricalHarvest> findByYear(Integer year);

    /**
     * Find historical harvest by year and month
     */
    List<HistoricalHarvest> findByYearAndMonth(Integer year, Integer month);

    /**
     * Find historical harvest by year, month, and week
     */
    List<HistoricalHarvest> findByYearAndMonthAndWeek(Integer year, Integer month, Integer week);

    /**
     * Find historical harvest by variety
     */
    List<HistoricalHarvest> findByMangoVariety(MangoVariety mangoVariety);

    /**
     * Find historical harvest by year and variety
     */
    @Query("SELECT h FROM HistoricalHarvest h WHERE h.year = :year AND h.mangoVariety = :variety")
    List<HistoricalHarvest> findByYearAndVariety(@Param("year") Integer year, 
                                          @Param("variety") MangoVariety variety);

    /**
     * Get total harvest quantity by year
     */
    @Query("SELECT SUM(h.harvestQuantityKg) FROM HistoricalHarvest h WHERE h.year = :year")
    Double getTotalHarvestByYear(@Param("year") Integer year);

    /**
     * Get average quality grade distribution by year
     */
    @Query("SELECT AVG(h.qualityGradeAPct), AVG(h.qualityGradeBPct), AVG(h.qualityGradeCPct) FROM HistoricalHarvest h WHERE h.year = :year")
    Object[] getAverageQualityDistributionByYear(@Param("year") Integer year);

    /**
     * Find historical harvest by date range
     */
    @Query("SELECT h FROM HistoricalHarvest h WHERE h.year = :year AND h.month BETWEEN :startMonth AND :endMonth")
    List<HistoricalHarvest> findByYearAndMonthRange(@Param("year") Integer year,
                                            @Param("startMonth") Integer startMonth,
                                            @Param("endMonth") Integer endMonth);

    /**
     * Get historical data for forecasting (minimum 7 years)
     * Business Rule: Minimum 7 years required for forecasting
     */
    @Query("SELECT h FROM HistoricalHarvest h WHERE h.year >= :minYear ORDER BY h.year, h.month, h.week")
    List<HistoricalHarvest> findForecastingData(@Param("minYear") Integer minYear);
}