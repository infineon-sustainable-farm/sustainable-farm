package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryLossRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.StageChangeRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.TransplantRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryEventResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantingResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryBatch;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.exception.NurseryBatchNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.NurseryBatchRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.NurseryEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.service.NurseryBatchCalculator.BatchSummary;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class NurseryEventService {

    /** {@code source} of every event written through the API. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    private final NurseryBatchRepository nurseryBatchRepository;
    private final NurseryEventRepository nurseryEventRepository;
    private final PopulationEventRepository populationEventRepository;
    private final PlantingService plantingService;

    /**
     * A code as the enums spell it: trimmed and upper-cased, so
     * {@code " grafted "} becomes {@code "GRAFTED"}. The request validation
     * already refused any other value.
     */
    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Maps a stored event to its API representation. The farm and the code are
     * those of its batch.
     *
     * @param event the stored event, with its batch
     * @return the API representation of that event
     */
    private static NurseryEventResponse toResponse(NurseryEvent event) {
        NurseryBatch batch = event.getBatch();
        PopulationEvent planting = event.getPopulationEvent();
        return new NurseryEventResponse(
                event.getId(),
                batch.getId(),
                batch.getBatchCode(),
                batch.getFarmId(),
                event.getEventType(),
                event.getEventDate(),
                event.getStage(),
                event.getQuantity(),
                event.getReason(),
                event.getBlockCode(),
                planting == null ? null : planting.getId(),
                event.getSource(),
                event.getLastUpdated());
    }

    /** A new event of a batch, with the columns shared by every type. */
    private static NurseryEvent newEvent(NurseryBatch batch, NurseryEventType type, LocalDate date, Instant now) {
        NurseryEvent event = new NurseryEvent();
        event.setBatch(batch);
        event.setEventType(type);
        event.setEventDate(date);
        event.setSource(USER_ENTRY_SOURCE);
        event.setLastUpdated(now);
        return event;
    }

    /**
     * Finds a batch and locks its row until the end of the transaction, so the
     * writes to one batch run one after the other and each one reads the
     * events of the previous ones.
     *
     * @throws NurseryBatchNotFoundException if no batch has this identifier
     */
    private NurseryBatch lockBatch(Long batchId) {
        return nurseryBatchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new NurseryBatchNotFoundException(batchId));
    }

    /** What the events of a batch add up to, read after its row is locked. */
    private BatchSummary summarize(NurseryBatch batch) {
        return NurseryBatchCalculator.summarize(batch.getInitialCount(),
                nurseryEventRepository.findByBatchIds(List.of(batch.getId())));
    }

    /**
     * Refuses an event dated before the start of its batch.
     *
     * @param batch the batch
     * @param what  the event as the message names it, for example {@code "loss"}
     * @param date  the date of the event
     * @throws BusinessRuleException if the date is before the start date
     */
    private static void requireNotBeforeStart(NurseryBatch batch, String what, LocalDate date) {
        if (date.isBefore(batch.getStartedOn())) {
            throw new BusinessRuleException("The " + what + " date " + date + " is before the start date "
                    + batch.getStartedOn() + " of batch " + batch.getBatchCode());
        }
    }

    /**
     * Refuses a loss or a transplant larger than the plants left in the
     * nursery. The check uses the count today, not the count on the date of the
     * event, as for the stock of a fertilizer.
     *
     * @throws BusinessRuleException if the quantity is larger than the plants left
     */
    private static void requirePlants(NurseryBatch batch, BatchSummary summary, int quantity) {
        if (quantity > summary.currentCount()) {
            throw new BusinessRuleException(NurseryBatchCalculator.notEnoughPlantsMessage(
                    batch.getBatchCode(), summary.currentCount(), quantity));
        }
    }

    /**
     * Records that a batch reached a stage.
     *
     * <p>Any stage can follow any other: a failed graft takes the batch back to
     * ROOTSTOCK_GROWTH. The checks come in this order, before anything is
     * written: the date against the start of the batch, then against its last
     * stage change, the same day being accepted, then the stage against the
     * current one.
     *
     * @param batchId the batch
     * @param request the stage change, already validated
     * @return the recorded stage change
     * @throws NurseryBatchNotFoundException if no batch has this identifier
     * @throws BusinessRuleException         if the date is before the start or the
     *                                       last stage change, or the batch is
     *                                       already at this stage
     */
    @Transactional
    public NurseryEventResponse recordStageChange(Long batchId, StageChangeRequest request) {
        return recordStageChange(batchId, request, Instant.now());
    }

    /**
     * Same as {@link #recordStageChange(Long, StageChangeRequest)}, at an
     * explicit write time so the {@code lastUpdated} value can be tested.
     */
    NurseryEventResponse recordStageChange(Long batchId, StageChangeRequest request, Instant now) {
        NurseryBatch batch = lockBatch(batchId);
        requireNotBeforeStart(batch, "stage change", request.changedOn());
        BatchSummary summary = summarize(batch);
        if (summary.currentStageSince() != null && request.changedOn().isBefore(summary.currentStageSince())) {
            throw new BusinessRuleException("The stage change date " + request.changedOn()
                    + " is before the last stage change " + summary.currentStageSince()
                    + " of batch " + batch.getBatchCode());
        }
        NurseryStage stage = NurseryStage.valueOf(normalizeCode(request.stage()));
        if (stage == summary.currentStage()) {
            throw new BusinessRuleException("Batch " + batch.getBatchCode() + " is already at stage " + stage);
        }

        NurseryEvent stageChange = newEvent(batch, NurseryEventType.STAGE_CHANGE, request.changedOn(), now);
        stageChange.setStage(stage);
        return toResponse(nurseryEventRepository.save(stageChange));
    }

    /**
     * Records plants of a batch lost, which leave the nursery and do not count
     * as survivors.
     *
     * <p>The checks come in this order, before anything is written: the date
     * against the start of the batch, then the quantity against the plants
     * left.
     *
     * @param batchId the batch
     * @param request the loss, already validated
     * @return the recorded loss
     * @throws NurseryBatchNotFoundException if no batch has this identifier
     * @throws BusinessRuleException         if the date is before the start, or
     *                                       the quantity larger than the plants left
     */
    @Transactional
    public NurseryEventResponse recordLoss(Long batchId, NurseryLossRequest request) {
        return recordLoss(batchId, request, Instant.now());
    }

    /**
     * Same as {@link #recordLoss(Long, NurseryLossRequest)}, at an explicit
     * write time so the {@code lastUpdated} value can be tested.
     */
    NurseryEventResponse recordLoss(Long batchId, NurseryLossRequest request, Instant now) {
        NurseryBatch batch = lockBatch(batchId);
        requireNotBeforeStart(batch, "loss", request.lostOn());
        // A whole number of 1 or more: the request validation refused anything else.
        int quantity = request.quantity().intValue();
        requirePlants(batch, summarize(batch), quantity);

        NurseryEvent loss = newEvent(batch, NurseryEventType.LOSS, request.lostOn(), now);
        loss.setQuantity(quantity);
        loss.setReason(request.reason().trim());
        return toResponse(nurseryEventRepository.save(loss));
    }

    /**
     * Transplants plants of a batch into the orchard, in a single transaction.
     *
     * <ol>
     *     <li>Refuses a date before the start of the batch, then a quantity
     *         larger than the plants left, before anything is written. The stage
     *         is not checked: a transplant that happened is recorded, whatever
     *         the stage of the batch.</li>
     *     <li>Records the planting of the variety of the batch on the block
     *         through {@link PlantingService}, with the farm of the batch and the
     *         date and quantity of the transplant. Its public method joins this
     *         transaction; it creates the variety row and the calendar date of
     *         the block as needed.</li>
     *     <li>Stores the TRANSPLANT event, linked to the PLANTING it created.</li>
     * </ol>
     * A refusal of the planting service, such as the 409 of a variety already
     * planted on the block, comes through unchanged and nothing is stored.
     *
     * @param batchId the batch
     * @param request the transplant, already validated
     * @return the recorded transplant, with the identifier of the planting
     * @throws NurseryBatchNotFoundException if no batch has this identifier
     * @throws BusinessRuleException         if the date is before the start, or
     *                                       the quantity larger than the plants left
     * @throws com.infineonbit.sustainablefarm.core.exception.ConflictException
     *         if the variety of the batch is already planted on the block
     */
    @Transactional
    public NurseryEventResponse recordTransplant(Long batchId, TransplantRequest request) {
        return recordTransplant(batchId, request, Instant.now());
    }

    /**
     * Same as {@link #recordTransplant(Long, TransplantRequest)}, at an explicit
     * write time so the {@code lastUpdated} value of the transplant can be
     * tested. The planting takes its own write time.
     */
    NurseryEventResponse recordTransplant(Long batchId, TransplantRequest request, Instant now) {
        NurseryBatch batch = lockBatch(batchId);
        requireNotBeforeStart(batch, "transplant", request.transplantedOn());
        // A whole number of 1 or more: the request validation refused anything else.
        int quantity = request.quantity().intValue();
        requirePlants(batch, summarize(batch), quantity);

        String blockCode = normalizeCode(request.blockCode());
        // Never caught: a refusal of the planting service has already marked the
        // transaction for rollback, and a caught exception would only turn into
        // a failed commit (UnexpectedRollbackException) instead of its own answer.
        PlantingResponse planting = plantingService.recordPlanting(new PlantingRequest(
                batch.getFarmId(), blockCode, batch.getVarietyName(), request.transplantedOn(), quantity));

        NurseryEvent transplant = newEvent(batch, NurseryEventType.TRANSPLANT, request.transplantedOn(), now);
        transplant.setQuantity(quantity);
        transplant.setBlockCode(blockCode);
        transplant.setPopulationEvent(populationEventRepository.getReferenceById(planting.id()));
        return toResponse(nurseryEventRepository.save(transplant));
    }

    /**
     * Retrieves the events matching the optional filters: the history of the
     * nursery.
     *
     * <p>Every filter is independent and optional; a missing farm means every
     * farm, as for the other lists of the module. The farm is that of the
     * batch. Both dates are included. A filter that matches nothing, such as an
     * unknown batch, or a {@code from} after {@code to}, returns an empty list
     * and is never an error.
     *
     * @param batchId   batch identifier, or {@code null} for every batch
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param eventType event type, or {@code null} for every type
     * @param from      first event date, included, or {@code null}
     * @param to        last event date, included, or {@code null}
     * @return the matching events, ordered by date then identifier
     */
    public List<NurseryEventResponse> getAllEvents(Long batchId, Integer farmId, NurseryEventType eventType,
                                                   LocalDate from, LocalDate to) {
        return nurseryEventRepository.findByOptionalFilters(batchId, farmId, eventType, from, to).stream()
                .map(NurseryEventService::toResponse)
                .toList();
    }
}
