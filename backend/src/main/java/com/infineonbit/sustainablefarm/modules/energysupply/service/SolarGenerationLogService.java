package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.SolarGenerationLogRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.SolarGenerationLogResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.SolarGenerationLog;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.SolarGenerationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service("energySolarGenerationLogService")
@Transactional(readOnly = true)
public class SolarGenerationLogService {

    private final SolarGenerationLogRepository solarGenerationLogRepository;
    private final EnergyComponentRepository energyComponentRepository;

    public SolarGenerationLogService(
            SolarGenerationLogRepository solarGenerationLogRepository,
            EnergyComponentRepository energyComponentRepository) {
        this.solarGenerationLogRepository = solarGenerationLogRepository;
        this.energyComponentRepository = energyComponentRepository;
    }

    public List<SolarGenerationLogResponse> findAll() {
        return solarGenerationLogRepository.findAll().stream().map(SolarGenerationLogResponse::from).toList();
    }

    public List<SolarGenerationLogResponse> findLast7Days() {
        return solarGenerationLogRepository.findByDateBetweenOrderByDateAsc(
                        LocalDate.now().minusDays(6), LocalDate.now())
                .stream().map(SolarGenerationLogResponse::from).toList();
    }

    public SolarGenerationLogResponse findById(String logId) {
        return SolarGenerationLogResponse.from(findEntity(logId));
    }

    @Transactional
    public SolarGenerationLogResponse create(SolarGenerationLogRequest request) {
        SolarGenerationLog log = new SolarGenerationLog();
        applyRequest(log, request);
        return SolarGenerationLogResponse.from(solarGenerationLogRepository.save(log));
    }

    @Transactional
    public SolarGenerationLogResponse update(String logId, SolarGenerationLogRequest request) {
        SolarGenerationLog log = findEntity(logId);
        applyRequest(log, request);
        return SolarGenerationLogResponse.from(solarGenerationLogRepository.save(log));
    }

    @Transactional
    public void delete(String logId) {
        solarGenerationLogRepository.delete(findEntity(logId));
    }

    private SolarGenerationLog findEntity(String logId) {
        return solarGenerationLogRepository.findById(logId)
                .orElseThrow(() -> new EnergySupplyNotFoundException("Solar generation log", logId));
    }

    private void applyRequest(SolarGenerationLog log, SolarGenerationLogRequest request) {
        EnergyComponent component = energyComponentRepository.findById(request.componentId())
                .orElseThrow(() -> new EnergySupplyNotFoundException("Energy component", request.componentId()));
        log.setComponent(component);
        log.setDate(request.date());
        log.setKwhProduced(request.kwhProduced());
        log.setEfficiencyPct(request.efficiencyPct());
    }
}
