package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthTreatment;
import com.infineonbit.sustainablefarm.modules.plants.entity.TreatmentUnit;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
public class HealthTreatmentRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private HealthIssueReferenceRepository healthIssueReferenceRepository;

    @Autowired
    private HealthInspectionRepository healthInspectionRepository;

    @Autowired
    private HealthFindingRepository healthFindingRepository;

    @Autowired
    private HealthTreatmentRepository healthTreatmentRepository;

    private HealthIssueReference anthracnose;
    private HealthIssueReference fruitFly;

    @BeforeEach
    void setUp() {
        anthracnose = healthIssueReferenceRepository.save(new HealthIssueReference(null, "ANTHRACNOSE", "Anthracnose",
                HealthIssueKind.DISEASE, null, null, "Dianda_2025", null));
        fruitFly = healthIssueReferenceRepository.save(new HealthIssueReference(null, "FRUIT_FLY", "Fruit flies",
                HealthIssueKind.PEST, null, null, "Zida_2023", null));
    }

    private HealthFinding finding(String blockCode) {
        HealthInspection inspection = new HealthInspection();
        inspection.setBlockCode(blockCode);
        inspection.setInspectedOn(LocalDate.of(2026, 9, 1));
        inspection.setHealthScorePct(48);
        inspection.setSource("user_entry");
        healthInspectionRepository.save(inspection);
        HealthFinding finding = new HealthFinding();
        finding.setInspection(inspection);
        finding.setIssue(anthracnose);
        finding.setSource("user_entry");
        return healthFindingRepository.save(finding);
    }

    private HealthTreatment treatment(Integer farmId, String blockCode, HealthFinding finding, LocalDate treatedOn,
                                      String quantity) {
        HealthTreatment treatment = new HealthTreatment();
        treatment.setFarmId(farmId);
        treatment.setBlockCode(blockCode);
        treatment.setTargetIssue(finding == null ? fruitFly : finding.getIssue());
        treatment.setFinding(finding);
        treatment.setTreatedOn(treatedOn);
        treatment.setProductName("Product");
        treatment.setActiveIngredient("ingredient");
        treatment.setQuantity(new BigDecimal(quantity));
        treatment.setUnit(TreatmentUnit.KG);
        treatment.setPreHarvestIntervalDays(14);
        treatment.setApplicator("Awa");
        treatment.setSource("user_entry");
        return healthTreatmentRepository.save(treatment);
    }

    private List<Long> find(Integer farmId, String blockCode, LocalDate from, LocalDate to) {
        return healthTreatmentRepository.findByOptionalFilters(farmId, blockCode, from, to).stream()
                .map(HealthTreatment::getId)
                .toList();
    }

    @Test
    void findByOptionalFilters_shouldOrderByDateThenId_andFilterOnFarmBlockAndDates() {
        // Arrange: saved out of date order, two on the same day
        HealthTreatment late = treatment(null, "C", null, LocalDate.of(2026, 9, 22), "1");
        HealthTreatment early = treatment(null, "D", null, LocalDate.of(2026, 9, 4), "1");
        HealthTreatment sameDay = treatment(1, "C", null, LocalDate.of(2026, 9, 4), "1");
        // Act & Assert: every treatment, by date then identifier
        assertEquals(List.of(early.getId(), sameDay.getId(), late.getId()), find(null, null, null, null));
        // Act & Assert: farm, block and dates, both included
        assertEquals(List.of(sameDay.getId()), find(1, null, null, null));
        assertEquals(List.of(sameDay.getId(), late.getId()), find(null, "C", null, null));
        assertEquals(List.of(early.getId(), sameDay.getId()),
                find(null, null, LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 4)));
        assertEquals(List.of(), find(null, null, LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 4)));
    }

    @Test
    void findByFindingIds_shouldReturnOnlyTheTreatmentsOfThoseFindings() {
        // Arrange
        HealthFinding first = finding("C");
        HealthFinding second = finding("C");
        HealthFinding notAskedFor = finding("D");
        HealthTreatment firstLate = treatment(null, "C", first, LocalDate.of(2026, 9, 10), "1");
        HealthTreatment firstEarly = treatment(null, "C", first, LocalDate.of(2026, 9, 4), "1");
        HealthTreatment secondOne = treatment(null, "C", second, LocalDate.of(2026, 9, 5), "1");
        treatment(null, "D", notAskedFor, LocalDate.of(2026, 9, 5), "1");
        treatment(null, "C", null, LocalDate.of(2026, 9, 5), "1");
        // Act
        List<HealthTreatment> treatments = healthTreatmentRepository.findByFindingIds(
                List.of(first.getId(), second.getId()));
        // Assert: preventive treatments and other findings are left out
        assertEquals(List.of(firstEarly.getId(), secondOne.getId(), firstLate.getId()),
                treatments.stream().map(HealthTreatment::getId).toList());
    }

    @Test
    void save_shouldKeepTheQuantityExact() {
        // Arrange
        HealthTreatment treatment = treatment(null, "C", null, LocalDate.of(2026, 9, 4), "1234.567");
        entityManager.flush();
        entityManager.clear();
        // Act
        HealthTreatment stored = healthTreatmentRepository.findById(treatment.getId()).orElseThrow();
        // Assert
        assertEquals(0, new BigDecimal("1234.567").compareTo(stored.getQuantity()));
    }
}
