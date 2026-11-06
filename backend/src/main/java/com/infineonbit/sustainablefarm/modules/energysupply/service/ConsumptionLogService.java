package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.ConsumptionLogRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.ConsumptionLogResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ConsumptionLog;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.LoadCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.ConsumptionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service("energyConsumptionLogService")
@Transactional(readOnly = true)
public class ConsumptionLogService {

    private final ConsumptionLogRepository repository;

    public ConsumptionLogService(ConsumptionLogRepository repository) {
        this.repository = repository;
    }

    public List<ConsumptionLogResponse> findAll() {
        return repository.findAll().stream().map(ConsumptionLogResponse::from).toList();
    }

    public List<ConsumptionLogResponse> findToday() {
        return repository.findByDate(LocalDate.now()).stream().map(ConsumptionLogResponse::from).toList();
    }

    public List<ConsumptionLogResponse> findByCategory(LoadCategory category) {
        return repository.findByLoadCategory(category).stream().map(ConsumptionLogResponse::from).toList();
    }

    public ConsumptionLogResponse findById(String logId) {
        return ConsumptionLogResponse.from(findEntity(logId));
    }

    @Transactional
    public ConsumptionLogResponse create(ConsumptionLogRequest request) {
        ConsumptionLog log = new ConsumptionLog();
        applyRequest(log, request);
        return ConsumptionLogResponse.from(repository.save(log));
    }

    @Transactional
    public ConsumptionLogResponse update(String logId, ConsumptionLogRequest request) {
        ConsumptionLog log = findEntity(logId);
        applyRequest(log, request);
        return ConsumptionLogResponse.from(repository.save(log));
    }

    @Transactional
    public void delete(String logId) {
        repository.delete(findEntity(logId));
    }

    private ConsumptionLog findEntity(String logId) {
        return repository.findById(logId)
                .orElseThrow(() -> new EnergySupplyNotFoundException("Consumption log", logId));
    }

    private void applyRequest(ConsumptionLog log, ConsumptionLogRequest request) {
        log.setDate(request.date());
        log.setLoadCategory(request.loadCategory());
        log.setKwhConsumed(request.kwhConsumed());
        log.setSource(request.source());
    }
}
