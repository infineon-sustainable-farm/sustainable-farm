package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthTreatment;
import com.infineonbit.sustainablefarm.modules.plants.entity.InspectionMethod;
import com.infineonbit.sustainablefarm.modules.plants.entity.TreatmentUnit;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthFindingRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthTreatmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HealthFindingServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final LocalDate SEPTEMBER_1 = LocalDate.of(2026, 9, 1);

    private static final HealthIssueReference MEALYBUG = new HealthIssueReference(2L, "MANGO_MEALYBUG",
            "Mango mealybug", HealthIssueKind.PEST, "Rastrococcus invadens", "RASTIN", "Vayssieres_2012", NOW);
    private static final HealthIssueReference ANTHRACNOSE = new HealthIssueReference(5L, "ANTHRACNOSE",
            "Anthracnose", HealthIssueKind.DISEASE, "Colletotrichum gloeosporioides", "COLLGL", "Dianda_2025", NOW);
    private static final HealthIssueReference OTHER = new HealthIssueReference(8L, "OTHER", "Other",
            HealthIssueKind.OTHER, null, null, "by_definition", NOW);

    private static final HealthInspection BLOCK_C = new HealthInspection(1L, null, "C", SEPTEMBER_1, 48, "Awa",
            InspectionMethod.VISUAL, "user_entry", NOW);

    @Mock
    private HealthFindingRepository healthFindingRepository;

    @Mock
    private HealthTreatmentRepository healthTreatmentRepository;

    @InjectMocks
    private HealthFindingService healthFindingService;

    private static HealthFinding finding(long id, HealthIssueReference issue, String otherLabel, LocalDate resolvedOn) {
        return new HealthFinding(id, BLOCK_C, issue, otherLabel, null, resolvedOn, null, "user_entry", NOW);
    }

    private static HealthTreatment treatment(HealthFinding finding, LocalDate treatedOn, int interval) {
        return new HealthTreatment(null, null, "C", finding.getIssue(), null, finding, treatedOn, "Product",
                "ingredient", BigDecimal.ONE, TreatmentUnit.KG, interval, "Awa", null, "user_entry", NOW);
    }

    /**
     * Three findings of block C: anthracnose treated twice, the mealybug resolved
     * without treatment, and an "Other" left alone.
     */
    private void blockCHasThreeFindings(LocalDate from, LocalDate to) {
        HealthFinding anthracnose = finding(10L, ANTHRACNOSE, null, null);
        HealthFinding mealybug = finding(11L, MEALYBUG, null, LocalDate.of(2026, 9, 10));
        HealthFinding leafCurl = finding(12L, OTHER, "Leaf curl", null);
        when(healthFindingRepository.findByOptionalFilters(null, "C", from, to))
                .thenReturn(List.of(anthracnose, mealybug, leafCurl));
        when(healthTreatmentRepository.findByFindingIds(List.of(10L, 11L, 12L))).thenReturn(List.of(
                treatment(anthracnose, LocalDate.of(2026, 9, 4), 14),
                treatment(anthracnose, LocalDate.of(2026, 9, 10), 1)));
    }

    @Test
    void getAllFindings_shouldComputeStatusCountAndDates_withOneTreatmentQuery() {
        // Arrange
        blockCHasThreeFindings(null, null);
        // Act
        List<HealthFindingResponse> findings = healthFindingService.getAllFindings(null, " C ", null, null, null);
        // Assert: the earlier treatment's longer interval sets the harvest date
        assertEquals(new HealthFindingResponse(10L, 1L, null, "C", SEPTEMBER_1, "ANTHRACNOSE", "Anthracnose",
                HealthIssueKind.DISEASE, null, null, HealthFindingStatus.IN_PROGRESS, 2, LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 18), null, null), findings.get(0));
        assertEquals(HealthFindingStatus.CLOSED_WITHOUT_TREATMENT, findings.get(1).status());
        assertEquals(0, findings.get(1).treatmentCount());
        assertEquals(HealthFindingStatus.UNTREATED, findings.get(2).status());
        assertEquals("Leaf curl", findings.get(2).otherLabel());
        verify(healthTreatmentRepository, times(1)).findByFindingIds(any());
    }

    @Test
    void getAllFindings_shouldFilterOnTheStatus() {
        // Arrange
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        blockCHasThreeFindings(from, to);
        // Act
        List<HealthFindingResponse> untreated = healthFindingService.getAllFindings(null, "C",
                HealthFindingStatus.UNTREATED, from, to);
        // Assert
        assertEquals(List.of(12L), untreated.stream().map(HealthFindingResponse::id).toList());
    }

    @Test
    void getAllFindings_shouldNotQueryTreatments_whenThereIsNoFinding() {
        // Arrange
        when(healthFindingRepository.findByOptionalFilters(1, null, null, null)).thenReturn(List.of());
        // Act
        List<HealthFindingResponse> findings = healthFindingService.getAllFindings(1, "  ", null, null, null);
        // Assert: a blank block is no filter, and no treatment query is sent
        assertTrue(findings.isEmpty());
        verify(healthTreatmentRepository, never()).findByFindingIds(any());
    }
}
