package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.exception.VarietyNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository.TreeBalance;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class VarietyService {

    private final VarietyRepository varietyRepository;
    private final PopulationEventRepository populationEventRepository;

    /**
     * Turns a blank filter into no filter at all.
     *
     * <p>A filter sent as an empty or whitespace-only string means "no filter".
     * It is turned into {@code null} so the query ignores it instead of looking
     * for a variety whose block is literally the empty string.
     *
     * @param value the filter value as received, possibly {@code null}
     * @return the trimmed value, or {@code null} if it was null or blank
     */
    private static String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

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
    private static VarietyResponse toResponse(Variety variety, Integer currentTreeCount) {
        return new VarietyResponse(
                variety.getId(),
                variety.getFarmId(),
                variety.getName(),
                variety.getTreeCount(),
                currentTreeCount,
                variety.getRowSpacingM(),
                variety.getTreeSpacingM(),
                variety.getTreeDensityPerHa(),
                variety.getExpectedYieldKg(),
                variety.getActualYieldKg(),
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
     * @param blockCode raw block value as stored (for example {@code "A"}),
     *                  or {@code null} for every block
     * @return the matching varieties, possibly empty
     */
    public List<VarietyResponse> getAllVarieties(Integer farmId, String blockCode) {
        List<Variety> varieties = varietyRepository.findByOptionalFilters(
                farmId,
                normalizeFilter(blockCode));
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
        Variety variety = varietyRepository.findById(id)
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
    private Map<Long, Integer> currentTreeCounts(List<Variety> varieties) {
        if (varieties.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = varieties.stream().map(Variety::getId).toList();
        return populationEventRepository.findTreeBalances(ids).stream()
                .collect(Collectors.toMap(
                        TreeBalance::getVarietyId,
                        balance -> Math.toIntExact(balance.getBalance())));
    }
}
