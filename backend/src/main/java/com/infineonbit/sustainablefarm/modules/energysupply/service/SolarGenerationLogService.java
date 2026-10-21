package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.SolarGenerationLog;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.SolarGenerationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SolarGenerationLogService {

    private final SolarGenerationLogRepository repository;

    @Autowired
    public SolarGenerationLogService(SolarGenerationLogRepository repository) {
        this.repository = repository;
    }

    public List<SolarGenerationLog> findAll() {
        return repository.findAll();
    }

    public List<SolarGenerationLog> findLast7Days() {
        return repository.findByDateBetween(LocalDate.now().minusDays(7), LocalDate.now());
    }

    public Optional<SolarGenerationLog> findById(String logId) {
        return repository.findById(logId);
    }

    public SolarGenerationLog save(SolarGenerationLog log) {
        return repository.save(log);
    }

    public void deleteById(String logId) {
        repository.deleteById(logId);
    }
}
