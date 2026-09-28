package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthIssueResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthIssueReferenceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class HealthIssueService {

    /**
     * Pests first, then diseases, then "Other", each by name. Sorted in Java:
     * the kind is stored as text, which SQL would sort alphabetically.
     */
    private static final Comparator<HealthIssueReference> CATALOGUE_ORDER =
            Comparator.comparing(HealthIssueReference::getKind)
                    .thenComparing(HealthIssueReference::getName)
                    .thenComparing(HealthIssueReference::getId);

    private final HealthIssueReferenceRepository healthIssueReferenceRepository;

    static HealthIssueResponse toResponse(HealthIssueReference issue) {
        return new HealthIssueResponse(
                issue.getId(),
                issue.getCode(),
                issue.getName(),
                issue.getKind(),
                issue.getScientificName(),
                issue.getEppoCode(),
                issue.getSource());
    }

    /**
     * Retrieves the whole catalogue: pests, then diseases, then "Other", each by
     * name, so "Other" stays last whatever is added later.
     *
     * @return the catalogue, possibly empty
     */
    public List<HealthIssueResponse> getAllIssues() {
        return healthIssueReferenceRepository.findAll().stream()
                .sorted(CATALOGUE_ORDER)
                .map(HealthIssueService::toResponse)
                .toList();
    }

    /**
     * The catalogue row of a code sent by the API, compared without case or
     * surrounding spaces: {@code " fruit_fly "} finds {@code FRUIT_FLY}.
     *
     * @param code the code as received, already checked by the request pattern
     * @return the catalogue row
     * @throws BusinessRuleException if the catalogue has no such code, answered
     *                               with a 422: the code has the right form,
     *                               the catalogue refuses it
     */
    public HealthIssueReference getIssueByCode(String code) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return healthIssueReferenceRepository.findByCode(normalized)
                .orElseThrow(() -> new BusinessRuleException(
                        "No health issue with code " + normalized + " in the catalogue"));
    }
}
