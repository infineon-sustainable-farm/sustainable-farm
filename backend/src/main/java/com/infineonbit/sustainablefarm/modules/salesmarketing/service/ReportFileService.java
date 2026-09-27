package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ReportFile;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ReportFileRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportFileService {

    private final ReportFileRepository repository;

    public ReportFileService(ReportFileRepository repository) {
        this.repository = repository;
    }

    public List<ReportFile> getAll() {
        return repository.findAll();
    }

    public ReportFile getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ReportFile not found with id: " + id));
    }

    public ReportFile create(ReportFile item) {
        return repository.save(item);
    }

    public ReportFile update(Integer id, ReportFile updatedData) {
        ReportFile existing = getById(id);

        existing.setReportId(updatedData.getReportId());
        existing.setFormat(updatedData.getFormat());
        existing.setFileUrl(updatedData.getFileUrl());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
