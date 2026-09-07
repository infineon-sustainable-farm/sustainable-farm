package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.CarbonMetric;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.CarbonMetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CarbonMetricService {

    private final CarbonMetricRepository repository;

    @Autowired
    public CarbonMetricService(CarbonMetricRepository repository) {
        this.repository = repository;
    }

    public List<CarbonMetric> findAll() {
        return repository.findAll();
    }

    public Optional<CarbonMetric> findById(String metricId) {
        return repository.findById(metricId);
    }

    public CarbonMetric save(CarbonMetric metric) {
        return repository.save(metric);
    }

    public void deleteById(String metricId) {
        repository.deleteById(metricId);
    }
}
