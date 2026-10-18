package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryBatchRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryBatchResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryBatch;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryOrigin;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;
import com.infineonbit.sustainablefarm.modules.plants.exception.NurseryBatchNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.NurseryBatchRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.NurseryEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.service.NurseryBatchCalculator.BatchSummary;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class NurseryBatchService {

    /** {@code source} of every batch and event written through the API. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    private final NurseryBatchRepository nurseryBatchRepository;
    private final NurseryEventRepository nurseryEventRepository;

    /**
     * A code as stored: trimmed and upper-cased, so {@code " p1 "} becomes
     * {@code "P1"}. The request validation already refused any other form.
     */
    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * An optional text as stored: trimmed, and {@code null} when it is blank.
     *
     * @param value the text as received, possibly {@code null}
     * @return the trimmed text, or {@code null} if it was null or blank
     */
    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** An optional code as stored: trimmed and upper-cased, {@code null} when blank. */
    private static String normalizeOptionalCode(String value) {
        String text = normalizeOptionalText(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }

    /**
     * Maps a stored batch to its API representation, with the values computed
     * from its events.
     *
     * @param batch  the stored batch
     * @param events its events, in any order
     * @return the API representation of that batch
     */
    private static NurseryBatchResponse toResponse(NurseryBatch batch, List<NurseryEvent> events) {
        BatchSummary summary = NurseryBatchCalculator.summarize(batch.getInitialCount(), events);
        return new NurseryBatchResponse(
                batch.getId(),
                batch.getFarmId(),
                batch.getBatchCode(),
                batch.getVarietyName(),
                batch.getOrigin(),
                batch.getSupplier(),
                batch.getSupplierLotNumber(),
                batch.getStartedOn(),
                batch.getInitialCount(),
                batch.getPlannedTransplantOn(),
                batch.getPlannedBlockCode(),
                summary.currentStage(),
                summary.currentStageSince(),
                summary.lossCount(),
                summary.transplantedCount(),
                summary.currentCount(),
                summary.survivedCount(),
                summary.survivalRatePct().doubleValue(),
                batch.getSource(),
                batch.getLastUpdated());
    }

    /**
     * Maps stored batches to their API representation, in the given order. The
     * events of every batch come from one query, skipped when there is no batch.
     */
    private List<NurseryBatchResponse> toResponses(List<NurseryBatch> batches) {
        if (batches.isEmpty()) {
            return List.of();
        }
        Map<Long, List<NurseryEvent>> eventsByBatch = nurseryEventRepository
                .findByBatchIds(batches.stream().map(NurseryBatch::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(event -> event.getBatch().getId()));
        return batches.stream()
                .map(batch -> toResponse(batch, eventsByBatch.getOrDefault(batch.getId(), List.of())))
                .toList();
    }

    /**
     * Starts a batch of young plants, in a single transaction.
     *
     * <ol>
     *     <li>Refuses a code already used by a batch of the same farm, ignoring
     *         case; a missing farm is a farm of its own.</li>
     *     <li>Refuses a planned transplant date before the start date.</li>
     *     <li>Stores the batch, then its initial stage as its first stage
     *         change, dated on the start date.</li>
     * </ol>
     * Both refusals come before anything is written.
     *
     * @param request the batch, already validated
     * @return the created batch, with its computed values
     * @throws ConflictException     if the farm already has a batch with this code
     * @throws BusinessRuleException if the planned transplant date is before the start date
     */
    @Transactional
    public NurseryBatchResponse createBatch(NurseryBatchRequest request) {
        return createBatch(request, Instant.now());
    }

    /**
     * Same as {@link #createBatch(NurseryBatchRequest)}, at an explicit write
     * time so the {@code lastUpdated} values can be tested.
     */
    NurseryBatchResponse createBatch(NurseryBatchRequest request, Instant now) {
        String batchCode = normalizeCode(request.batchCode());
        if (!nurseryBatchRepository.findByFarmAndCode(request.farmId(), batchCode).isEmpty()) {
            throw new ConflictException("A nursery batch with code " + batchCode + " already exists");
        }
        if (request.plannedTransplantOn().isBefore(request.startedOn())) {
            throw new BusinessRuleException("The planned transplant date " + request.plannedTransplantOn()
                    + " is before the start date " + request.startedOn() + " of batch " + batchCode);
        }

        NurseryBatch batch = new NurseryBatch();
        batch.setFarmId(request.farmId());
        batch.setBatchCode(batchCode);
        batch.setVarietyName(request.varietyName().trim());
        batch.setOrigin(NurseryOrigin.valueOf(normalizeCode(request.origin())));
        batch.setSupplier(normalizeOptionalText(request.supplier()));
        batch.setSupplierLotNumber(normalizeOptionalText(request.supplierLotNumber()));
        batch.setStartedOn(request.startedOn());
        // A whole number of 1 or more: the request validation refused anything else.
        batch.setInitialCount(request.initialCount().intValue());
        batch.setPlannedTransplantOn(request.plannedTransplantOn());
        batch.setPlannedBlockCode(normalizeOptionalCode(request.plannedBlockCode()));
        batch.setSource(USER_ENTRY_SOURCE);
        batch.setLastUpdated(now);
        NurseryBatch savedBatch = nurseryBatchRepository.save(batch);

        NurseryEvent initialStage = new NurseryEvent();
        initialStage.setBatch(savedBatch);
        initialStage.setEventType(NurseryEventType.STAGE_CHANGE);
        initialStage.setEventDate(savedBatch.getStartedOn());
        initialStage.setStage(NurseryStage.valueOf(normalizeCode(request.initialStage())));
        initialStage.setSource(USER_ENTRY_SOURCE);
        initialStage.setLastUpdated(now);
        return toResponse(savedBatch, List.of(nurseryEventRepository.save(initialStage)));
    }

    /**
     * Retrieves the batches, each with its computed values.
     *
     * <p>A missing farm means every farm, as for the other lists of the module.
     * The current stage is computed from the events, so its filter applies
     * after that, in Java.
     *
     * @param farmId farm identifier, or {@code null} for every farm
     * @param stage  current stage, or {@code null} for every stage
     * @return the matching batches, ordered by start date then identifier
     */
    public List<NurseryBatchResponse> getAllBatches(Integer farmId, NurseryStage stage) {
        return toResponses(nurseryBatchRepository.findByOptionalFarm(farmId)).stream()
                .filter(batch -> stage == null || batch.currentStage() == stage)
                .toList();
    }

    /**
     * Retrieves one batch with its computed values.
     *
     * @param id the batch identifier
     * @return the batch
     * @throws NurseryBatchNotFoundException if no batch has this identifier
     */
    public NurseryBatchResponse getBatchById(Long id) {
        NurseryBatch batch = nurseryBatchRepository.findById(id)
                .orElseThrow(() -> new NurseryBatchNotFoundException(id));
        return toResponse(batch, nurseryEventRepository.findByBatchIds(List.of(id)));
    }
}
