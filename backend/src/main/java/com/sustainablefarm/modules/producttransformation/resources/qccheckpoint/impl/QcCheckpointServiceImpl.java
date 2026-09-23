package com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.impl;

import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch;
import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint.QcResult;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint.QcStage;
import com.sustainablefarm.modules.producttransformation.resources.batch.repository.BatchRepository;
import com.sustainablefarm.modules.producttransformation.resources.operator.repository.OperatorRepository;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.repository.QcCheckpointRepository;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.service.QcCheckpointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Implementation for QcCheckpoint Entity
 * Quality control checkpoint data - Core Processing Entity
 * 
 * Business Rules:
 * - Mandatory at washing (Phase 2) and cooling (Phase 5) stages
 * - QC inspectors must be certified
 * - Result must be PASS for batch to proceed
 * - Failed checkpoints require rework
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@Transactional
public class QcCheckpointServiceImpl implements QcCheckpointService {

    private final QcCheckpointRepository qcCheckpointRepository;
    private final BatchRepository batchRepository;
    private final OperatorRepository operatorRepository;

    @Autowired
    public QcCheckpointServiceImpl(QcCheckpointRepository qcCheckpointRepository,
                                 BatchRepository batchRepository,
                                 OperatorRepository operatorRepository) {
        this.qcCheckpointRepository = qcCheckpointRepository;
        this.batchRepository = batchRepository;
        this.operatorRepository = operatorRepository;
    }

    @Override
    public QcCheckpoint createQcCheckpoint(QcCheckpoint qcCheckpoint) {
        // Business Rule: Validate inspector is certified
        if (qcCheckpoint.getInspector() != null) {
            Operator inspector = operatorRepository.findById(qcCheckpoint.getInspector().getOperatorId())
                    .orElseThrow(() -> new IllegalArgumentException("Inspector not found"));
            
            if (inspector.getRole() != Operator.Role.QC_INSPECTOR) {
                throw new IllegalArgumentException(
                    "Inspector must have QC_INSPECTOR role. Current role: " + inspector.getRole()
                );
            }
        }
        
        return qcCheckpointRepository.save(qcCheckpoint);
    }

    @Override
    public QcCheckpoint updateQcCheckpoint(String checkpointId, QcCheckpoint qcCheckpoint) {
        QcCheckpoint existingCheckpoint = getQcCheckpointById(checkpointId);
        
        // Update fields
        existingCheckpoint.setStage(qcCheckpoint.getStage());
        existingCheckpoint.setResult(qcCheckpoint.getResult());
        existingCheckpoint.setDefects(qcCheckpoint.getDefects());
        existingCheckpoint.setDefectsCount(qcCheckpoint.getDefectsCount());
        existingCheckpoint.setInspector(qcCheckpoint.getInspector());
        existingCheckpoint.setCheckpointTime(qcCheckpoint.getCheckpointTime());
        existingCheckpoint.setNotes(qcCheckpoint.getNotes());
        
        return qcCheckpointRepository.save(existingCheckpoint);
    }

    @Override
    public void deleteQcCheckpoint(String checkpointId) {
        if (!qcCheckpointRepository.existsById(checkpointId)) {
            throw new IllegalArgumentException("QC checkpoint not found with ID: " + checkpointId);
        }
        qcCheckpointRepository.deleteById(checkpointId);
    }

    @Override
    @Transactional(readOnly = true)
    public QcCheckpoint getQcCheckpointById(String checkpointId) {
        return qcCheckpointRepository.findById(checkpointId)
                .orElseThrow(() -> new IllegalArgumentException("QC checkpoint not found with ID: " + checkpointId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QcCheckpoint> getAllQcCheckpoints() {
        return qcCheckpointRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QcCheckpoint> getQcCheckpointsByBatch(String batchId) {
        return qcCheckpointRepository.findByBatchBatchId(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QcCheckpoint> getQcCheckpointsByStage(QcStage stage) {
        return qcCheckpointRepository.findByStage(stage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QcCheckpoint> getQcCheckpointsByResult(QcResult result) {
        return qcCheckpointRepository.findByResult(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QcCheckpoint> getQcCheckpointsByInspector(String inspectorId) {
        return qcCheckpointRepository.findByInspectorOperatorId(inspectorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QcCheckpoint> getQcCheckpointsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return qcCheckpointRepository.findByCheckpointTimeBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QcCheckpoint> getQcCheckpointsByBatchAndStage(String batchId, QcStage stage) {
        return qcCheckpointRepository.findByBatchAndStage(batchId, stage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QcCheckpoint> getQcCheckpointsByBatchAndResult(String batchId, QcResult result) {
        return qcCheckpointRepository.findByBatchAndResult(batchId, result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QcCheckpoint> getMandatoryCheckpoints() {
        // Business Rule: Mandatory at washing (Phase 2) and cooling (Phase 5) stages
        return qcCheckpointRepository.findMandatoryCheckpoints();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QcCheckpoint> getFailedCheckpoints() {
        return qcCheckpointRepository.findFailedCheckpoints();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFailedByBatch(String batchId) {
        return qcCheckpointRepository.countFailedByBatch(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByBatchAndStage(String batchId, QcStage stage) {
        return qcCheckpointRepository.countByBatchAndStage(batchId, stage);
    }

    @Override
    @Transactional(readOnly = true)
    public double getPassRateByStage(QcStage stage) {
        long passed = qcCheckpointRepository.countPassedByStage(stage);
        long total = qcCheckpointRepository.countTotalByStage(stage);
        
        if (total == 0) {
            return 0.0;
        }
        
        return (double) passed / total * 100;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasMandatoryCheckpointsCompleted(String batchId) {
        // Business Rule: Mandatory at washing (Phase 2) and cooling (Phase 5) stages
        long washingCount = qcCheckpointRepository.countByBatchAndStage(batchId, QcStage.WASHING);
        long coolingCount = qcCheckpointRepository.countByBatchAndStage(batchId, QcStage.COOLING);
        
        return washingCount > 0 && coolingCount > 0;
    }

    @Override
    public QcCheckpoint createMandatoryCheckpoint(String batchId, QcStage stage, String inspectorId) {
        // Business Rule: Validate stage is mandatory
        if (stage != QcStage.WASHING && stage != QcStage.COOLING) {
            throw new IllegalArgumentException(
                "Stage must be WASHING or COOLING for mandatory checkpoint. Current: " + stage
            );
        }
        
        // Validate batch exists
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));
        
        // Validate inspector exists and has correct role
        Operator inspector = operatorRepository.findById(inspectorId)
                .orElseThrow(() -> new IllegalArgumentException("Inspector not found with ID: " + inspectorId));
        
        if (inspector.getRole() != Operator.Role.QC_INSPECTOR) {
            throw new IllegalArgumentException(
                "Inspector must have QC_INSPECTOR role. Current role: " + inspector.getRole()
            );
        }
        
        // Create mandatory checkpoint
        QcCheckpoint checkpoint = new QcCheckpoint();
        checkpoint.setBatch(batch);
        checkpoint.setStage(stage);
        checkpoint.setInspector(inspector);
        checkpoint.setCheckpointTime(LocalDateTime.now());
        checkpoint.setResult(QcResult.PENDING);
        checkpoint.setDefectsCount(0);
        
        return qcCheckpointRepository.save(checkpoint);
    }
}