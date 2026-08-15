package com.sustainablefarm.service.impl;

import com.sustainablefarm.model.HarvestEvent;
import com.sustainablefarm.model.HistoricalHarvest;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.model.HistoricalHarvestId;
import com.sustainablefarm.repository.HarvestEventRepository;
import com.sustainablefarm.repository.HistoricalHarvestRepository;
import com.sustainablefarm.service.HistoricalHarvestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Implementation for HistoricalHarvest Entity
 * Aggregated historical harvest data for forecasting - Integration Entity
 * 
 * Business Rules:
 * - Minimum 7 years required for forecasting
 * - Historical data aggregated from harvest events for forecasting
 * - Composite key: (year, month, week, mango_variety)
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@Transactional
public class HistoricalHarvestServiceImpl implements HistoricalHarvestService {

    private final HistoricalHarvestRepository historicalHarvestRepository;
    private final HarvestEventRepository harvestEventRepository;

    @Autowired
    public HistoricalHarvestServiceImpl(HistoricalHarvestRepository historicalHarvestRepository,
                                      HarvestEventRepository harvestEventRepository) {
        this.historicalHarvestRepository = historicalHarvestRepository;
        this.harvestEventRepository = harvestEventRepository;
    }

    @Override
    public HistoricalHarvest createHistoricalHarvest(HistoricalHarvest historicalHarvest) {
        // Business Rule: Validate week range (1-53)
        if (historicalHarvest.getWeek() < 1 || historicalHarvest.getWeek() > 53) {
            throw new IllegalArgumentException("Week must be between 1 and 53. Current: " + historicalHarvest.getWeek());
        }
        
        // Business Rule: Validate month range (1-12)
        if (historicalHarvest.getMonth() < 1 || historicalHarvest.getMonth() > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12. Current: " + historicalHarvest.getMonth());
        }
        
        return historicalHarvestRepository.save(historicalHarvest);
    }

    @Override
    public HistoricalHarvest updateHistoricalHarvest(Integer year, Integer month, Integer week, MangoVariety variety, HistoricalHarvest historicalHarvest) {
        HistoricalHarvestId id = new HistoricalHarvestId(year, month, week, variety);
        HistoricalHarvest existingRecord = getHistoricalHarvestById(year, month, week, variety);
        
        // Update fields
        existingRecord.setHarvestQuantityKg(historicalHarvest.getHarvestQuantityKg());
        existingRecord.setQualityGradeAPct(historicalHarvest.getQualityGradeAPct());
        existingRecord.setQualityGradeBPct(historicalHarvest.getQualityGradeBPct());
        existingRecord.setQualityGradeCPct(historicalHarvest.getQualityGradeCPct());
        existingRecord.setWeatherCondition(historicalHarvest.getWeatherCondition());
        existingRecord.setRainfallMm(historicalHarvest.getRainfallMm());
        existingRecord.setTemperatureAvgC(historicalHarvest.getTemperatureAvgC());
        
        return historicalHarvestRepository.save(existingRecord);
    }

    @Override
    public void deleteHistoricalHarvest(Integer year, Integer month, Integer week, MangoVariety variety) {
        HistoricalHarvestId id = new HistoricalHarvestId(year, month, week, variety);
        if (!historicalHarvestRepository.existsById(id)) {
            throw new IllegalArgumentException("Historical harvest not found with composite key: " + id);
        }
        historicalHarvestRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoricalHarvest getHistoricalHarvestById(Integer year, Integer month, Integer week, MangoVariety variety) {
        HistoricalHarvestId id = new HistoricalHarvestId(year, month, week, variety);
        return historicalHarvestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Historical harvest not found with composite key: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalHarvest> getAllHistoricalHarvests() {
        return historicalHarvestRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalHarvest> getHistoricalHarvestByYear(Integer year) {
        return historicalHarvestRepository.findByYear(year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalHarvest> getHistoricalHarvestByYearAndMonth(Integer year, Integer month) {
        return historicalHarvestRepository.findByYearAndMonth(year, month);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalHarvest> getHistoricalHarvestByYearMonthWeek(Integer year, Integer month, Integer week) {
        return historicalHarvestRepository.findByYearAndMonthAndWeek(year, month, week);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalHarvest> getHistoricalHarvestByVariety(MangoVariety variety) {
        return historicalHarvestRepository.findByMangoVariety(variety);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalHarvest> getHistoricalHarvestByYearAndVariety(Integer year, MangoVariety variety) {
        return historicalHarvestRepository.findByYearAndVariety(year, variety);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalHarvestByYear(Integer year) {
        return historicalHarvestRepository.getTotalHarvestByYear(year);
    }

    @Override
    @Transactional(readOnly = true)
    public Object[] getAverageQualityDistributionByYear(Integer year) {
        return historicalHarvestRepository.getAverageQualityDistributionByYear(year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalHarvest> getHistoricalHarvestByDateRange(Integer year, Integer startMonth, Integer endMonth) {
        return historicalHarvestRepository.findByYearAndMonthRange(year, startMonth, endMonth);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalHarvest> getForecastingData(Integer minYear) {
        // Business Rule: Minimum 7 years required for forecasting
        int actualMinYear = minYear != null ? minYear : (java.time.Year.now().getValue() - 7);
        return historicalHarvestRepository.findForecastingData(actualMinYear);
    }

    @Override
    public HistoricalHarvest aggregateFromHarvestEvents(Integer year, Integer month, Integer week, MangoVariety variety) {
        // Business Rule: Historical data aggregated from harvest events for forecasting
        // This would aggregate harvest events from the specified period
        // For now, this is a placeholder for the actual aggregation logic
        
        List<HarvestEvent> harvestEvents = harvestEventRepository.findByHarvestDateBetween(
            java.time.LocalDate.of(year, month, 1),
            java.time.LocalDate.of(year, month, java.time.YearMonth.of(year, month).lengthOfMonth())
        );
        
        // Filter by variety and week (simplified logic)
        double totalQuantity = harvestEvents.stream()
            .filter(he -> he.getMangoVariety() == variety)
            .mapToDouble(HarvestEvent::getHarvestQuantityKg)
            .sum();
        
        // Calculate quality grade distribution
        long gradeACount = harvestEvents.stream()
            .filter(he -> he.getMangoVariety() == variety && he.getQualityGrade() == HarvestEvent.QualityGrade.A)
            .count();
        
        long gradeBCount = harvestEvents.stream()
            .filter(he -> he.getMangoVariety() == variety && he.getQualityGrade() == HarvestEvent.QualityGrade.B)
            .count();
        
        long gradeCCount = harvestEvents.stream()
            .filter(he -> he.getMangoVariety() == variety && he.getQualityGrade() == HarvestEvent.QualityGrade.C)
            .count();
        
        long totalGraded = gradeACount + gradeBCount + gradeCCount;
        
        HistoricalHarvest historicalHarvest = new HistoricalHarvest();
        historicalHarvest.setYear(year);
        historicalHarvest.setMonth(month);
        historicalHarvest.setWeek(week);
        historicalHarvest.setMangoVariety(variety);
        historicalHarvest.setHarvestQuantityKg(totalQuantity);
        
        if (totalGraded > 0) {
            historicalHarvest.setQualityGradeAPct((gradeACount * 100.0) / totalGraded);
            historicalHarvest.setQualityGradeBPct((gradeBCount * 100.0) / totalGraded);
            historicalHarvest.setQualityGradeCPct((gradeCCount * 100.0) / totalGraded);
        }
        
        return historicalHarvestRepository.save(historicalHarvest);
    }
}