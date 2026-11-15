package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthFindingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthInspectionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.BlockHealthResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthInspectionResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.InspectionMethod;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthFindingRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthInspectionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class HealthInspectionService {

    /** {@code source} of every inspection and finding written through the API. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    private final HealthInspectionRepository healthInspectionRepository;
    private final HealthFindingRepository healthFindingRepository;
    private final HealthIssueService healthIssueService;
    private final HealthFindingService healthFindingService;

    /**
     * A code as stored: trimmed and upper-cased, so {@code " c "} becomes
     * {@code "C"}, as for a planting. The request validation already refused any
     * other form.
     */
    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * An optional text as stored: trimmed, and {@code null} when it is blank.
     * Also turns a blank list filter into no filter, as for the other lists of
     * the module.
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

    private static HealthInspectionResponse toResponse(HealthInspection inspection,
                                                       List<HealthFindingResponse> findings) {
        return new HealthInspectionResponse(
                inspection.getId(),
                inspection.getFarmId(),
                inspection.getBlockCode(),
                inspection.getInspectedOn(),
                inspection.getHealthScorePct(),
                HealthCalculator.category(inspection.getHealthScorePct()),
                inspection.getObserver(),
                inspection.getMethod(),
                inspection.getSource(),
                inspection.getLastUpdated(),
                findings);
    }

    /**
     * Records an inspection of a block and the problems seen, in a single
     * transaction.
     *
     * <ol>
     *     <li>Finds the catalogue row of every finding first. A code missing from
     *         the catalogue refuses the whole inspection with a 422, before
     *         anything is written. "Other" is added to the catalogue on its
     *         first use, once every other code is found, see
     *         {@link HealthIssueService#getIssuesByCode(List)}.</li>
     *     <li>Stores the inspection, then each finding. The block needs no
     *         recorded planting.</li>
     * </ol>
     * If any write fails, the transaction stores nothing, inspection included.
     *
     * @param request the inspection, already validated
     * @return the recorded inspection, with its findings
     * @throws com.infineonbit.sustainablefarm.core.exception.BusinessRuleException
     *         if an issue code other than {@code OTHER} is not in the catalogue
     */
    @Transactional
    public HealthInspectionResponse recordInspection(HealthInspectionRequest request) {
        return recordInspection(request, Instant.now());
    }

    /**
     * Same as {@link #recordInspection(HealthInspectionRequest)}, at an explicit
     * write time so the {@code lastUpdated} value can be tested.
     */
    HealthInspectionResponse recordInspection(HealthInspectionRequest request, Instant now) {
        List<HealthIssueReference> issues = healthIssueService.getIssuesByCode(request.findings().stream()
                .map(HealthFindingRequest::issueCode)
                .toList());

        HealthInspection inspection = new HealthInspection();
        inspection.setFarmId(request.farmId());
        inspection.setBlockCode(normalizeCode(request.blockCode()));
        inspection.setInspectedOn(request.inspectedOn());
        // A whole number from 0 to 100: the request validation refused anything else.
        inspection.setHealthScorePct(request.healthScorePct().intValue());
        inspection.setObserver(normalizeOptionalText(request.observer()));
        inspection.setMethod(request.method() == null
                ? null
                : InspectionMethod.valueOf(normalizeCode(request.method())));
        inspection.setSource(USER_ENTRY_SOURCE);
        inspection.setLastUpdated(now);
        HealthInspection savedInspection = healthInspectionRepository.save(inspection);

        List<HealthFinding> findings = new ArrayList<>();
        for (int index = 0; index < issues.size(); index++) {
            HealthFindingRequest findingRequest = request.findings().get(index);
            HealthFinding finding = new HealthFinding();
            finding.setInspection(savedInspection);
            finding.setIssue(issues.get(index));
            finding.setOtherLabel(normalizeOptionalText(findingRequest.otherLabel()));
            finding.setTreeLabel(normalizeOptionalCode(findingRequest.treeLabel()));
            finding.setSource(USER_ENTRY_SOURCE);
            finding.setLastUpdated(now);
            findings.add(healthFindingRepository.save(finding));
        }
        return toResponse(savedInspection, healthFindingService.toResponses(findings));
    }

    /**
     * Retrieves the inspections matching the optional filters, each with its
     * findings.
     *
     * <p>Every filter is independent and optional; a missing farm means every
     * farm, as for the other lists of the module. Both dates are included. The
     * findings of every inspection come from one query.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode raw block value as stored (for example {@code "C"}),
     *                  or {@code null} for every block
     * @param from      first inspection date, included, or {@code null}
     * @param to        last inspection date, included, or {@code null}
     * @return the matching inspections, ordered by date then identifier
     */
    public List<HealthInspectionResponse> getAllInspections(Integer farmId, String blockCode,
                                                            LocalDate from, LocalDate to) {
        List<HealthInspection> inspections = healthInspectionRepository.findByOptionalFilters(
                farmId, normalizeOptionalText(blockCode), from, to);
        if (inspections.isEmpty()) {
            return List.of();
        }
        List<HealthFinding> findings = healthFindingRepository.findByInspectionIds(
                inspections.stream().map(HealthInspection::getId).toList());
        Map<Long, List<HealthFindingResponse>> findingsByInspection = healthFindingService.toResponses(findings)
                .stream()
                .collect(Collectors.groupingBy(HealthFindingResponse::inspectionId));
        return inspections.stream()
                .map(inspection -> toResponse(inspection,
                        findingsByInspection.getOrDefault(inspection.getId(), List.of())))
                .toList();
    }

    /**
     * Retrieves the current health of each inspected block: the score of its
     * most recent inspection, with the date and the category.
     *
     * <p>A block is the (farm, block) pair, a missing farm being a farm of its
     * own; the farm filter keeps the usual meaning, a missing farm meaning every
     * farm. The inspections come from one query, and the most recent one of each
     * block is picked in Java: latest date, then highest identifier.
     *
     * @param farmId farm identifier, or {@code null} for every farm
     * @return one line per inspected block, by block code then farm
     */
    public List<BlockHealthResponse> getBlockHealth(Integer farmId) {
        return HealthCalculator.latestByBlock(healthInspectionRepository.findByOptionalFilters(farmId, null, null, null))
                .stream()
                .map(inspection -> new BlockHealthResponse(
                        inspection.getFarmId(),
                        inspection.getBlockCode(),
                        inspection.getId(),
                        inspection.getInspectedOn(),
                        inspection.getHealthScorePct(),
                        HealthCalculator.category(inspection.getHealthScorePct())))
                .toList();
    }
}
