package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FindingTreatmentRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PreventiveTreatmentRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthTreatmentResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HealthTreatmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final LocalDate SEPTEMBER_1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEPTEMBER_4 = LocalDate.of(2026, 9, 4);

    private static final HealthIssueReference FRUIT_FLY = new HealthIssueReference(1L, "FRUIT_FLY", "Fruit flies",
            HealthIssueKind.PEST, null, null, "Zida_2023", NOW);
    private static final HealthIssueReference ANTHRACNOSE = new HealthIssueReference(5L, "ANTHRACNOSE",
            "Anthracnose", HealthIssueKind.DISEASE, "Colletotrichum gloeosporioides", "COLLGL", "Dianda_2025", NOW);
    private static final HealthIssueReference OTHER = new HealthIssueReference(8L, "OTHER", "Other",
            HealthIssueKind.OTHER, null, null, "by_definition", NOW);

    @Mock
    private HealthTreatmentRepository healthTreatmentRepository;

    @Mock
    private HealthFindingRepository healthFindingRepository;

    @Mock
    private HealthIssueService healthIssueService;

    @InjectMocks
    private HealthTreatmentService healthTreatmentService;

    private static HealthInspection blockC(Integer farmId) {
        return new HealthInspection(1L, farmId, "C", SEPTEMBER_1, 48, "Awa", InspectionMethod.VISUAL,
                "user_entry", NOW);
    }

    private HealthFinding findingInDatabase(HealthIssueReference issue, String otherLabel, LocalDate resolvedOn) {
        HealthFinding finding = new HealthFinding(10L, blockC(1), issue, otherLabel, "42", resolvedOn, null,
                "user_entry", NOW);
        when(healthFindingRepository.findById(10L)).thenReturn(Optional.of(finding));
        return finding;
    }

    /** Saving gives the treatment an identifier, as the database would. */
    private void treatmentSaveAssignsId() {
        when(healthTreatmentRepository.save(any(HealthTreatment.class))).thenAnswer(invocation -> {
            HealthTreatment treatment = invocation.getArgument(0);
            treatment.setId(20L);
            return treatment;
        });
    }

    private HealthTreatment savedTreatment() {
        ArgumentCaptor<HealthTreatment> captor = ArgumentCaptor.forClass(HealthTreatment.class);
        verify(healthTreatmentRepository).save(captor.capture());
        return captor.getValue();
    }

    private static FindingTreatmentRequest copperOn(LocalDate treatedOn) {
        return new FindingTreatmentRequest(treatedOn, " Copper fungicide A ", " copper hydroxide ", 2.5, " kg ",
                14.0, " Awa ", " knapsack sprayer ");
    }

    private static PreventiveTreatmentRequest preventive(String blockCode, String target, String label,
                                                         double quantity, String unit, double interval,
                                                         String equipment) {
        return new PreventiveTreatmentRequest(null, blockCode, target, label, LocalDate.of(2026, 9, 5),
                " Protein bait B ", " spinosad ", quantity, unit, interval, " Issa ", equipment);
    }

    @Test
    void recordFindingTreatment_shouldCopyTheFarmBlockAndIssueOfTheFinding() {
        // Arrange
        HealthFinding finding = findingInDatabase(ANTHRACNOSE, null, null);
        treatmentSaveAssignsId();
        // Act
        HealthTreatmentResponse response = healthTreatmentService.recordFindingTreatment(10L,
                copperOn(SEPTEMBER_4), NOW);
        // Assert: the stored row, its farm, block and target copied from the finding
        HealthTreatment treatment = savedTreatment();
        assertEquals(1, treatment.getFarmId());
        assertEquals("C", treatment.getBlockCode());
        assertSame(ANTHRACNOSE, treatment.getTargetIssue());
        assertNull(treatment.getTargetOtherLabel());
        assertSame(finding, treatment.getFinding());
        assertEquals(SEPTEMBER_4, treatment.getTreatedOn());
        assertEquals("Copper fungicide A", treatment.getProductName());
        assertEquals("copper hydroxide", treatment.getActiveIngredient());
        assertEquals(0, new BigDecimal("2.5").compareTo(treatment.getQuantity()));
        assertEquals(TreatmentUnit.KG, treatment.getUnit());
        assertEquals(14, treatment.getPreHarvestIntervalDays());
        assertEquals("Awa", treatment.getApplicator());
        assertEquals("knapsack sprayer", treatment.getEquipment());
        assertEquals("user_entry", treatment.getSource());
        assertEquals(NOW, treatment.getLastUpdated());
        // Assert: the response, with the harvest date computed
        assertEquals(new HealthTreatmentResponse(20L, 1, "C", "ANTHRACNOSE", "Anthracnose", null, 10L, SEPTEMBER_4,
                "Copper fungicide A", "copper hydroxide", 2.5, TreatmentUnit.KG, 14, LocalDate.of(2026, 9, 18),
                "Awa", "knapsack sprayer", "user_entry", NOW), response);
    }

    @Test
    void recordFindingTreatment_shouldCopyTheLabelOfAnOtherFinding() {
        // Arrange
        findingInDatabase(OTHER, "Powdery mildew", null);
        treatmentSaveAssignsId();
        // Act
        HealthTreatmentResponse response = healthTreatmentService.recordFindingTreatment(10L,
                copperOn(SEPTEMBER_4), NOW);
        // Assert
        HealthTreatment treatment = savedTreatment();
        assertSame(OTHER, treatment.getTargetIssue());
        assertEquals("Powdery mildew", treatment.getTargetOtherLabel());
        assertEquals("Powdery mildew", response.targetOtherLabel());
    }

    @Test
    void recordFindingTreatment_shouldAllowTheInspectionDay() {
        // Arrange
        findingInDatabase(ANTHRACNOSE, null, null);
        treatmentSaveAssignsId();
        // Act
        healthTreatmentService.recordFindingTreatment(10L, copperOn(SEPTEMBER_1), NOW);
        // Assert
        assertEquals(SEPTEMBER_1, savedTreatment().getTreatedOn());
    }

    @Test
    void recordFindingTreatment_shouldThrow422_whenDatedBeforeTheInspection() {
        // Arrange
        findingInDatabase(ANTHRACNOSE, null, null);
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> healthTreatmentService.recordFindingTreatment(10L, copperOn(LocalDate.of(2026, 8, 30)), NOW));
        // Assert
        assertEquals("The treatment date 2026-08-30 is before the inspection date 2026-09-01 of finding 10",
                exception.getMessage());
        verify(healthTreatmentRepository, never()).save(any());
    }

    @Test
    void recordFindingTreatment_shouldThrow422_whenTheFindingIsResolved() {
        // Arrange
        findingInDatabase(ANTHRACNOSE, null, LocalDate.of(2026, 9, 20));
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> healthTreatmentService.recordFindingTreatment(10L, copperOn(LocalDate.of(2026, 9, 22)), NOW));
        // Assert
        assertEquals("Finding 10 was resolved on 2026-09-20 and cannot be treated any more", exception.getMessage());
        verify(healthTreatmentRepository, never()).save(any());
    }

    @Test
    void recordFindingTreatment_shouldThrow404_whenTheFindingDoesNotExist() {
        // Arrange
        when(healthFindingRepository.findById(999L)).thenReturn(Optional.empty());
        // Act
        HealthFindingNotFoundException exception = assertThrows(HealthFindingNotFoundException.class,
                () -> healthTreatmentService.recordFindingTreatment(999L, copperOn(SEPTEMBER_4), NOW));
        // Assert
        assertEquals("Health finding with ID 999 not found", exception.getMessage());
        verify(healthTreatmentRepository, never()).save(any());
    }

    @Test
    void recordPreventiveTreatment_shouldNormalizeTheBlockTargetAndUnit() {
        // Arrange
        when(healthIssueService.getIssueByCode("fruit_fly")).thenReturn(FRUIT_FLY);
        treatmentSaveAssignsId();
        // Act
        HealthTreatmentResponse response = healthTreatmentService.recordPreventiveTreatment(
                preventive(" d ", "fruit_fly", null, 1.0, " l ", 1.0, "  "), NOW);
        // Assert
        HealthTreatment treatment = savedTreatment();
        assertNull(treatment.getFarmId());
        assertEquals("D", treatment.getBlockCode());
        assertSame(FRUIT_FLY, treatment.getTargetIssue());
        assertNull(treatment.getTargetOtherLabel());
        assertNull(treatment.getFinding());
        assertEquals(TreatmentUnit.L, treatment.getUnit());
        assertEquals("Protein bait B", treatment.getProductName());
        assertEquals("Issa", treatment.getApplicator());
        assertNull(treatment.getEquipment());
        assertEquals(new HealthTreatmentResponse(20L, null, "D", "FRUIT_FLY", "Fruit flies", null, null,
                LocalDate.of(2026, 9, 5), "Protein bait B", "spinosad", 1.0, TreatmentUnit.L, 1,
                LocalDate.of(2026, 9, 6), "Issa", null, "user_entry", NOW), response);
    }

    @Test
    void recordPreventiveTreatment_shouldStoreTheLabelOfAnOtherTarget() {
        // Arrange
        when(healthIssueService.getIssueByCode("OTHER")).thenReturn(OTHER);
        treatmentSaveAssignsId();
        // Act
        HealthTreatmentResponse response = healthTreatmentService.recordPreventiveTreatment(
                preventive("D", "OTHER", " Powdery mildew ", 3.0, "KG", 0.0, null), NOW);
        // Assert: the label kept; a zero interval allows harvesting the same day
        assertEquals("Powdery mildew", savedTreatment().getTargetOtherLabel());
        assertEquals("OTHER", response.targetIssueCode());
        assertEquals("Powdery mildew", response.targetOtherLabel());
        assertEquals(LocalDate.of(2026, 9, 5), response.harvestAllowedFrom());
    }

    @Test
    void recordPreventiveTreatment_shouldThrow422_whenTheTargetIsNotInTheCatalogue() {
        // Arrange
        when(healthIssueService.getIssueByCode("powdery_mildew"))
                .thenThrow(new BusinessRuleException("No health issue with code POWDERY_MILDEW in the catalogue"));
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> healthTreatmentService.recordPreventiveTreatment(
                        preventive("D", "powdery_mildew", null, 3.0, "KG", 0.0, null), NOW));
        // Assert
        assertEquals("No health issue with code POWDERY_MILDEW in the catalogue", exception.getMessage());
        verify(healthTreatmentRepository, never()).save(any());
    }

    @Test
    void recordTreatment_shouldStoreTheQuantityExactly() {
        // Arrange: 1234.567 as a double is not exactly 1234.567
        when(healthIssueService.getIssueByCode("FRUIT_FLY")).thenReturn(FRUIT_FLY);
        treatmentSaveAssignsId();
        // Act
        HealthTreatmentResponse response = healthTreatmentService.recordPreventiveTreatment(
                preventive("D", "FRUIT_FLY", null, 1234.567, "L", 1.0, null), NOW);
        // Assert
        assertEquals(new BigDecimal("1234.567"), savedTreatment().getQuantity());
        assertEquals(1234.567, response.quantity());
    }

    @Test
    void getAllTreatments_shouldPassTheFilters_andComputeHarvestAllowedFrom() {
        // Arrange
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        HealthTreatment copper = new HealthTreatment(20L, 1, "C", ANTHRACNOSE, null,
                new HealthFinding(10L, blockC(1), ANTHRACNOSE, null, "42", null, null, "user_entry", NOW),
                SEPTEMBER_4, "Copper fungicide A", "copper hydroxide", new BigDecimal("2.500"), TreatmentUnit.KG, 14,
                "Awa", "knapsack sprayer", "user_entry", NOW);
        HealthTreatment bait = new HealthTreatment(21L, 1, "C", FRUIT_FLY, null, null, LocalDate.of(2026, 9, 5),
                "Protein bait B", "spinosad", new BigDecimal("1.000"), TreatmentUnit.L, 1, "Issa", null,
                "user_entry", NOW);
        when(healthTreatmentRepository.findByOptionalFilters(1, "C", from, to)).thenReturn(List.of(copper, bait));
        // Act
        List<HealthTreatmentResponse> treatments = healthTreatmentService.getAllTreatments(1, " C ", from, to);
        // Assert
        assertEquals(List.of(20L, 21L), treatments.stream().map(HealthTreatmentResponse::id).toList());
        assertEquals(10L, treatments.get(0).findingId());
        assertEquals(2.5, treatments.get(0).quantity());
        assertEquals(LocalDate.of(2026, 9, 18), treatments.get(0).harvestAllowedFrom());
        assertNull(treatments.get(1).findingId());
        assertEquals(LocalDate.of(2026, 9, 6), treatments.get(1).harvestAllowedFrom());
    }
}
