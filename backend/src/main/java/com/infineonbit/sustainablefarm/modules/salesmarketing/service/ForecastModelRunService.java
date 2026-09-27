package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ForecastModelRun;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ForecastModelRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForecastModelRunService {

    private final ForecastModelRunRepository repository;

    public ForecastModelRunService(ForecastModelRunRepository repository) {
        this.repository = repository;
    }

    public List<ForecastModelRun> getAll() {
        return repository.findAll();
    }

    public ForecastModelRun getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ForecastModelRun not found with id: " + id));
    }

    public ForecastModelRun create(ForecastModelRun item) {
        return repository.save(item);
    }

    /**
     * Copies every editable field from updatedData onto the existing
     * row, then saves it. The id and created_at are left untouched.
     */
    public ForecastModelRun update(Integer id, ForecastModelRun updatedData) {
        ForecastModelRun existing = getById(id);

        existing.setCropVariety(updatedData.getCropVariety());
        existing.setRegion(updatedData.getRegion());
        existing.setConfidenceIntervalPct(updatedData.getConfidenceIntervalPct());
        existing.setUseWeatherData(updatedData.getUseWeatherData());
        existing.setUseCommodityPrices(updatedData.getUseCommodityPrices());
        existing.setUseGeopoliticalIndex(updatedData.getUseGeopoliticalIndex());
        existing.setModelStatus(updatedData.getModelStatus());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
