package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.BlockVariety;
import com.infineonbit.sustainablefarm.modules.plants.exception.VarietyNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.BlockVarietyRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository.TreeBalance;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class VarietyService {

    private final BlockVarietyRepository blockVarietyRepository;
    private final PopulationEventRepository populationEventRepository;
    private final PlantsBlocks plantsBlocks;

    /**
     * Maps an entity to its API representation.
     *
     * <p>No stored value is derived or defaulted here: a NULL column stays a
     * {@code null} component, and the client decides how to display it.
     *
     * @param variety          the entity to map
     * @param currentTreeCount balance of the row's population events, or
     *                         {@code null} if it has none
     * @return the API representation of that variety
     */
    private static VarietyResponse toResponse(BlockVariety variety, Integer currentTreeCount) {
        return new VarietyResponse(
                variety.getId(),
                variety.getFarmId(),
                variety.getName(),
                variety.getTreeCount(),
                currentTreeCount,
                Decimals.toDouble(variety.getRowSpacingM()),
                Decimals.toDouble(variety.getTreeSpacingM()),
                Decimals.toDouble(variety.getTreeDensityPerHa()),
                variety.getVigor(),
                variety.getBlockCode(),
                variety.getPlantOrigin(),
                variety.getSource(),
                variety.getLastUpdated());
    }

    /**
     * Retrieves the varieties matching the optional filters.
     *
     * <p>Both filters are independent and optional. A filter that matches no
     * row is a normal outcome and returns an empty list; it is never an error.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode block code (for example {@code "A"}), or {@code null}
     *                  or blank for every block
     * @return the matching varieties ordered by block code, name and id, possibly empty
     */
    public List<VarietyResponse> getAllVarieties(Long farmId, String blockCode) {
        PlantsBlocks.BlockFilter blocks = plantsBlocks.filter(farmId, blockCode);
        List<BlockVariety> varieties = PlantsBlocks.sortedByBlockCode(
                blockVarietyRepository.findByOptionalFilters(blocks.all(), blocks.ids()), BlockVariety::getBlockCode);
        Map<Long, Integer> currentTreeCounts = currentTreeCounts(varieties);
        return varieties.stream()
                .map(variety -> toResponse(variety, currentTreeCounts.get(variety.getId())))
                .toList();
    }

    /**
     * Retrieves a single variety by its identifier.
     *
     * @param id the variety identifier
     * @return the representation of that variety
     * @throws VarietyNotFoundException if no variety exists with this ID
     */
    public VarietyResponse getVarietyById(Long id) {
        BlockVariety variety = blockVarietyRepository.findById(id)
                .orElseThrow(() -> new VarietyNotFoundException(id));
        return toResponse(variety, currentTreeCounts(List.of(variety)).get(variety.getId()));
    }

    /**
     * Current number of trees of each variety, from its population events.
     *
     * <p>One query for the whole list, not one per row. A variety without any
     * event has no entry in the map, so its {@code currentTreeCount} is
     * {@code null}.
     *
     * @param varieties the varieties about to be returned
     * @return the balance of each variety that has at least one event, by variety ID
     */
    private Map<Long, Integer> currentTreeCounts(List<BlockVariety> varieties) {
        if (varieties.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = varieties.stream().map(BlockVariety::getId).toList();
        return populationEventRepository.findTreeBalances(ids).stream()
                .collect(Collectors.toMap(
                        TreeBalance::getVarietyId,
                        balance -> Math.toIntExact(balance.getBalance())));
    }
}
