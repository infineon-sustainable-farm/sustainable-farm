package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthIssueResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthIssueReferenceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

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
}
