package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantScenarioRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantScenarioResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantScenario;
import com.infineonbit.sustainablefarm.modules.plants.exception.PlantScenarioNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.PlantScenarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PlantScenarioServiceTest {

    private static final Instant ENTERED = Instant.parse("2026-10-20T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");

    @Mock
    private PlantScenarioRepository plantScenarioRepository;

    @InjectMocks
    private PlantScenarioService plantScenarioService;

    /** The dev scenario as the loader writes it. */
    private static PlantScenario zalka() {
        return new PlantScenario(1L, "Zalka 2025", "Keitt", 200, "Zalka_2025_C43",
                220.0, "derived: Zalka_2025 D4 / B4", null, null,
                new BigDecimal("0.9072"), "derived: Zalka_2025 R19 / D4", "EUR",
                new BigDecimal("6707.00"), "Zalka_2025_L23", null, null,
                new BigDecimal("171512.00"), "Zalka_2025_H28", null, null, ENTERED);
    }

    private static PlantScenario named(long id, String name) {
        PlantScenario scenario = zalka();
        scenario.setId(id);
        scenario.setName(name);
        return scenario;
    }

    /** The values of the dev scenario, sent without any source. */
    private static PlantScenarioRequest zalkaWithoutSources() {
        return new PlantScenarioRequest("Zalka 2025", "Keitt", 200, null, 220.0, null, null, null,
                0.9072, null, "EUR", 6707.0, null, null, null, 171512.0, null, null, null);
    }

    private void stored(PlantScenario... rows) {
        when(plantScenarioRepository.findAll()).thenReturn(List.of(rows));
    }

    private PlantScenario correcting(PlantScenario recorded, PlantScenario... others) {
        when(plantScenarioRepository.findById(recorded.getId())).thenReturn(Optional.of(recorded));
        PlantScenario[] rows = new PlantScenario[others.length + 1];
        rows[0] = recorded;
        System.arraycopy(others, 0, rows, 1, others.length);
        stored(rows);
        when(plantScenarioRepository.saveAndFlush(recorded)).thenReturn(recorded);
        return recorded;
    }

    private void insertAssignsId(long id) {
        when(plantScenarioRepository.saveAndFlush(any(PlantScenario.class))).thenAnswer(invocation -> {
            PlantScenario scenario = invocation.getArgument(0);
            scenario.setId(id);
            return scenario;
        });
    }

    private PlantScenario writtenRow() {
        ArgumentCaptor<PlantScenario> captor = ArgumentCaptor.forClass(PlantScenario.class);
        verify(plantScenarioRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void getAllScenarios_shouldOrderByName_ignoringCaseAndAccents_thenById() {
        // Arrange
        stored(named(3L, "zalka 2025 bis"), named(1L, "Plan B"), named(4L, "Été 2027"), named(2L, "ete 2027"));
        // Act & Assert
        assertEquals(List.of("ete 2027", "Été 2027", "Plan B", "zalka 2025 bis"),
                plantScenarioService.getAllScenarios().stream().map(PlantScenarioResponse::name).toList());
    }

    @Test
    void getScenario_shouldGiveTheValuesAndTheirSources() {
        // Arrange
        when(plantScenarioRepository.findById(1L)).thenReturn(Optional.of(zalka()));
        // Act
        PlantScenarioResponse scenario = plantScenarioService.getScenario(1L);
        // Assert
        assertEquals(new PlantScenarioResponse(1L, "Zalka 2025", "Keitt", 200, "Zalka_2025_C43",
                220.0, "derived: Zalka_2025 D4 / B4", null, null, 0.9072, "derived: Zalka_2025 R19 / D4", "EUR",
                6707.0, "Zalka_2025_L23", null, null, 171512.0, "Zalka_2025_H28", null, null, ENTERED), scenario);
    }

    @Test
    void getScenario_shouldThrowNotFound_forAnUnknownId() {
        // Arrange
        when(plantScenarioRepository.findById(99L)).thenReturn(Optional.empty());
        // Act
        PlantScenarioNotFoundException refused = assertThrows(PlantScenarioNotFoundException.class,
                () -> plantScenarioService.getScenario(99L));
        // Assert
        assertEquals("Scenario with ID 99 not found", refused.getMessage());
    }

    @Test
    void createScenario_shouldTrimTheTexts_upperCaseTheCurrency_andCreditUserEntry() {
        // Arrange
        stored();
        insertAssignsId(5L);
        // Act: an optional value is sent without a source, another is left out with one
        PlantScenarioResponse created = plantScenarioService.createScenario(new PlantScenarioRequest(
                "  Plan B ", "  ", 120, " ", 180.0, null, 0.15, null, null, "ignored_source", " xof ",
                250000.0, null, 25.0, " transport_quote_2026 ", 3000000.0, "  ", null, "ignored_too"), NOW);
        // Assert
        PlantScenario inserted = writtenRow();
        assertEquals("Plan B", inserted.getName());
        assertNull(inserted.getVarietyName());
        assertEquals("XOF", inserted.getCurrency());
        assertEquals("user_entry", inserted.getTreeCountSource());
        assertEquals("user_entry", inserted.getYieldSource());
        assertEquals("user_entry", inserted.getLossRateSource());
        assertNull(inserted.getPricePerKg());
        assertNull(inserted.getPriceSource());
        assertEquals("user_entry", inserted.getAnnualFixedCostSource());
        assertEquals("transport_quote_2026", inserted.getVariableCostSource());
        assertEquals("user_entry", inserted.getInvestmentSource());
        assertNull(inserted.getCostPerAdditionalTree());
        assertNull(inserted.getCostPerAdditionalTreeSource());
        assertEquals(new PlantScenarioResponse(5L, "Plan B", null, 120, "user_entry", 180.0, "user_entry",
                0.15, "user_entry", null, null, "XOF", 250000.0, "user_entry", 25.0, "transport_quote_2026",
                3000000.0, "user_entry", null, null, NOW), created);
    }

    @Test
    void createScenario_shouldRefuseANameTaken_ignoringCaseAccentsAndSpaces() {
        // Arrange
        stored(named(1L, "Été 2027"));
        PlantScenarioRequest request = new PlantScenarioRequest(" ete 2027 ", null, 200, null, 220.0, null,
                null, null, null, null, "EUR", 6707.0, null, null, null, 171512.0, null, null, null);
        // Act
        ConflictException refused = assertThrows(ConflictException.class,
                () -> plantScenarioService.createScenario(request, NOW));
        // Assert: the message names the scenario as recorded
        assertEquals("A scenario named Été 2027 already exists", refused.getMessage());
        verify(plantScenarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void createScenario_shouldAnswer409_whenTheSameNameIsCommittedFirst() {
        // Arrange: the name is free when checked, taken when written
        stored();
        when(plantScenarioRepository.saveAndFlush(any(PlantScenario.class)))
                .thenThrow(new DataIntegrityViolationException("uk_plant_scenario_name"));
        // Act
        ConflictException refused = assertThrows(ConflictException.class,
                () -> plantScenarioService.createScenario(zalkaWithoutSources(), NOW));
        // Assert
        assertEquals("A scenario named Zalka 2025 already exists", refused.getMessage());
    }

    @Test
    void updateScenario_shouldKeepTheSourceOfAnUnchangedValue_andCreditUserEntryForAChangedOne() {
        // Arrange
        PlantScenario recorded = correcting(zalka());
        // Act: the price and the investment are sent again as they are, the trees and the fixed cost change
        plantScenarioService.updateScenario(1L, new PlantScenarioRequest("Zalka 2025", "Keitt", 240, null,
                220.0, null, null, null, 0.90720, null, "EUR", 7000.0, null, null, null, 171512.00, null,
                null, null), NOW);
        // Assert
        assertEquals("user_entry", recorded.getTreeCountSource());
        assertEquals("derived: Zalka_2025 D4 / B4", recorded.getYieldSource());
        assertEquals("derived: Zalka_2025 R19 / D4", recorded.getPriceSource());
        assertEquals("user_entry", recorded.getAnnualFixedCostSource());
        assertEquals("Zalka_2025_H28", recorded.getInvestmentSource());
        assertEquals(240, recorded.getTreeCount());
        assertEquals(0, new BigDecimal("7000").compareTo(recorded.getAnnualFixedCost()));
        assertEquals(NOW, recorded.getLastUpdated());
    }

    @Test
    void updateScenario_shouldRecordTheSourcesSent_andRemoveAnOptionalValueLeftOut() {
        // Arrange
        PlantScenario recorded = correcting(zalka());
        // Act: a loss rate entered with its source, the price left out
        PlantScenarioResponse corrected = plantScenarioService.updateScenario(1L, new PlantScenarioRequest(
                "Zalka 2025", "Keitt", 200, "farm_count_2026", 220.0, null, 0.12, " packhouse_records ",
                null, "ignored", "EUR", 6707.0, null, null, null, 171512.0, null, 15.0, null), NOW);
        // Assert
        assertEquals("farm_count_2026", recorded.getTreeCountSource());
        assertEquals("packhouse_records", recorded.getLossRateSource());
        assertNull(recorded.getPricePerKg());
        assertNull(recorded.getPriceSource());
        assertEquals("user_entry", recorded.getCostPerAdditionalTreeSource());
        assertEquals(0.12, corrected.lossRate());
        assertEquals(15.0, corrected.costPerAdditionalTree());
    }

    @Test
    void updateScenario_shouldLetAScenarioChangeTheCaseOfItsOwnName() {
        // Arrange
        PlantScenario recorded = correcting(zalka(), named(2L, "Plan B"));
        PlantScenarioRequest request = new PlantScenarioRequest("ZALKA 2025", "Keitt", 200, null, 220.0, null,
                null, null, 0.9072, null, "EUR", 6707.0, null, null, null, 171512.0, null, null, null);
        // Act
        plantScenarioService.updateScenario(1L, request, NOW);
        // Assert
        assertEquals("ZALKA 2025", recorded.getName());
    }

    @Test
    void updateScenario_shouldRefuseTheNameOfAnotherScenario() {
        // Arrange: nothing is written, so saveAndFlush is not stubbed
        when(plantScenarioRepository.findById(1L)).thenReturn(Optional.of(zalka()));
        stored(zalka(), named(2L, "Plan B"));
        PlantScenarioRequest request = new PlantScenarioRequest("plan b", "Keitt", 200, null, 220.0, null,
                null, null, 0.9072, null, "EUR", 6707.0, null, null, null, 171512.0, null, null, null);
        // Act
        ConflictException refused = assertThrows(ConflictException.class,
                () -> plantScenarioService.updateScenario(1L, request, NOW));
        // Assert
        assertEquals("A scenario named Plan B already exists", refused.getMessage());
    }

    @Test
    void updateScenario_shouldThrowNotFound_forAnUnknownId() {
        // Arrange
        when(plantScenarioRepository.findById(99L)).thenReturn(Optional.empty());
        // Act & Assert
        assertThrows(PlantScenarioNotFoundException.class,
                () -> plantScenarioService.updateScenario(99L, zalkaWithoutSources(), NOW));
        verify(plantScenarioRepository, never()).saveAndFlush(any());
    }
}
