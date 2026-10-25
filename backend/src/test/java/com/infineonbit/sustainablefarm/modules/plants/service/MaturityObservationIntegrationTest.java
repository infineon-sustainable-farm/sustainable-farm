package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.MaturityObservationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.MaturityObservationResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityMethod;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.repository.MaturityObservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Maturity observations through the real services and the database. The
 * variety of an observation is a lazy association and open-in-view is
 * disabled, so the list is built after the session of the read is closed; a
 * mocked repository cannot show that the variety is loaded with the
 * observations. In CI the database is PostgreSQL, so the measured value is
 * read back from its numeric column there.
 *
 * <p>Farm 8 and its blocks are used by no other test.
 */
@SpringBootTest
@ActiveProfiles("test")
class MaturityObservationIntegrationTest {

    private static final int FARM = 8;

    @Autowired
    private PlantingService plantingService;

    @Autowired
    private MaturityObservationService maturityObservationService;

    @Autowired
    private MaturityObservationRepository maturityObservationRepository;

    @Test
    void getAllObservations_shouldReturnEveryField_withTheVarietyLoadedWithTheObservations() {
        // Arrange: Keitt planted on block MO1 of farm 8, then a Brix reading sent in any case
        Long varietyId = plantingService.recordPlanting(
                new PlantingRequest(FARM, "MO1", "Keitt", LocalDate.of(2015, 3, 1), 40)).varietyId();
        Long observationId = maturityObservationService.recordObservation(new MaturityObservationRequest(FARM,
                " mo1 ", " keitt ", LocalDate.of(2026, 10, 12), "ready_for_harvest", "brix", 14.2, " Awa ")).id();
        // The write time as stored, at the precision of the database
        Instant lastUpdated = maturityObservationRepository.findById(observationId).orElseThrow().getLastUpdated();
        // Act
        List<MaturityObservationResponse> observations =
                maturityObservationService.getAllObservations(FARM, "MO1", null, null);
        // Assert: every field, the value with its unit, the variety as planted
        assertEquals(List.of(new MaturityObservationResponse(observationId, FARM, "MO1", varietyId, "Keitt",
                LocalDate.of(2026, 10, 12), MaturityStatus.READY_FOR_HARVEST, MaturityMethod.BRIX, 14.2, "°Bx",
                "Awa", "user_entry", lastUpdated)), observations);
    }
}
