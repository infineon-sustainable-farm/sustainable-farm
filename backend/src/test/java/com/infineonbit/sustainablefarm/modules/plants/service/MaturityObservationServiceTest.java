package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.MaturityObservationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.MaturityObservationResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityMethod;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityObservation;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.repository.MaturityObservationRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MaturityObservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");
    private static final LocalDate PLANTING_DATE = LocalDate.of(2015, 3, 1);
    private static final LocalDate OBSERVED_ON = LocalDate.of(2026, 10, 12);

    @Mock
    private VarietyRepository varietyRepository;

    @Mock
    private PopulationEventRepository populationEventRepository;

    @Mock
    private MaturityObservationRepository maturityObservationRepository;

    @InjectMocks
    private MaturityObservationService maturityObservationService;

    private static MaturityObservationRequest request(String blockCode, String varietyName, LocalDate observedOn,
                                                      String maturityStatus, String method, Double measuredValue,
                                                      String observer) {
        return new MaturityObservationRequest(null, blockCode, varietyName, observedOn, maturityStatus, method,
                measuredValue, observer);
    }

    private static Variety variety(Long id, Integer farmId, String blockCode, String name) {
        Variety variety = new Variety();
        variety.setId(id);
        variety.setFarmId(farmId);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        return variety;
    }

    private static PopulationEvent planting(Variety variety, LocalDate date) {
        return new PopulationEvent(50L, variety, PopulationEventType.PLANTING, date, 40, "user_entry", NOW);
    }

    /** Keitt planted on block B without farm on 1 March 2015, looked up by the given name. */
    private Variety keittPlantedOnB(String nameAsLookedUp) {
        Variety keitt = variety(10L, null, "B", "Keitt");
        when(varietyRepository.findByFarmBlockAndName(null, "B", nameAsLookedUp)).thenReturn(List.of(keitt));
        when(populationEventRepository.findFirstByVarietyIdAndEventTypeOrderByEventDateAsc(
                10L, PopulationEventType.PLANTING)).thenReturn(Optional.of(planting(keitt, PLANTING_DATE)));
        return keitt;
    }

    /** Saving the observation gives it an identifier, as the database would. */
    private void saveAssignsId(long id) {
        when(maturityObservationRepository.save(any(MaturityObservation.class))).thenAnswer(invocation -> {
            MaturityObservation observation = invocation.getArgument(0);
            observation.setId(id);
            return observation;
        });
    }

    private MaturityObservation savedObservation() {
        ArgumentCaptor<MaturityObservation> captor = ArgumentCaptor.forClass(MaturityObservation.class);
        verify(maturityObservationRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordObservation_shouldStoreTheObservation_whenVarietyIsPlanted() {
        // Arrange
        Variety keitt = keittPlantedOnB("Keitt");
        saveAssignsId(300L);
        // Act
        MaturityObservationResponse response = maturityObservationService.recordObservation(
                request("B", "Keitt", OBSERVED_ON, "READY_FOR_HARVEST", "BRIX", 14.2, "Awa"), NOW);
        // Assert: the stored observation, with its value at one decimal
        MaturityObservation observation = savedObservation();
        assertSame(keitt, observation.getVariety());
        assertEquals(OBSERVED_ON, observation.getObservedOn());
        assertEquals(MaturityStatus.READY_FOR_HARVEST, observation.getMaturityStatus());
        assertEquals(MaturityMethod.BRIX, observation.getMethod());
        assertEquals(new BigDecimal("14.2"), observation.getMeasuredValue());
        assertEquals("Awa", observation.getObserver());
        assertEquals("user_entry", observation.getSource());
        assertEquals(NOW, observation.getLastUpdated());
        // Assert: the response describes the variety row, with the unit of the method
        assertEquals(new MaturityObservationResponse(300L, null, "B", 10L, "Keitt", OBSERVED_ON,
                MaturityStatus.READY_FOR_HARVEST, MaturityMethod.BRIX, 14.2, "°Bx", "Awa", "user_entry", NOW),
                response);
    }

    @Test
    void recordObservation_shouldNormalizeTheCodesAndTheTexts() {
        // Arrange: " b " and " keitt " find the row of "Keitt" on block B
        keittPlantedOnB("keitt");
        saveAssignsId(300L);
        // Act
        MaturityObservationResponse response = maturityObservationService.recordObservation(
                request(" b ", " keitt ", OBSERVED_ON, " ripe ", " dry_matter ", 15.0, "  Awa Ouedraogo "), NOW);
        // Assert: the codes in upper case, the value with its decimal, the stored spelling of the variety
        MaturityObservation observation = savedObservation();
        assertEquals(MaturityStatus.RIPE, observation.getMaturityStatus());
        assertEquals(MaturityMethod.DRY_MATTER, observation.getMethod());
        assertEquals(new BigDecimal("15.0"), observation.getMeasuredValue());
        assertEquals("Awa Ouedraogo", observation.getObserver());
        assertEquals("B", response.blockCode());
        assertEquals("Keitt", response.varietyName());
        assertEquals(15.0, response.measuredValue());
        assertEquals("%", response.measuredUnit());
    }

    @Test
    void recordObservation_shouldStoreNoValueAndNoObserver_whenNoneIsGiven() {
        // Arrange
        keittPlantedOnB("Keitt");
        saveAssignsId(300L);
        // Act: a visual observation, with a blank observer
        MaturityObservationResponse response = maturityObservationService.recordObservation(
                request("B", "Keitt", OBSERVED_ON, "IMMATURE", "VISUAL", null, "   "), NOW);
        // Assert: neither a value nor a unit, and no observer rather than a blank one
        MaturityObservation observation = savedObservation();
        assertNull(observation.getMeasuredValue());
        assertNull(observation.getObserver());
        assertEquals(new MaturityObservationResponse(300L, null, "B", 10L, "Keitt", OBSERVED_ON,
                MaturityStatus.IMMATURE, MaturityMethod.VISUAL, null, null, null, "user_entry", NOW), response);
    }

    @Test
    void recordObservation_shouldAcceptAnObservationOnThePlantingDay() {
        // Arrange
        keittPlantedOnB("Keitt");
        saveAssignsId(300L);
        // Act & Assert: the planting date itself is not "before" the planting
        assertEquals(PLANTING_DATE, maturityObservationService.recordObservation(
                request("B", "Keitt", PLANTING_DATE, "IMMATURE", "FLESH_COLOR", null, null), NOW).observedOn());
    }

    @Test
    void recordObservation_shouldAcceptTwoObservationsOnTheSameDay() {
        // Arrange
        keittPlantedOnB("Keitt");
        saveAssignsId(300L);
        MaturityObservationRequest observation = request("B", "Keitt", OBSERVED_ON, "RIPE", "VISUAL", null, null);
        // Act
        maturityObservationService.recordObservation(observation, NOW);
        maturityObservationService.recordObservation(observation, NOW);
        // Assert: both stored, no duplicate rule
        verify(maturityObservationRepository, times(2)).save(any(MaturityObservation.class));
    }

    @Test
    void recordObservation_shouldLookUpTheFarmAsSent() {
        // Arrange: Keitt on block B of farm 1
        Variety keitt = variety(11L, 1, "B", "Keitt");
        when(varietyRepository.findByFarmBlockAndName(1, "B", "Keitt")).thenReturn(List.of(keitt));
        when(populationEventRepository.findFirstByVarietyIdAndEventTypeOrderByEventDateAsc(
                11L, PopulationEventType.PLANTING)).thenReturn(Optional.of(planting(keitt, PLANTING_DATE)));
        saveAssignsId(300L);
        // Act
        MaturityObservationResponse response = maturityObservationService.recordObservation(
                new MaturityObservationRequest(1, "B", "Keitt", OBSERVED_ON, "RIPE", "VISUAL", null, null), NOW);
        // Assert
        assertEquals(1, response.farmId());
        assertEquals(11L, response.varietyId());
    }

    @Test
    void recordObservation_shouldThrow422_whenVarietyIsNotOnTheBlock() {
        // Arrange: no Kent row on block C
        when(varietyRepository.findByFarmBlockAndName(null, "C", "Kent")).thenReturn(List.of());
        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> maturityObservationService.recordObservation(
                        request(" c ", "Kent", OBSERVED_ON, "RIPE", "VISUAL", null, null), NOW));
        // Assert: the name as sent, the block as normalized; nothing written
        assertEquals("No planting of Kent is recorded on block C", ex.getMessage());
        verifyNoInteractions(populationEventRepository);
        verify(maturityObservationRepository, never()).save(any());
    }

    @Test
    void recordObservation_shouldThrow422_whenVarietyRowHasNoPlanting() {
        // Arrange: the Zalka row of the dev profile, which has no PLANTING event
        Variety zalkaKeitt = variety(1L, null, "A", "Keitt");
        when(varietyRepository.findByFarmBlockAndName(null, "A", "keitt")).thenReturn(List.of(zalkaKeitt));
        when(populationEventRepository.findFirstByVarietyIdAndEventTypeOrderByEventDateAsc(
                1L, PopulationEventType.PLANTING)).thenReturn(Optional.empty());
        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> maturityObservationService.recordObservation(
                        request("A", "keitt", OBSERVED_ON, "RIPE", "VISUAL", null, null), NOW));
        // Assert: the stored name
        assertEquals("No planting of Keitt is recorded on block A", ex.getMessage());
        verify(maturityObservationRepository, never()).save(any());
    }

    @Test
    void recordObservation_shouldThrow422_whenObservationIsBeforeThePlanting() {
        // Arrange
        keittPlantedOnB("Keitt");
        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> maturityObservationService.recordObservation(
                        request("B", "Keitt", LocalDate.of(2014, 1, 1), "IMMATURE", "VISUAL", null, null), NOW));
        // Assert
        assertEquals("The observation date 2014-01-01 is before the planting date 2015-03-01 of Keitt on block B",
                ex.getMessage());
        verify(maturityObservationRepository, never()).save(any());
    }

    @Test
    void getAllObservations_shouldMapEveryObservation_andTurnABlankBlockIntoNoFilter() {
        // Arrange
        Variety keitt = variety(10L, 1, "B", "Keitt");
        MaturityObservation brix = new MaturityObservation(300L, keitt, OBSERVED_ON, MaturityStatus.RIPE,
                MaturityMethod.BRIX, new BigDecimal("12.0"), "Awa", "user_entry", NOW);
        when(maturityObservationRepository.findByOptionalFilters(1, null, OBSERVED_ON, null))
                .thenReturn(List.of(brix));
        // Act
        List<MaturityObservationResponse> observations =
                maturityObservationService.getAllObservations(1, "  ", OBSERVED_ON, null);
        // Assert
        assertEquals(List.of(new MaturityObservationResponse(300L, 1, "B", 10L, "Keitt", OBSERVED_ON,
                MaturityStatus.RIPE, MaturityMethod.BRIX, 12.0, "°Bx", "Awa", "user_entry", NOW)), observations);
    }
}
