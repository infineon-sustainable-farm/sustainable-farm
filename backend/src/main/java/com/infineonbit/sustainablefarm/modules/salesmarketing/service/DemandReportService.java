package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandReport;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.DemandReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DemandReportService {

    private final DemandReportRepository repository;

    public DemandReportService(DemandReportRepository repository) {
        this.repository = repository;
    }

    public List<DemandReport> getAll() {
        return repository.findAll();
    }

    public DemandReport getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "DemandReport not found with id: " + id));
    }

    public DemandReport create(DemandReport report) {
        return repository.save(report);
    }

    public DemandReport update(Integer id, DemandReport updatedData) {
        DemandReport existing = getById(id);

        existing.setReportType(updatedData.getReportType());
        existing.setPeriodStart(updatedData.getPeriodStart());
        existing.setPeriodEnd(updatedData.getPeriodEnd());
        existing.setTotalForecastedVolumeT(
                updatedData.getTotalForecastedVolumeT());
        existing.setTotalActualVolumeT(
                updatedData.getTotalActualVolumeT());
        existing.setVariancePct(updatedData.getVariancePct());
        existing.setStatus(updatedData.getStatus());
        existing.setIsScheduled(updatedData.getIsScheduled());
        existing.setNextRunAt(updatedData.getNextRunAt());
        existing.setScheduleFrequency(updatedData.getScheduleFrequency());
        existing.setSummary(updatedData.getSummary());
        existing.setGeneratedBy(updatedData.getGeneratedBy());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}