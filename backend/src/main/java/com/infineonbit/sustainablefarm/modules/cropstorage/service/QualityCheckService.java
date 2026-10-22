package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.QualityCheck;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.QualityCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QualityCheckService {

    private final QualityCheckRepository qualityCheckRepository;
    private final BatchRepository batchRepository;

    /**
     * Saves a quality check; the batch arriving as a bare-id shell would be
     * read as unsaved since Batch is versioned, so the managed batch is
     * attached first, with a 404 if the id is unknown.
     */
    public QualityCheck create(QualityCheck check) {
        if (check.getBatch() != null && check.getBatch().getId() != null) {
            check.setBatch(batchRepository.findById(check.getBatch().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Batch not found with id " + check.getBatch().getId())));
        }
        return qualityCheckRepository.save(check);
    }

    public List<QualityCheck> findAll() {
        return qualityCheckRepository.findAll();
    }

    public List<QualityCheck> findByBatch(Long batchId) {
        return qualityCheckRepository.findByBatchIdOrderByCheckedAtDesc(batchId);
    }
}