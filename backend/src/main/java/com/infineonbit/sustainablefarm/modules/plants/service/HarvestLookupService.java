package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.api.HarvestLookup;
import com.infineonbit.sustainablefarm.modules.plants.api.HarvestSummary;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.exception.HarvestNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * The {@link HarvestLookup} of plants: the harvests of {@link HarvestService},
 * as {@code GET /api/plants/harvests} gives them.
 */
@Service
@Transactional(readOnly = true)
public class HarvestLookupService implements HarvestLookup {

    private final HarvestService harvestService;

    public HarvestLookupService(HarvestService harvestService) {
        this.harvestService = harvestService;
    }

    @Override
    public List<HarvestSummary> listHarvests(Long farmId, String blockCode) {
        return harvestService.getAllHarvests(farmId, blockCode, null, null, null).stream()
                .map(HarvestLookupService::toSummary)
                .toList();
    }

    @Override
    public Optional<HarvestSummary> findHarvest(Long id) {
        try {
            return Optional.of(toSummary(harvestService.getHarvestById(id)));
        } catch (HarvestNotFoundException notFound) {
            return Optional.empty();
        }
    }

    private static HarvestSummary toSummary(HarvestResponse harvest) {
        return new HarvestSummary(
                harvest.id(),
                harvest.farmId(),
                harvest.blockCode(),
                harvest.varietyId(),
                harvest.varietyName(),
                harvest.harvestDate(),
                harvest.quantityKg(),
                harvest.source(),
                harvest.lastUpdated());
    }
}
