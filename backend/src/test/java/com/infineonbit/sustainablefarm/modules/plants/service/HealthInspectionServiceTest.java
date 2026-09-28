package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthFindingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthInspectionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthInspectionResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthCategory;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.InspectionMethod;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthFindingRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthInspectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HealthInspectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final LocalDate SEPTEMBER_1 = LocalDate.of(2026, 9, 1);

    private static final HealthIssueReference MEALYBUG = new HealthIssueReference(2L, "MANGO_MEALYBUG",
            "Mango mealybug", HealthIssueKind.PEST, "Rastrococcus invadens", "RASTIN", "Vayssieres_2012", NOW);
    private static final HealthIssueReference ANTHRACNOSE = new HealthIssueReference(5L, "ANTHRACNOSE",
            "Anthracnose", HealthIssueKind.DISEASE, "Colletotrichum gloeosporioides", "COLLGL", "Dianda_2025", NOW);
    private static final HealthIssueReference OTHER = new HealthIssueReference(8L, "OTHER", "Other",
            HealthIssueKind.OTHER, null, null, "by_definition", NOW);

    @Mock
    private HealthInspectionRepository healthInspectionRepository;

    @Mock
    private HealthFindingRepository healthFindingRepository;

    @Mock
    private HealthIssueService healthIssueService;

    private HealthInspectionService healthInspectionService;

    @BeforeEach
    void setUp() {
        healthInspectionService = new HealthInspectionService(healthInspectionRepository, healthFindingRepository,
                healthIssueService, new HealthFindingService());
    }

    /** The catalogue answers as the real service does: case and spaces ignored, 422 when missing. */
    private void catalogueHasTheUsualIssues() {
        Map<String, HealthIssueReference> catalogue = Map.of(
                "MANGO_MEALYBUG", MEALYBUG, "ANTHRACNOSE", ANTHRACNOSE, "OTHER", OTHER);
        when(healthIssueService.getIssueByCode(anyString())).thenAnswer(invocation -> {
            String code = invocation.<String>getArgument(0).trim().toUpperCase(Locale.ROOT);
            HealthIssueReference issue = catalogue.get(code);
            if (issue == null) {
                throw new BusinessRuleException("No health issue with code " + code + " in the catalogue");
            }
            return issue;
        });
    }

    /**
     * Saving gives each row an identifier, as the database would. Lenient for the
     * findings: an inspection with nothing found saves none.
     */
    private void savesAssignIds() {
        when(healthInspectionRepository.save(any(HealthInspection.class))).thenAnswer(invocation -> {
            HealthInspection inspection = invocation.getArgument(0);
            inspection.setId(1L);
            return inspection;
        });
        AtomicLong findingIds = new AtomicLong(10);
        lenient().when(healthFindingRepository.save(any(HealthFinding.class))).thenAnswer(invocation -> {
            HealthFinding finding = invocation.getArgument(0);
            finding.setId(findingIds.getAndIncrement());
            return finding;
        });
    }

    private HealthInspection savedInspection() {
        ArgumentCaptor<HealthInspection> captor = ArgumentCaptor.forClass(HealthInspection.class);
        verify(healthInspectionRepository).save(captor.capture());
        return captor.getValue();
    }

    private List<HealthFinding> savedFindings(int count) {
        ArgumentCaptor<HealthFinding> captor = ArgumentCaptor.forClass(HealthFinding.class);
        verify(healthFindingRepository, times(count)).save(captor.capture());
        return captor.getAllValues();
    }

    private static HealthInspectionRequest inspection(Integer farmId, String blockCode, double score,
                                                      List<HealthFindingRequest> findings) {
        return new HealthInspectionRequest(farmId, blockCode, SEPTEMBER_1, score, "Awa", "VISUAL", findings);
    }

    @Test
    void recordInspection_shouldStoreTheInspectionAndItsFindings() {
        // Arrange
        catalogueHasTheUsualIssues();
        savesAssignIds();
        // Act
        HealthInspectionResponse response = healthInspectionService.recordInspection(inspection(null, "C", 48.0,
                List.of(new HealthFindingRequest("ANTHRACNOSE", null, "42"),
                        new HealthFindingRequest("MANGO_MEALYBUG", null, null))), NOW);
        // Assert: the stored inspection
        HealthInspection inspection = savedInspection();
        assertNull(inspection.getFarmId());
        assertEquals("C", inspection.getBlockCode());
        assertEquals(SEPTEMBER_1, inspection.getInspectedOn());
        assertEquals(48, inspection.getHealthScorePct());
        assertEquals("Awa", inspection.getObserver());
        assertEquals(InspectionMethod.VISUAL, inspection.getMethod());
        assertEquals("user_entry", inspection.getSource());
        assertEquals(NOW, inspection.getLastUpdated());
        // Assert: the stored findings, attached to it
        List<HealthFinding> findings = savedFindings(2);
        assertSame(inspection, findings.get(0).getInspection());
        assertSame(ANTHRACNOSE, findings.get(0).getIssue());
        assertEquals("42", findings.get(0).getTreeLabel());
        assertSame(MEALYBUG, findings.get(1).getIssue());
        assertNull(findings.get(1).getTreeLabel());
        findings.forEach(finding -> {
            assertNull(finding.getOtherLabel());
            assertNull(finding.getResolvedOn());
            assertEquals("user_entry", finding.getSource());
            assertEquals(NOW, finding.getLastUpdated());
        });
        // Assert: the response, both findings untreated
        assertEquals(new HealthInspectionResponse(1L, null, "C", SEPTEMBER_1, 48, HealthCategory.MODERATE, "Awa",
                InspectionMethod.VISUAL, "user_entry", NOW, List.of(
                new HealthFindingResponse(10L, 1L, null, "C", SEPTEMBER_1, "ANTHRACNOSE", "Anthracnose",
                        HealthIssueKind.DISEASE, null, "42", HealthFindingStatus.UNTREATED, null, null),
                new HealthFindingResponse(11L, 1L, null, "C", SEPTEMBER_1, "MANGO_MEALYBUG", "Mango mealybug",
                        HealthIssueKind.PEST, null, null, HealthFindingStatus.UNTREATED, null, null))), response);
    }

    @Test
    void recordInspection_shouldStoreAnInspectionWithoutFinding() {
        // Arrange
        savesAssignIds();
        // Act
        HealthInspectionResponse response = healthInspectionService.recordInspection(
                inspection(null, "D", 85.0, List.of()), NOW);
        // Assert: a visit with nothing found is kept
        assertEquals(85, savedInspection().getHealthScorePct());
        verify(healthFindingRepository, never()).save(any());
        assertEquals(HealthCategory.VERY_HEALTHY, response.healthCategory());
        assertTrue(response.findings().isEmpty());
    }

    @Test
    void recordInspection_shouldNormalizeTheBlockCodesTreeLabelAndMethod() {
        // Arrange
        catalogueHasTheUsualIssues();
        savesAssignIds();
        // Act
        HealthInspectionResponse response = healthInspectionService.recordInspection(new HealthInspectionRequest(
                null, " c ", SEPTEMBER_1, 48.0, " Awa ", " visual ",
                List.of(new HealthFindingRequest(" anthracnose ", null, " r3-12 "))), NOW);
        // Assert
        HealthInspection inspection = savedInspection();
        assertEquals("C", inspection.getBlockCode());
        assertEquals("Awa", inspection.getObserver());
        assertEquals(InspectionMethod.VISUAL, inspection.getMethod());
        HealthFinding finding = savedFindings(1).getFirst();
        assertSame(ANTHRACNOSE, finding.getIssue());
        assertEquals("R3-12", finding.getTreeLabel());
        assertEquals("ANTHRACNOSE", response.findings().getFirst().issueCode());
    }

    @Test
    void recordInspection_shouldKeepTheFarmOfTheRequest() {
        // Arrange
        savesAssignIds();
        // Act
        HealthInspectionResponse response = healthInspectionService.recordInspection(
                inspection(1, "C", 48.0, List.of()), NOW);
        // Assert
        assertEquals(1, savedInspection().getFarmId());
        assertEquals(1, response.farmId());
    }

    @Test
    void recordInspection_shouldStoreNothing_whenAnIssueCodeIsUnknown() {
        // Arrange
        catalogueHasTheUsualIssues();
        HealthInspectionRequest request = inspection(null, "C", 48.0, List.of(
                new HealthFindingRequest("ANTHRACNOSE", null, null),
                new HealthFindingRequest("powdery_mildew", null, null)));
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> healthInspectionService.recordInspection(request, NOW));
        // Assert: every code is checked before anything is written
        assertEquals("No health issue with code POWDERY_MILDEW in the catalogue", exception.getMessage());
        verify(healthInspectionRepository, never()).save(any());
        verify(healthFindingRepository, never()).save(any());
    }

    @Test
    void recordInspection_shouldKeepTheLabelOfAnOtherFinding_andDropABlankTreeLabel() {
        // Arrange
        catalogueHasTheUsualIssues();
        savesAssignIds();
        // Act
        HealthInspectionResponse response = healthInspectionService.recordInspection(inspection(null, "C", 48.0,
                List.of(new HealthFindingRequest("OTHER", " Leaf curl ", "  "))), NOW);
        // Assert
        HealthFinding finding = savedFindings(1).getFirst();
        assertSame(OTHER, finding.getIssue());
        assertEquals("Leaf curl", finding.getOtherLabel());
        assertNull(finding.getTreeLabel());
        assertEquals("Leaf curl", response.findings().getFirst().otherLabel());
    }

    @Test
    void getAllInspections_shouldGroupTheFindingsOfEachInspection_withTwoQueries() {
        // Arrange: two inspections, the second with nothing found
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        HealthInspection blockC = new HealthInspection(1L, 1, "C", SEPTEMBER_1, 48, "Awa",
                InspectionMethod.VISUAL, "user_entry", NOW);
        HealthInspection blockCAgain = new HealthInspection(3L, 1, "C", LocalDate.of(2026, 9, 21), 72, null,
                null, "user_entry", NOW);
        when(healthInspectionRepository.findByOptionalFilters(1, "C", from, to))
                .thenReturn(List.of(blockC, blockCAgain));
        when(healthFindingRepository.findByInspectionIds(List.of(1L, 3L))).thenReturn(List.of(
                new HealthFinding(10L, blockC, ANTHRACNOSE, null, "42", null, null, "user_entry", NOW),
                new HealthFinding(11L, blockC, MEALYBUG, null, null, null, null, "user_entry", NOW)));
        // Act
        List<HealthInspectionResponse> inspections = healthInspectionService.getAllInspections(1, " C ", from, to);
        // Assert: in the order of the repository, each with its own findings
        assertEquals(List.of(1L, 3L), inspections.stream().map(HealthInspectionResponse::id).toList());
        assertEquals(List.of(10L, 11L),
                inspections.get(0).findings().stream().map(HealthFindingResponse::id).toList());
        assertTrue(inspections.get(1).findings().isEmpty());
        assertEquals(HealthCategory.HEALTHY, inspections.get(1).healthCategory());
        verify(healthFindingRepository, times(1)).findByInspectionIds(any());
    }
}
