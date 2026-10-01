package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.QualityCheck;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.QualityCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QualityCheckService {

    private final QualityCheckRepository qualityCheckRepository;

    public QualityCheck create(QualityCheck check) {
        return qualityCheckRepository.save(check);
    }

    public List<QualityCheck> findAll() {
        return qualityCheckRepository.findAll();
    }

    public List<QualityCheck> findByBatch(Long batchId) {
        return qualityCheckRepository.findByBatchIdOrderByCheckedAtDesc(batchId);
    }
}