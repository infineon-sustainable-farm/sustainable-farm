package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FindingResolutionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthTreatment;
import com.infineonbit.sustainablefarm.modules.plants.entity.InspectionMethod;
import com.infineonbit.sustainablefarm.modules.plants.entity.TreatmentUnit;
import com.infineonbit.sustainablefarm.modules.plants.exception.HealthFindingNotFoundException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    /**
     * The anthracnose of block C, inspected on 1 September: read once before the
     * resolution, then once more after it, as the database would return it.
     */
    private void anthracnoseResolvedOn(LocalDate resolvedOn, String note, List<LocalDate> treatmentDates) {
        HealthFinding open = finding(10L, ANTHRACNOSE, null, null);
        HealthFinding resolved = new HealthFinding(10L, BLOCK_C, ANTHRACNOSE, null, "42", resolvedOn, note,
                "user_entry", NOW);
        when(healthFindingRepository.findById(10L)).thenReturn(Optional.of(open)).thenReturn(Optional.of(resolved));
        when(healthTreatmentRepository.findByFindingIds(List.of(10L))).thenReturn(treatmentDates.stream()
                .map(date -> treatment(open, date, 14))
                .toList());
    }

    private static FindingResolutionRequest resolution(LocalDate resolvedOn) {
        return new FindingResolutionRequest(resolvedOn, " No new lesions ");
    }

    @Test
    void resolveFinding_shouldStoreTheDateAndNote_andReturnTreated() {
        // Arrange
        LocalDate september20 = LocalDate.of(2026, 9, 20);
        anthracnoseResolvedOn(september20, "No new lesions", List.of(LocalDate.of(2026, 9, 4)));
        when(healthFindingRepository.resolve(10L, september20, "No new lesions", NOW)).thenReturn(1);
        // Act
        HealthFindingResponse response = healthFindingService.resolveFinding(10L, resolution(september20), NOW);
        // Assert: the trimmed note written by the conditional update, and the new status
        verify(healthFindingRepository).resolve(10L, september20, "No new lesions", NOW);
        assertEquals(new HealthFindingResponse(10L, 1L, null, "C", SEPTEMBER_1, "ANTHRACNOSE", "Anthracnose",
                HealthIssueKind.DISEASE, null, "42", HealthFindingStatus.TREATED, 1, LocalDate.of(2026, 9, 4),
                LocalDate.of(2026, 9, 18), september20, "No new lesions"), response);
    }

    @Test
    void resolveFinding_shouldReturnClosedWithoutTreatment_whenNeverTreated() {
        // Arrange
        LocalDate september10 = LocalDate.of(2026, 9, 10);
        anthracnoseResolvedOn(september10, null, List.of());
        when(healthFindingRepository.resolve(10L, september10, null, NOW)).thenReturn(1);
        // Act
        HealthFindingResponse response = healthFindingService.resolveFinding(10L,
                new FindingResolutionRequest(september10, "  "), NOW);
        // Assert: a blank note is no note
        assertEquals(HealthFindingStatus.CLOSED_WITHOUT_TREATMENT, response.status());
        assertEquals(0, response.treatmentCount());
        assertEquals(september10, response.resolvedOn());
    }

    @Test
    void resolveFinding_shouldThrow409_whenAlreadyResolved() {
        // Arrange
        when(healthFindingRepository.findById(10L)).thenReturn(Optional.of(
                finding(10L, ANTHRACNOSE, null, LocalDate.of(2026, 9, 20))));
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> healthFindingService.resolveFinding(10L, resolution(LocalDate.of(2026, 9, 21)), NOW));
        // Assert
        assertEquals("Finding 10 was already resolved on 2026-09-20", exception.getMessage());
        verify(healthFindingRepository, never()).resolve(anyLong(), any(), any(), any());
    }

    @Test
    void resolveFinding_shouldThrow409_whenAnotherRequestResolvedItFirst() {
        // Arrange: open when read, resolved by another request before the update
        LocalDate september20 = LocalDate.of(2026, 9, 20);
        anthracnoseResolvedOn(september20, "Resolved elsewhere", List.of());
        when(healthFindingRepository.resolve(10L, LocalDate.of(2026, 9, 21), "No new lesions", NOW)).thenReturn(0);
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> healthFindingService.resolveFinding(10L, resolution(LocalDate.of(2026, 9, 21)), NOW));
        // Assert: the same message, with the date the other request wrote
        assertEquals("Finding 10 was already resolved on 2026-09-20", exception.getMessage());
    }

    @Test
    void resolveFinding_shouldThrow422_whenDatedBeforeTheInspection() {
        // Arrange
        when(healthFindingRepository.findById(10L)).thenReturn(Optional.of(finding(10L, ANTHRACNOSE, null, null)));
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> healthFindingService.resolveFinding(10L, resolution(LocalDate.of(2026, 8, 30)), NOW));
        // Assert
        assertEquals("The resolution date 2026-08-30 is before the inspection date 2026-09-01 of finding 10",
                exception.getMessage());
        verify(healthFindingRepository, never()).resolve(anyLong(), any(), any(), any());
    }

    @Test
    void resolveFinding_shouldThrow422_whenDatedBeforeTheLastTreatment() {
        // Arrange: treated on 4 and 10 September
        HealthFinding open = finding(10L, ANTHRACNOSE, null, null);
        when(healthFindingRepository.findById(10L)).thenReturn(Optional.of(open));
        when(healthTreatmentRepository.findByFindingIds(List.of(10L))).thenReturn(List.of(
                treatment(open, LocalDate.of(2026, 9, 4), 14),
                treatment(open, LocalDate.of(2026, 9, 10), 1)));
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> healthFindingService.resolveFinding(10L, resolution(LocalDate.of(2026, 9, 5)), NOW));
        // Assert
        assertEquals("The resolution date 2026-09-05 is before the last treatment date 2026-09-10 of finding 10",
                exception.getMessage());
        verify(healthFindingRepository, never()).resolve(anyLong(), any(), any(), any());
    }

    @Test
    void resolveFinding_shouldAllowTheDayOfTheLastTreatment() {
        // Arrange
        LocalDate september4 = LocalDate.of(2026, 9, 4);
        anthracnoseResolvedOn(september4, "No new lesions", List.of(september4));
        when(healthFindingRepository.resolve(10L, september4, "No new lesions", NOW)).thenReturn(1);
        // Act
        HealthFindingResponse response = healthFindingService.resolveFinding(10L, resolution(september4), NOW);
        // Assert
        assertEquals(HealthFindingStatus.TREATED, response.status());
        assertEquals(september4, response.resolvedOn());
    }

    @Test
    void resolveFinding_shouldThrow404_whenTheFindingDoesNotExist() {
        // Arrange
        when(healthFindingRepository.findById(999L)).thenReturn(Optional.empty());
        // Act
        HealthFindingNotFoundException exception = assertThrows(HealthFindingNotFoundException.class,
                () -> healthFindingService.resolveFinding(999L, resolution(LocalDate.of(2026, 9, 20)), NOW));
        // Assert
        assertEquals("Health finding with ID 999 not found", exception.getMessage());
    }
}
