package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.ConsumptionLog;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.LoadCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.ConsumptionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ConsumptionLogService {

    private final ConsumptionLogRepository repository;

    @Autowired
    public ConsumptionLogService(ConsumptionLogRepository repository) {
        this.repository = repository;
    }

    public List<ConsumptionLog> findAll() {
        return repository.findAll();
    }

    public List<ConsumptionLog> findToday() {
        return repository.findByDate(LocalDate.now());
    }

    public List<ConsumptionLog> findByCategory(LoadCategory category) {
        return repository.findByLoadCategory(category);
    }

    public Optional<ConsumptionLog> findById(String logId) {
        return repository.findById(logId);
    }

    public ConsumptionLog save(ConsumptionLog log) {
        return repository.save(log);
    }

    public void deleteById(String logId) {
        repository.deleteById(logId);
    }
}
