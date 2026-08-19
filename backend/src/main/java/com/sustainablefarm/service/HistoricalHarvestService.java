package com.sustainablefarm.service;

import com.sustainablefarm.model.HistoricalHarvest;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;

import java.util.List;

/**
 * Service Interface for HistoricalHarvest Entity
 * Aggregated historical harvest data for forecasting - Integration Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface HistoricalHarvestService {

    /**
     * Create new historical harvest record
     */
    HistoricalHarvest createHistoricalHarvest(HistoricalHarvest historicalHarvest);

    /**
     * Update historical harvest record
     */
    HistoricalHarvest updateHistoricalHarvest(Integer year, Integer month, Integer week, MangoVariety variety, HistoricalHarvest historicalHarvest);

    /**
     * Delete historical harvest record
     */
    void deleteHistoricalHarvest(Integer year, Integer month, Integer week, MangoVariety variety);

    /**
     * Get historical harvest by composite key
     */
    HistoricalHarvest getHistoricalHarvestById(Integer year, Integer month, Integer week, MangoVariety variety);

    /**
     * Get all historical harvest records
     */
    List<HistoricalHarvest> getAllHistoricalHarvests();

    /**
     * Get historical harvest by year
     */
    List<HistoricalHarvest> getHistoricalHarvestByYear(Integer year);

    /**
     * Get historical harvest by year and month
     */
    List<HistoricalHarvest> getHistoricalHarvestByYearAndMonth(Integer year, Integer month);

    /**
     * Get historical harvest by year, month, and week
     */
    List<HistoricalHarvest> getHistoricalHarvestByYearMonthWeek(Integer year, Integer month, Integer week);

    /**
     * Get historical harvest by variety
     */
    List<HistoricalHarvest> getHistoricalHarvestByVariety(MangoVariety variety);

    /**
     * Get historical harvest by year and variety
     */
    List<HistoricalHarvest> getHistoricalHarvestByYearAndVariety(Integer year, MangoVariety variety);

    /**
     * Get total harvest quantity by year
     */
    Double getTotalHarvestByYear(Integer year);

    /**
     * Get average quality grade distribution by year
     */
    Object[] getAverageQualityDistributionByYear(Integer year);

    /**
     * Get historical harvest by date range
     */
    List<HistoricalHarvest> getHistoricalHarvestByDateRange(Integer year, Integer startMonth, Integer endMonth);

    /**
     * Get historical data for forecasting
     * Business Rule: Minimum 7 years required for forecasting
     */
    List<HistoricalHarvest> getForecastingData(Integer minYear);

    /**
     * Aggregate harvest data from events
     * Business Rule: Historical data aggregated from harvest events for forecasting
     */
    HistoricalHarvest aggregateFromHarvestEvents(Integer year, Integer month, Integer week, MangoVariety variety);
}