package com.infineonbit.sustainablefarm.modules.plants.support;

import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryBatch;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryOrigin;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Nursery rows built in memory for the unit tests, with the arguments of the former all-args
 * constructors. A block code is set by hand, as a row read from the farm referential shows it.
 */
public final class NurseryRows {
    private NurseryRows() {
    }

    public static NurseryBatch batch(Long id, Long farmId, String batchCode, String varietyName, NurseryOrigin origin,
                                     String supplier, String supplierLotNumber, LocalDate startedOn,
                                     Integer initialCount, LocalDate plannedTransplantOn, String plannedBlockCode,
                                     String source, Instant lastUpdated) {
        NurseryBatch batch = new NurseryBatch();
        batch.setId(id);
        batch.setFarmId(farmId);
        batch.setBatchCode(batchCode);
        batch.setVarietyName(varietyName);
        batch.setOrigin(origin);
        batch.setSupplier(supplier);
        batch.setSupplierLotNumber(supplierLotNumber);
        batch.setStartedOn(startedOn);
        batch.setInitialCount(initialCount);
        batch.setPlannedTransplantOn(plannedTransplantOn);
        if (plannedBlockCode != null) {
            batch.setPlannedBlockCode(plannedBlockCode);
        }
        batch.setSource(source);
        batch.setLastUpdated(lastUpdated);
        return batch;
    }

    public static NurseryEvent event(Long id, NurseryBatch batch, NurseryEventType eventType, LocalDate eventDate,
                                     NurseryStage stage, Integer quantity, String reason, String blockCode,
                                     PopulationEvent populationEvent, String source, Instant lastUpdated) {
        NurseryEvent event = new NurseryEvent();
        event.setId(id);
        event.setBatch(batch);
        event.setEventType(eventType);
        event.setEventDate(eventDate);
        event.setStage(stage);
        event.setQuantity(quantity);
        event.setReason(reason);
        if (blockCode != null) {
            event.setBlockCode(blockCode);
        }
        event.setPopulationEvent(populationEvent);
        event.setSource(source);
        event.setLastUpdated(lastUpdated);
        return event;
    }
}
