package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthIssueResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthIssueReferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HealthIssueServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");

    @Mock
    private HealthIssueReferenceRepository healthIssueReferenceRepository;

    @InjectMocks
    private HealthIssueService healthIssueService;

    static HealthIssueReference issue(long id, String code, String name, HealthIssueKind kind) {
        return new HealthIssueReference(id, code, name, kind, null, null, "by_definition", NOW);
    }

    @Test
    void getAllIssues_shouldListPestsThenDiseasesThenOther_eachByName() {
        // Arrange: stored in no particular order, with an issue added after "Other"
        when(healthIssueReferenceRepository.findAll()).thenReturn(List.of(
                issue(8, "OTHER", "Other", HealthIssueKind.OTHER),
                issue(5, "ANTHRACNOSE", "Anthracnose", HealthIssueKind.DISEASE),
                issue(3, "TERMITES", "Termites", HealthIssueKind.PEST),
                issue(9, "POWDERY_MILDEW", "Powdery mildew", HealthIssueKind.DISEASE),
                issue(1, "FRUIT_FLY", "Fruit flies", HealthIssueKind.PEST)));
        // Act
        List<HealthIssueResponse> issues = healthIssueService.getAllIssues();
        // Assert
        assertEquals(List.of("FRUIT_FLY", "TERMITES", "ANTHRACNOSE", "POWDERY_MILDEW", "OTHER"),
                issues.stream().map(HealthIssueResponse::code).toList());
        assertEquals(new HealthIssueResponse(1L, "FRUIT_FLY", "Fruit flies", HealthIssueKind.PEST, null, null,
                "by_definition"), issues.getFirst());
    }

    @Test
    void getIssueByCode_shouldIgnoreCaseAndSurroundingSpaces() {
        // Arrange
        HealthIssueReference fruitFly = issue(1, "FRUIT_FLY", "Fruit flies", HealthIssueKind.PEST);
        when(healthIssueReferenceRepository.findByCode("FRUIT_FLY")).thenReturn(Optional.of(fruitFly));
        // Act & Assert
        assertSame(fruitFly, healthIssueService.getIssueByCode(" fruit_fly "));
    }

    @Test
    void getIssueByCode_shouldThrow422_whenTheCodeIsNotInTheCatalogue() {
        // Arrange
        when(healthIssueReferenceRepository.findByCode("POWDERY_MILDEW")).thenReturn(Optional.empty());
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> healthIssueService.getIssueByCode("powdery_mildew"));
        // Assert: the code as looked up
        assertEquals("No health issue with code POWDERY_MILDEW in the catalogue", exception.getMessage());
    }
}
