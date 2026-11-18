package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service;

import com.infineonbit.sustainablefarm.modules.producttransformation.block.ProductTransformationBlocks;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.variety.MangoVarietyCodes;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.api.HarvestLookup;
import com.infineonbit.sustainablefarm.modules.plants.api.HarvestSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Plants Integration Service
 * Reads the harvests of the Plants module through its public API
 * ({@link HarvestLookup}, docs/adr/0003-communication-entre-modules.md), in the
 * same process, and maps them to the Product Transformation contract
 * ({@link PlantsHarvestResponse}: id, farmId, blockCode, varietyId, varietyName,
 * harvestDate, quantityKg, source, lastUpdated, in camelCase).
 *
 * <p>An unknown harvest id is a 404 ({@link ResourceNotFoundException}); a farm id
 * outside the range Plants stores is a 400.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.1.0
 */
// Explicit bean name: the watersupply module ships its own
// PlantsIntegrationService, and two @Service classes with the same simple
// name make the Spring context fail (ConflictingBeanDefinitionException).
@Service("productTransformationPlantsIntegrationService")
public class PlantsIntegrationService {

    private final HarvestLookup harvestLookup;
    private final MangoVarietyCodes mangoVarietyCodes;
    private final ProductTransformationBlocks blocks;

    public PlantsIntegrationService(HarvestLookup harvestLookup, MangoVarietyCodes mangoVarietyCodes,
                                    ProductTransformationBlocks blocks) {
        this.harvestLookup = harvestLookup;
        this.mangoVarietyCodes = mangoVarietyCodes;
        this.blocks = blocks;
    }

    /**
     * Fetch all harvests from Plants module
     *
     * @return List of harvests from Plants
     */
    public List<PlantsHarvestResponse> getAllHarvests() {
        return toResponses(harvestLookup.listHarvests(null, null));
    }

    /**
     * Fetch harvest by ID from Plants module
     *
     * @param harvestId Harvest ID from Plants
     * @return Harvest data from Plants
     * @throws ResourceNotFoundException if Plants has no harvest with this ID
     */
    public PlantsHarvestResponse getHarvestById(Long harvestId) {
        return harvestLookup.findHarvest(harvestId)
                .map(PlantsIntegrationService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Plants harvest " + harvestId + " not found"));
    }

    /**
     * Fetch harvests by farm ID from Plants module
     *
     * @param farmId Farm ID
     * @return List of harvests from Plants for the specified farm
     */
    public List<PlantsHarvestResponse> getHarvestsByFarm(Long farmId) {
        return toResponses(harvestLookup.listHarvests(farmId, null));
    }

    /**
     * Fetch harvests by block code from Plants module
     *
     * @param blockCode Block code (e.g., "A", "B", "C")
     * @return List of harvests from Plants for the specified block
     */
    public List<PlantsHarvestResponse> getHarvestsByBlock(String blockCode) {
        return toResponses(harvestLookup.listHarvests(null, blockCode));
    }

    private static List<PlantsHarvestResponse> toResponses(List<HarvestSummary> harvests) {
        return harvests.stream().map(PlantsIntegrationService::toResponse).toList();
    }

    /** The Plants harvest in the Product Transformation contract, field for field. */
    static PlantsHarvestResponse toResponse(HarvestSummary harvest) {
        return PlantsHarvestResponse.builder()
                .id(harvest.id())
                .farmId(harvest.farmId())
                .blockCode(harvest.blockCode())
                .varietyId(harvest.varietyId())
                .varietyName(harvest.varietyName())
                .harvestDate(harvest.harvestDate() != null ? harvest.harvestDate().toString() : null)
                .quantityKg(harvest.quantityKg())
                .source(harvest.source())
                .lastUpdated(harvest.lastUpdated())
                .build();
    }

    /**
     * Map a Plants harvest to the Product Transformation HarvestEvent.
     *
     * <p>The batch/lot identifier is supplied by the caller and is stored
     * as-is: this method never regenerates it, so the original lot number of
     * the harvest is preserved across imports. The Plants record id is kept in
     * {@code plantsHarvestId} to keep the cross-module link.
     *
     * <p>The variety name comes back from Plants as the planted name
     * ("Keitt", "keitt", "Amélie"...), so it is resolved to a catalogue key
     * through the plants variety catalogue
     * ({@link MangoVarietyCodes#keyOfPlantsName(String)}); an unknown name gives
     * {@code OTHER}.
     *
     * <p>The quality grade is not part of the Plants harvest contract: the
     * caller remains responsible for it and must set it before persisting.
     * For the same reason {@code harvestId} (the harvest event primary key)
     * is left untouched: it is a Product Transformation identifier, distinct
     * from the batch id (see {@code DtoMapper}), and remains the caller's
     * responsibility.
     *
     * @param harvest          harvest returned by the Plants module
     * @param originalBatchId  original lot/batch identifier to preserve
     * @return a HarvestEvent carrying the Plants identity and values
     */
    public HarvestEvent toHarvestEvent(PlantsHarvestResponse harvest, String originalBatchId) {
        if (harvest == null) {
            throw new IllegalArgumentException("Plants harvest must not be null");
        }
        if (originalBatchId == null || originalBatchId.isBlank()) {
            throw new IllegalArgumentException("originalBatchId (Plants lot/batch) is required");
        }
        if (harvest.getHarvestDate() == null || harvest.getHarvestDate().isBlank()) {
            throw new IllegalArgumentException("Plants harvest date is required");
        }

        HarvestEvent event = new HarvestEvent();
        event.setPlantsHarvestId(harvest.getId());
        event.setBatchId(originalBatchId.trim());
        event.setHarvestDate(LocalDate.parse(harvest.getHarvestDate()));
        event.setVarietyKey(mangoVarietyCodes.keyOfPlantsName(harvest.getVarietyName()));
        // A Plants block code is the code of a farm.block row: the same block for both modules.
        event.setFarmBlockId(blocks.resolve(harvest.getFarmId() != null ? String.valueOf(harvest.getFarmId()) : null,
                harvest.getBlockCode()));
        event.setHarvestQuantityKg(harvest.getQuantityKg() != null
                ? BigDecimal.valueOf(harvest.getQuantityKg())
                : null);
        return event;
    }
}
