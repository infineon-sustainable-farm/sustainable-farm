package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HarvestRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HarvestRecord;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.exception.HarvestNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.HarvestRecordRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class HarvestService {

    /** {@code source} of every harvest written through the API. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    private final VarietyRepository varietyRepository;
    private final PopulationEventRepository populationEventRepository;
    private final HarvestRecordRepository harvestRecordRepository;

    /**
     * Block code as stored: trimmed and upper-cased, so {@code " b "} becomes
     * {@code "B"}, as for a planting.
     *
     * @param blockCode the block code as received
     * @return the block code as stored
     */
    private static String normalizeBlockCode(String blockCode) {
        return blockCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Turns a blank filter into no filter at all, as for the other lists of the
     * module.
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
     * Maps a stored harvest to its API representation. The farm, block and name
     * are those of the variety row the harvest belongs to.
     *
     * @param harvest the stored harvest
     * @return the API representation of that harvest
     */
    private static HarvestResponse toResponse(HarvestRecord harvest) {
        Variety variety = harvest.getVariety();
        return new HarvestResponse(
                harvest.getId(),
                harvest.getLotNumber(),
                variety.getFarmId(),
                variety.getBlockCode(),
                variety.getId(),
                variety.getName(),
                harvest.getHarvestDate(),
                harvest.getQuantityKg(),
                harvest.getSource(),
                harvest.getLastUpdated());
    }

    /**
     * Refusal of a harvest whose variety has no recorded planting on the block.
     *
     * @param varietyName the stored name when the variety row exists, the name as
     *                    sent otherwise
     * @param blockCode   normalized block code
     * @return the exception to throw, answered with a 422
     */
    private static BusinessRuleException noPlanting(String varietyName, String blockCode) {
        return new BusinessRuleException("No planting of " + varietyName + " is recorded on block " + blockCode);
    }

    /**
     * Records a harvest of a variety planted on a block, in a single transaction.
     *
     * <ol>
     *     <li>Finds the variety row of the (farm, block, name) triple, as a
     *         planting does: a NULL farm matches only the rows without a farm,
     *         and the name ignores case but not accents.</li>
     *     <li>Refuses a harvest of a variety with no recorded planting.</li>
     *     <li>Refuses a harvest dated before the planting of that variety.</li>
     *     <li>Stores the harvest with the next lot number of its block for the
     *         year of the harvest date. Several harvests on the same day are
     *         allowed, one per picking, each with its own lot.</li>
     * </ol>
     *
     * @param request the harvest, already validated
     * @return the recorded harvest
     * @throws BusinessRuleException if the variety has no planting on that block,
     *                               or if the harvest predates the planting
     */
    @Transactional
    public HarvestResponse recordHarvest(HarvestRequest request) {
        return recordHarvest(request, Instant.now());
    }

    /**
     * Same as {@link #recordHarvest(HarvestRequest)}, at an explicit write time
     * so the {@code lastUpdated} value can be tested.
     */
    HarvestResponse recordHarvest(HarvestRequest request, Instant now) {
        Integer farmId = request.farmId();
        String blockCode = normalizeBlockCode(request.blockCode());
        String varietyName = request.varietyName().trim();

        Variety variety = varietyRepository.findByFarmBlockAndName(farmId, blockCode, varietyName)
                .stream()
                .findFirst()
                .orElse(null);
        if (variety == null) {
            throw noPlanting(varietyName, blockCode);
        }
        PopulationEvent planting = populationEventRepository
                .findFirstByVarietyIdAndEventTypeOrderByEventDateAsc(variety.getId(), PopulationEventType.PLANTING)
                .orElseThrow(() -> noPlanting(variety.getName(), blockCode));
        if (request.harvestDate().isBefore(planting.getEventDate())) {
            throw new BusinessRuleException("The harvest date " + request.harvestDate()
                    + " is before the planting date " + planting.getEventDate()
                    + " of " + variety.getName() + " on block " + variety.getBlockCode());
        }

        HarvestRecord harvest = new HarvestRecord();
        harvest.setLotNumber(nextLotNumber(variety.getBlockCode(), request.harvestDate().getYear()));
        harvest.setVariety(variety);
        harvest.setHarvestDate(request.harvestDate());
        harvest.setQuantityKg(request.quantityKg());
        harvest.setSource(USER_ENTRY_SOURCE);
        harvest.setLastUpdated(now);
        return toResponse(harvestRecordRepository.save(harvest));
    }

    /**
     * Lot number of the next harvest of a block for a year: one more than the
     * highest sequence already given to that block and year, or 1 for the
     * first one. The highest, not the count, so a missing number never leads
     * to a duplicate. The sequence follows the order in which the harvests are
     * recorded, not their dates.
     *
     * <p>Two harvests of the block recorded at the same time can compute the
     * same number: the unique constraint on the lot number refuses the second
     * one, which {@link ConcurrentPlantingRetry} runs once more.
     *
     * @param blockCode block code as stored, for example {@code "A"}
     * @param year      year of the harvest date, not of the day it is recorded
     * @return the lot number, for example {@code "LOT-2026-A-003"}
     */
    String nextLotNumber(String blockCode, int year) {
        String prefix = HarvestRecord.lotPrefixOf(year, blockCode);
        int highest = harvestRecordRepository.findLotNumbersStartingWith(prefix).stream()
                .mapToInt(lotNumber -> Integer.parseInt(lotNumber.substring(prefix.length())))
                .max()
                .orElse(0);
        return HarvestRecord.lotNumberOf(year, blockCode, highest + 1);
    }

    /**
     * Retrieves the harvests matching the optional filters.
     *
     * <p>Every filter is independent and optional; a missing farm means every
     * farm, as for the other lists of the module. Both dates are included. A
     * filter that matches nothing, or a {@code from} after {@code to}, returns
     * an empty list and is never an error.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode raw block value as stored (for example {@code "A"}),
     *                  or {@code null} for every block
     * @param from      first harvest date, included, or {@code null}
     * @param to        last harvest date, included, or {@code null}
     * @return the matching harvests, ordered by date then identifier
     */
    public List<HarvestResponse> getAllHarvests(Integer farmId, String blockCode, LocalDate from, LocalDate to) {
        return harvestRecordRepository.findByOptionalFilters(farmId, normalizeFilter(blockCode), from, to)
                .stream()
                .map(HarvestService::toResponse)
                .toList();
    }

    /**
     * Retrieves one harvest with its variety row.
     *
     * @param id the harvest identifier
     * @return the harvest
     * @throws HarvestNotFoundException if no harvest has this identifier
     */
    public HarvestResponse getHarvestById(Long id) {
        HarvestRecord harvest = harvestRecordRepository.findWithVarietyById(id)
                .orElseThrow(() -> new HarvestNotFoundException(id));
        return toResponse(harvest);
    }
}
