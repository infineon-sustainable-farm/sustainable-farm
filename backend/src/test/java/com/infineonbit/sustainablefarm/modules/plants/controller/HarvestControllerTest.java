package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HarvestRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.exception.HarvestNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.ConcurrentPlantingRetry;
import com.infineonbit.sustainablefarm.modules.plants.service.HarvestService;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The error bodies are asserted in full, as for the planting route. */
@WebMvcTest(HarvestController.class)
@Import({CoreExceptionHandler.class, ConcurrentPlantingRetry.class})
public class HarvestControllerTest {

    private static final String URL = "/api/plants/harvests";
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HarvestService harvestService;

    private ResultActions postHarvest(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Common part of every 4xx body: the application-wide ApiError envelope. */
    private static void assertApiError(ResultActions result, int status, String error, String message)
            throws Exception {
        assertApiError(result, status, error, message, URL);
    }

    /** Same envelope, for a request to the given path, such as one harvest. */
    private static void assertApiError(ResultActions result, int status, String error, String message, String path)
            throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("uri=" + path))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private static HarvestResponse keittHarvest() {
        return new HarvestResponse(200L, "LOT-2026-B-001", null, "B", 10L, "Keitt", LocalDate.of(2026, 6, 10), 800.0,
                "user_entry", NOW);
    }

    @Test
    void recordHarvest_shouldReturn201WithTheHarvest_whenRequestIsValid() throws Exception {
        // Arrange
        when(harvestService.recordHarvest(any(HarvestRequest.class))).thenReturn(keittHarvest());
        // Act
        ResultActions result = postHarvest("""
                {"blockCode":" b ","varietyName":"Keitt","harvestDate":"2026-06-10","quantityKg":800}
                """);
        // Assert: no Location header, the harvest in the body
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.lotNumber").value("LOT-2026-B-001"))
                .andExpect(jsonPath("$.farmId").value(nullValue()))
                .andExpect(jsonPath("$.blockCode").value("B"))
                .andExpect(jsonPath("$.varietyId").value(10))
                .andExpect(jsonPath("$.varietyName").value("Keitt"))
                .andExpect(jsonPath("$.harvestDate").value("2026-06-10"))
                .andExpect(jsonPath("$.quantityKg").value(800.0))
                .andExpect(jsonPath("$.source").value("user_entry"))
                .andExpect(jsonPath("$.lastUpdated").value("2026-09-24T10:00:00Z"));
        // Assert: surrounding spaces pass the validation and reach the service as sent
        ArgumentCaptor<HarvestRequest> captor = ArgumentCaptor.forClass(HarvestRequest.class);
        verify(harvestService).recordHarvest(captor.capture());
        assertNull(captor.getValue().farmId());
        assertEquals(" b ", captor.getValue().blockCode());
        assertEquals(800.0, captor.getValue().quantityKg());
    }

    @Test
    void recordHarvest_shouldIgnoreALotNumberSentWithTheRequest() throws Exception {
        // Arrange: the lot number is given by the server, never taken from the client
        when(harvestService.recordHarvest(any(HarvestRequest.class))).thenReturn(keittHarvest());
        // Act
        ResultActions result = postHarvest("""
                {"blockCode":"B","varietyName":"Keitt","harvestDate":"2026-06-10","quantityKg":800,
                 "lotNumber":"LOT-1999-Z-999"}
                """);
        // Assert: recorded, with the lot number the service gave
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.lotNumber").value("LOT-2026-B-001"));
    }

    /** A violation of a unique constraint, as the repository layer reports it. */
    private static DataIntegrityViolationException violationOf(String constraintName) {
        return new DataIntegrityViolationException("could not execute statement", new ConstraintViolationException(
                "duplicate key", new SQLException("duplicate key", "23505"), constraintName));
    }

    @Test
    void recordHarvest_shouldRunTheHarvestOnceMore_whenItLostARaceOnTheLotNumber() throws Exception {
        // Arrange: another harvest of block B took the same number; the second run takes the next one
        when(harvestService.recordHarvest(any(HarvestRequest.class)))
                .thenThrow(violationOf("uk_harvest_record_lot_number"))
                .thenReturn(keittHarvest());
        // Act
        ResultActions result = postHarvest("""
                {"blockCode":"B","varietyName":"Keitt","harvestDate":"2026-06-10","quantityKg":800}
                """);
        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.lotNumber").value("LOT-2026-B-001"));
        verify(harvestService, times(2)).recordHarvest(any(HarvestRequest.class));
    }

    @Test
    void recordHarvest_shouldReturn409AndStop_whenTheSecondRunLosesARaceAgain() throws Exception {
        // Arrange: both runs lose, for example with three harvests or more of the block at once;
        // the constraint is named as H2 reports it
        when(harvestService.recordHarvest(any(HarvestRequest.class))).thenThrow(
                violationOf("PUBLIC.UK_HARVEST_RECORD_LOT_NUMBER INDEX PUBLIC.UK_HARVEST_RECORD_LOT_NUMBER_INDEX_A"));
        // Act
        ResultActions result = postHarvest("""
                {"blockCode":" b ","varietyName":"Keitt","harvestDate":"2026-06-10","quantityKg":800}
                """);
        // Assert: a clear 409 naming the block as stored, never a 500, and no third run
        assertApiError(result, 409, "Conflict", "Another harvest on block B was being recorded at the same time. "
                + "Nothing was saved: please send the request again.");
        verify(harvestService, times(2)).recordHarvest(any(HarvestRequest.class));
    }

    @Test
    void recordHarvest_shouldReturn422WithApiError_whenVarietyHasNoPlanting() throws Exception {
        // Arrange
        when(harvestService.recordHarvest(any(HarvestRequest.class)))
                .thenThrow(new BusinessRuleException("No planting of Kent is recorded on block C"));
        // Act
        ResultActions result = postHarvest("""
                {"blockCode":"C","varietyName":"Kent","harvestDate":"2026-06-10","quantityKg":800}
                """);
        // Assert
        assertApiError(result, 422, "Unprocessable Entity", "No planting of Kent is recorded on block C");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void recordHarvest_shouldReturn422WithApiError_whenHarvestIsBeforeThePlanting() throws Exception {
        // Arrange
        String message = "The harvest date 2023-01-01 is before the planting date 2023-09-24 of Keitt on block B";
        when(harvestService.recordHarvest(any(HarvestRequest.class))).thenThrow(new BusinessRuleException(message));
        // Act
        ResultActions result = postHarvest("""
                {"blockCode":"B","varietyName":"Keitt","harvestDate":"2023-01-01","quantityKg":800}
                """);
        // Assert
        assertApiError(result, 422, "Unprocessable Entity", message);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void recordHarvest_shouldReturn400WithEveryFailingField_whenValuesAreInvalid() throws Exception {
        // Arrange
        String future = LocalDate.now().plusYears(1).toString();
        // Act
        ResultActions result = postHarvest("""
                {"farmId":0,"blockCode":"Block B","varietyName":"   ","harvestDate":"%s","quantityKg":0}
                """.formatted(future));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(5)))
                .andExpect(jsonPath("$.fieldErrors.farmId").value("farmId must be at least 1"))
                .andExpect(jsonPath("$.fieldErrors.blockCode")
                        .value("blockCode must be a short code such as A or B2, without prefix or space"))
                .andExpect(jsonPath("$.fieldErrors.varietyName").value("varietyName is required"))
                .andExpect(jsonPath("$.fieldErrors.harvestDate").value("harvestDate must be today or in the past"))
                .andExpect(jsonPath("$.fieldErrors.quantityKg").value("quantityKg must be greater than 0"));
        verify(harvestService, never()).recordHarvest(any());
    }

    @Test
    void recordHarvest_shouldReturn400WithRequiredFields_whenBodyIsEmpty() throws Exception {
        // Act
        ResultActions result = postHarvest("{}");
        // Assert: the farm is optional, every other field is required
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(4)))
                .andExpect(jsonPath("$.fieldErrors.blockCode").value("blockCode is required"))
                .andExpect(jsonPath("$.fieldErrors.varietyName").value("varietyName is required"))
                .andExpect(jsonPath("$.fieldErrors.harvestDate").value("harvestDate is required"))
                .andExpect(jsonPath("$.fieldErrors.quantityKg").value("quantityKg is required"))
                .andExpect(jsonPath("$.fieldErrors.farmId").doesNotExist());
        verify(harvestService, never()).recordHarvest(any());
    }

    @Test
    void recordHarvest_shouldReturn400_whenQuantityIsNegative() throws Exception {
        // Act
        ResultActions result = postHarvest("""
                {"blockCode":"B","varietyName":"Keitt","harvestDate":"2026-06-10","quantityKg":-5}
                """);
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.quantityKg").value("quantityKg must be greater than 0"));
    }

    @Test
    void getAllHarvests_shouldPassEveryFilter() throws Exception {
        // Arrange
        when(harvestService.getAllHarvests(1, "B", null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(List.of(keittHarvest()));
        // Act
        ResultActions result = mockMvc.perform(get(URL)
                .param("farmId", "1")
                .param("blockCode", "B")
                .param("from", "2026-06-01")
                .param("to", "2026-06-30"));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(200))
                .andExpect(jsonPath("$[0].lotNumber").value("LOT-2026-B-001"))
                .andExpect(jsonPath("$[0].harvestDate").value("2026-06-10"));
    }

    @Test
    void getAllHarvests_shouldPassTheLotNumberAsSent() throws Exception {
        // Arrange: the service trims and upper-cases it
        when(harvestService.getAllHarvests(null, null, " lot-2026-b-001 ", null, null))
                .thenReturn(List.of(keittHarvest()));
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("lotNumber", " lot-2026-b-001 "));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lotNumber").value("LOT-2026-B-001"));
    }

    @Test
    void getAllHarvests_shouldReturnAnEmptyList_forAnUnknownLotNumber() throws Exception {
        // Arrange
        when(harvestService.getAllHarvests(null, null, "LOT-1999-Z-999", null, null)).thenReturn(List.of());
        // Act & Assert: a filter matching nothing, not a 404
        mockMvc.perform(get(URL).param("lotNumber", "LOT-1999-Z-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllHarvests_shouldPassNulls_whenNoFilterIsGiven() throws Exception {
        // Arrange
        when(harvestService.getAllHarvests(null, null, null, null, null)).thenReturn(List.of());
        // Act & Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllHarvests_shouldReturn400WithApiError_whenADateIsNotIso() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("from", "10/06/2026"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'from'");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
        verifyNoInteractions(harvestService);
    }

    @Test
    void getHarvestById_shouldReturn200WithTheHarvest() throws Exception {
        // Arrange: a harvest of farm 1, so that no field is null
        when(harvestService.getHarvestById(200L)).thenReturn(new HarvestResponse(200L, "LOT-2026-B-001", 1, "B",
                10L, "Keitt", LocalDate.of(2026, 6, 10), 800.0, "user_entry", NOW));
        // Act
        ResultActions result = mockMvc.perform(get(URL + "/200"));
        // Assert: every field of the harvest, and no other
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(10)))
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.lotNumber").value("LOT-2026-B-001"))
                .andExpect(jsonPath("$.farmId").value(1))
                .andExpect(jsonPath("$.blockCode").value("B"))
                .andExpect(jsonPath("$.varietyId").value(10))
                .andExpect(jsonPath("$.varietyName").value("Keitt"))
                .andExpect(jsonPath("$.harvestDate").value("2026-06-10"))
                .andExpect(jsonPath("$.quantityKg").value(800.0))
                .andExpect(jsonPath("$.source").value("user_entry"))
                .andExpect(jsonPath("$.lastUpdated").value("2026-09-24T10:00:00Z"));
    }

    @Test
    void getHarvestById_shouldReturn404WithApiError_whenTheHarvestIsUnknown() throws Exception {
        // Arrange
        when(harvestService.getHarvestById(999L)).thenThrow(new HarvestNotFoundException(999L));
        // Act
        ResultActions result = mockMvc.perform(get(URL + "/999"));
        // Assert
        assertApiError(result, 404, "Not Found", "Harvest with ID 999 not found", URL + "/999");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }
}
