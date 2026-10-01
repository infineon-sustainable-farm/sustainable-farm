package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.exception.VarietyNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class VarietyService {

    private final VarietyRepository varietyRepository;

    /**
     * Varieties Read Service
     * <p>Blank filter normalization method
     * <ul>
     *      <li>A filter sent as an empty or whitespace-only string means "no filter".</li>
     *      <li>It is turned into {@code null} so the query ignores it instead of
     *          looking for a variety whose block is literally the empty string.</li>
     * </ul>
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
     * <p>No value is derived or defaulted here: a NULL column stays a
     * {@code null} component, and the client decides how to display it.
     *
     * @param variety the entity to map
     * @return the API representation of that variety
     */
    private static VarietyResponse toResponse(Variety variety) {
        return new VarietyResponse(
                variety.getId(),
                variety.getFarmId(),
                variety.getName(),
                variety.getTreeCount(),
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
    public List<VarietyResponse> obtainAllVarieties(Integer farmId, String blockCode) {
        List<Variety> varieties = varietyRepository.findByOptionalFilters(
                farmId,
                normalizeFilter(blockCode));
        return varieties.stream().map(VarietyService::toResponse).toList();
    }

    /**
     * Retrieves a single variety by its identifier.
     *
     * @param id the variety identifier
     * @return the representation of that variety
     * @throws VarietyNotFoundException if no variety exists with this ID
     */
    public VarietyResponse obtainVarietyById(Long id) {
        Variety variety = varietyRepository.findById(id)
                .orElseThrow(() -> new VarietyNotFoundException(id));
        return toResponse(variety);
    }
}
