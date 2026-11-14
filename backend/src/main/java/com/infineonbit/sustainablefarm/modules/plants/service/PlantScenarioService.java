package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantScenarioRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantScenarioResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantScenario;
import com.infineonbit.sustainablefarm.modules.plants.exception.PlantScenarioNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.PlantScenarioRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The planting scenarios, as the user enters and corrects them: the
 * assumptions of each plan, with their sources.
 *
 * <p>Outside the dev profile no scenario is loaded at startup. Only the
 * assumptions are stored; the analysis computes the results on every read, so a
 * correction counts at once.
 *
 * <p>Names are compared in Java, ignoring case, accents and surrounding spaces,
 * as for the variety references.
 */
@Service
@AllArgsConstructor
public class PlantScenarioService {

    /** {@code source} of a value entered without one. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    /** By name, ignoring case and accents, then by identifier: the same order on every database. */
    private static final Comparator<PlantScenario> BY_NAME = Comparator
            .comparing((PlantScenario scenario) -> VarietyReferenceMatcher.matchKey(scenario.getName()))
            .thenComparing(PlantScenario::getId);

    private final PlantScenarioRepository plantScenarioRepository;

    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private static BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    static PlantScenarioResponse toResponse(PlantScenario scenario) {
        return new PlantScenarioResponse(
                scenario.getId(),
                scenario.getName(),
                scenario.getVarietyName(),
                scenario.getTreeCount(),
                scenario.getTreeCountSource(),
                scenario.getYieldPerTreeKg(),
                scenario.getYieldSource(),
                scenario.getLossRate(),
                scenario.getLossRateSource(),
                toDouble(scenario.getPricePerKg()),
                scenario.getPriceSource(),
                scenario.getCurrency(),
                toDouble(scenario.getAnnualFixedCost()),
                scenario.getAnnualFixedCostSource(),
                toDouble(scenario.getVariableCostPerKg()),
                scenario.getVariableCostSource(),
                toDouble(scenario.getInitialInvestment()),
                scenario.getInvestmentSource(),
                toDouble(scenario.getCostPerAdditionalTree()),
                scenario.getCostPerAdditionalTreeSource(),
                scenario.getLastUpdated());
    }

    /**
     * An optional text as stored: trimmed, and {@code null} when it is missing or blank.
     *
     * @param text the text as received, possibly {@code null}
     * @return the trimmed text, or {@code null} if it was null or blank
     */
    private static String normalizeText(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    /**
     * The source to store with a value.
     *
     * @param hasValue    whether the value is entered; a value left out has no source
     * @param sent        the source sent, possibly {@code null} or blank
     * @param unchanged   whether the value is the one already recorded
     * @param recorded    the source already recorded, for a correction
     * @return the source sent, trimmed; else the recorded one for an unchanged
     *         value, and {@code user_entry} for a new one; {@code null} without value
     */
    private static String sourceOf(boolean hasValue, String sent, boolean unchanged, String recorded) {
        if (!hasValue) {
            return null;
        }
        String source = normalizeText(sent);
        if (source != null) {
            return source;
        }
        return unchanged ? recorded : USER_ENTRY_SOURCE;
    }

    /** Whether a decimal sent equals the one recorded, whatever their scales: 0.9072 and 0.90720 are equal. */
    private static boolean sameDecimal(BigDecimal recorded, BigDecimal sent) {
        return recorded != null && sent != null && recorded.compareTo(sent) == 0;
    }

    /**
     * Refuses a name that another scenario already has, ignoring case, accents
     * and surrounding spaces.
     *
     * @param name      the trimmed name
     * @param excludeId the scenario being corrected, which may keep its own name,
     *                  or {@code null} for a new scenario
     * @throws ConflictException if another scenario has the name
     */
    private void refuseTakenName(String name, Long excludeId) {
        String key = VarietyReferenceMatcher.matchKey(name);
        plantScenarioRepository.findAll().stream()
                .filter(existing -> !existing.getId().equals(excludeId))
                .filter(existing -> VarietyReferenceMatcher.matchKey(existing.getName()).equals(key))
                .min(Comparator.comparing(PlantScenario::getId))
                .ifPresent(existing -> {
                    throw new ConflictException(nameTaken(existing.getName()));
                });
    }

    private static String nameTaken(String name) {
        return "A scenario named " + name + " already exists";
    }

    /**
     * Writes the scenario and flushes it, so that the unique constraint on the
     * name answers inside the request: two identical names sent at the same
     * time keep one row, and the other request gets a 409.
     */
    private PlantScenarioResponse saveFlushed(PlantScenario scenario) {
        try {
            return toResponse(plantScenarioRepository.saveAndFlush(scenario));
        } catch (DataIntegrityViolationException sameNameAtTheSameTime) {
            // The constraint answers once the other write is committed: the name is taken by now.
            throw new ConflictException(nameTaken(scenario.getName()));
        }
    }

    /**
     * Copies the request into the scenario, each value with its source. For a
     * correction, a source left out stays as it was when its value does not
     * change, and becomes {@code user_entry} when it does; for a new scenario,
     * every value is new.
     *
     * @param scenario the scenario to write, holding the recorded values for a correction
     * @param request  the request, already validated
     * @param name     the trimmed name
     * @param now      the write time
     */
    private static void write(PlantScenario scenario, PlantScenarioRequest request, String name, Instant now) {
        BigDecimal price = decimal(request.pricePerKg());
        BigDecimal fixedCost = decimal(request.annualFixedCost());
        BigDecimal variableCost = decimal(request.variableCostPerKg());
        BigDecimal investment = decimal(request.initialInvestment());
        BigDecimal costPerTree = decimal(request.costPerAdditionalTree());

        scenario.setTreeCountSource(sourceOf(true, request.treeCountSource(),
                Objects.equals(request.treeCount(), scenario.getTreeCount()), scenario.getTreeCountSource()));
        scenario.setYieldSource(sourceOf(true, request.yieldSource(),
                Objects.equals(request.yieldPerTreeKg(), scenario.getYieldPerTreeKg()), scenario.getYieldSource()));
        scenario.setLossRateSource(sourceOf(request.lossRate() != null, request.lossRateSource(),
                Objects.equals(request.lossRate(), scenario.getLossRate()), scenario.getLossRateSource()));
        scenario.setPriceSource(sourceOf(price != null, request.priceSource(),
                sameDecimal(scenario.getPricePerKg(), price), scenario.getPriceSource()));
        scenario.setAnnualFixedCostSource(sourceOf(true, request.annualFixedCostSource(),
                sameDecimal(scenario.getAnnualFixedCost(), fixedCost), scenario.getAnnualFixedCostSource()));
        scenario.setVariableCostSource(sourceOf(variableCost != null, request.variableCostSource(),
                sameDecimal(scenario.getVariableCostPerKg(), variableCost), scenario.getVariableCostSource()));
        scenario.setInvestmentSource(sourceOf(true, request.investmentSource(),
                sameDecimal(scenario.getInitialInvestment(), investment), scenario.getInvestmentSource()));
        scenario.setCostPerAdditionalTreeSource(sourceOf(costPerTree != null, request.costPerAdditionalTreeSource(),
                sameDecimal(scenario.getCostPerAdditionalTree(), costPerTree),
                scenario.getCostPerAdditionalTreeSource()));

        scenario.setName(name);
        scenario.setVarietyName(normalizeText(request.varietyName()));
        scenario.setTreeCount(request.treeCount());
        scenario.setYieldPerTreeKg(request.yieldPerTreeKg());
        scenario.setLossRate(request.lossRate());
        scenario.setPricePerKg(price);
        scenario.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        scenario.setAnnualFixedCost(fixedCost);
        scenario.setVariableCostPerKg(variableCost);
        scenario.setInitialInvestment(investment);
        scenario.setCostPerAdditionalTree(costPerTree);
        scenario.setLastUpdated(now);
    }

    /**
     * Retrieves every scenario.
     *
     * @return the scenarios ordered by name, ignoring case and accents, then by
     *         identifier; possibly empty
     */
    public List<PlantScenarioResponse> getAllScenarios() {
        return plantScenarioRepository.findAll().stream()
                .sorted(BY_NAME)
                .map(PlantScenarioService::toResponse)
                .toList();
    }

    /**
     * Retrieves one scenario.
     *
     * @param id the identifier of the scenario
     * @return the scenario
     * @throws PlantScenarioNotFoundException if no scenario has this identifier
     */
    public PlantScenarioResponse getScenario(Long id) {
        return plantScenarioRepository.findById(id)
                .map(PlantScenarioService::toResponse)
                .orElseThrow(() -> new PlantScenarioNotFoundException(id));
    }

    /**
     * Enters a scenario.
     *
     * <p>The name is trimmed, and refused when another scenario has it, ignoring
     * case, accents and surrounding spaces. A value entered without a source is
     * recorded with {@code user_entry}; an optional value left out has no source.
     *
     * @param request the scenario, already validated
     * @return the entered scenario
     * @throws ConflictException if a scenario with the same name exists,
     *                           including when the same name was being entered
     *                           at the same time
     */
    @Transactional
    public PlantScenarioResponse createScenario(PlantScenarioRequest request) {
        return createScenario(request, Instant.now());
    }

    /**
     * Same as {@link #createScenario(PlantScenarioRequest)}, at an explicit
     * write time so the {@code lastUpdated} value can be tested.
     */
    PlantScenarioResponse createScenario(PlantScenarioRequest request, Instant now) {
        String name = request.name().trim();
        refuseTakenName(name, null);
        PlantScenario scenario = new PlantScenario();
        write(scenario, request, name, now);
        return saveFlushed(scenario);
    }

    /**
     * Corrects a scenario: every value is replaced.
     *
     * <p>The name is checked as for a new scenario, except against the scenario
     * itself. A source sent is recorded as sent, trimmed. A source left out
     * stays as it was when its value does not change, and becomes
     * {@code user_entry} when it does: a corrected value is never credited to
     * the source of the value it replaced. An optional value left out is
     * removed, with its source.
     *
     * @param id      the identifier of the scenario
     * @param request the scenario, already validated
     * @return the corrected scenario
     * @throws PlantScenarioNotFoundException if no scenario has this identifier
     * @throws ConflictException              if another scenario has the name
     */
    @Transactional
    public PlantScenarioResponse updateScenario(Long id, PlantScenarioRequest request) {
        return updateScenario(id, request, Instant.now());
    }

    /**
     * Same as {@link #updateScenario(Long, PlantScenarioRequest)}, at an
     * explicit write time so the {@code lastUpdated} value can be tested.
     */
    PlantScenarioResponse updateScenario(Long id, PlantScenarioRequest request, Instant now) {
        PlantScenario scenario = plantScenarioRepository.findById(id)
                .orElseThrow(() -> new PlantScenarioNotFoundException(id));
        String name = request.name().trim();
        refuseTakenName(name, id);
        write(scenario, request, name, now);
        return saveFlushed(scenario);
    }
}
