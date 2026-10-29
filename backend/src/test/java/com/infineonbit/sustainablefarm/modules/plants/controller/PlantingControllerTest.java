package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantingResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.ConcurrentPlantingRetry;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantingService;
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

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error bodies are asserted in full, not only the status: this route is the
 * first write of the module and the template for the next ones.
 */
@WebMvcTest(PlantingController.class)
@Import({CoreExceptionHandler.class, ConcurrentPlantingRetry.class})
public class PlantingControllerTest {

    private static final String URL = "/api/plants/plantings";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlantingService plantingService;

    private ResultActions postPlanting(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Common part of every 4xx body: the application-wide ApiError envelope. */
    private static void assertApiError(ResultActions result, int status, String error, String message)
            throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("uri=" + URL))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void recordPlanting_shouldReturn201WithThePlanting_whenRequestIsValid() throws Exception {
        // Arrange
        when(plantingService.recordPlanting(any(PlantingRequest.class))).thenReturn(new PlantingResponse(
                100L, null, "A", 10L, "Keitt", LocalDate.of(2023, 9, 24), 150, "user_entry",
                Instant.parse("2026-09-24T10:00:00Z")));
        // Act
        ResultActions result = postPlanting("""
                {"blockCode":" a ","varietyName":"Keitt","plantingDate":"2023-09-24","treeCount":150}
                """);
        // Assert: surrounding spaces pass the validation and reach the service as sent
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.farmId").value(nullValue()))
                .andExpect(jsonPath("$.blockCode").value("A"))
                .andExpect(jsonPath("$.varietyId").value(10))
                .andExpect(jsonPath("$.varietyName").value("Keitt"))
                .andExpect(jsonPath("$.plantingDate").value("2023-09-24"))
                .andExpect(jsonPath("$.treeCount").value(150))
                .andExpect(jsonPath("$.source").value("user_entry"))
                .andExpect(jsonPath("$.lastUpdated").value("2026-09-24T10:00:00Z"));
        ArgumentCaptor<PlantingRequest> captor = ArgumentCaptor.forClass(PlantingRequest.class);
        verify(plantingService).recordPlanting(captor.capture());
        assertNull(captor.getValue().farmId());
        assertEquals(" a ", captor.getValue().blockCode());
        assertEquals(LocalDate.of(2023, 9, 24), captor.getValue().plantingDate());
    }

    @Test
    void recordPlanting_shouldReturn409WithApiError_whenVarietyIsAlreadyPlanted() throws Exception {
        // Arrange
        when(plantingService.recordPlanting(any(PlantingRequest.class)))
                .thenThrow(new ConflictException("A planting of Keitt is already recorded on block A"));
        // Act
        ResultActions result = postPlanting("""
                {"blockCode":"A","varietyName":"Keitt","plantingDate":"2023-09-24","treeCount":150}
                """);
        // Assert
        assertApiError(result, 409, "Conflict", "A planting of Keitt is already recorded on block A");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void recordPlanting_shouldReturn400WithEveryFailingField_whenValuesAreInvalid() throws Exception {
        // Arrange
        String future = LocalDate.now().plusYears(1).toString();
        // Act
        ResultActions result = postPlanting("""
                {"farmId":0,"blockCode":"Block A","varietyName":"   ","plantingDate":"%s","treeCount":0}
                """.formatted(future));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(5)))
                .andExpect(jsonPath("$.fieldErrors.farmId").value("farmId must be at least 1"))
                .andExpect(jsonPath("$.fieldErrors.blockCode")
                        .value("blockCode must be a short code such as A or B2, without prefix or space"))
                .andExpect(jsonPath("$.fieldErrors.varietyName").value("varietyName is required"))
                .andExpect(jsonPath("$.fieldErrors.plantingDate").value("plantingDate must be today or in the past"))
                .andExpect(jsonPath("$.fieldErrors.treeCount").value("treeCount must be at least 1"));
        verify(plantingService, never()).recordPlanting(any());
    }

    @Test
    void recordPlanting_shouldReturn400WithRequiredFields_whenBodyIsEmpty() throws Exception {
        // Act
        ResultActions result = postPlanting("{}");
        // Assert: the farm is optional, every other field is required
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(4)))
                .andExpect(jsonPath("$.fieldErrors.blockCode").value("blockCode is required"))
                .andExpect(jsonPath("$.fieldErrors.varietyName").value("varietyName is required"))
                .andExpect(jsonPath("$.fieldErrors.plantingDate").value("plantingDate is required"))
                .andExpect(jsonPath("$.fieldErrors.treeCount").value("treeCount is required"))
                .andExpect(jsonPath("$.fieldErrors.farmId").doesNotExist());
        verify(plantingService, never()).recordPlanting(any());
    }

    @Test
    void recordPlanting_shouldReturn400_whenBlockCodeIsBlank() throws Exception {
        // Act
        ResultActions result = postPlanting("""
                {"blockCode":"   ","varietyName":"Keitt","plantingDate":"2023-09-24","treeCount":150}
                """);
        // Assert: a blank code fails the pattern, and only the pattern
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.blockCode")
                        .value("blockCode must be a short code such as A or B2, without prefix or space"));
    }

    @Test
    void recordPlanting_shouldReturn400_whenVarietyNameIsTooLong() throws Exception {
        // Act
        ResultActions result = postPlanting("""
                {"blockCode":"A","varietyName":"%s","plantingDate":"2023-09-24","treeCount":150}
                """.formatted("x".repeat(256)));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.varietyName").value("varietyName must be at most 255 characters"));
    }

    @Test
    void recordPlanting_shouldReturn400WithoutFieldErrors_whenBodyIsUnreadable() throws Exception {
        // Act: a date that is not a date never reaches the validation
        ResultActions result = postPlanting("""
                {"blockCode":"A","varietyName":"Keitt","plantingDate":"not-a-date","treeCount":150}
                """);
        // Assert: the client shows such an error as a message, not under a field
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.fieldErrors").value(nullValue()));
        verify(plantingService, never()).recordPlanting(any());
    }

    /** A violation of a unique constraint, as the repository layer reports it. */
    private static DataIntegrityViolationException violationOf(String constraintName) {
        return new DataIntegrityViolationException("could not execute statement", new ConstraintViolationException(
                "duplicate key", new SQLException("duplicate key", "23505"), constraintName));
    }

    private static PlantingResponse keittOnBlockA() {
        return new PlantingResponse(100L, null, "A", 10L, "Keitt", LocalDate.of(2023, 9, 24), 150, "user_entry",
                Instant.parse("2026-09-24T10:00:00Z"));
    }

    @Test
    void recordPlanting_shouldRunThePlantingOnceMore_whenItLostARaceOnTheVarietyKey() throws Exception {
        // Arrange: the first run loses the race, the second one finds the row of the winner
        when(plantingService.recordPlanting(any(PlantingRequest.class)))
                .thenThrow(violationOf("uk_varietes_variety_key"))
                .thenReturn(keittOnBlockA());
        // Act
        ResultActions result = postPlanting("""
                {"blockCode":"A","varietyName":"Keitt","plantingDate":"2023-09-24","treeCount":150}
                """);
        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100));
        verify(plantingService, times(2)).recordPlanting(any(PlantingRequest.class));
    }

    @Test
    void recordPlanting_shouldReturn409AndStop_whenTheSecondRunLosesARaceAgain() throws Exception {
        // Arrange: both runs lose a race, for example with three plantings or more on a new block
        when(plantingService.recordPlanting(any(PlantingRequest.class)))
                .thenThrow(violationOf("uk_calendrier_croissance_block_key"));
        // Act
        ResultActions result = postPlanting("""
                {"blockCode":" c ","varietyName":"Kent","plantingDate":"2021-03-01","treeCount":50}
                """);
        // Assert: a clear 409 naming the block as stored, never a 500, and no third run
        assertApiError(result, 409, "Conflict", "Another planting on block C was being recorded at the same time. "
                + "Nothing was saved: please send the request again.");
        verify(plantingService, times(2)).recordPlanting(any(PlantingRequest.class));
    }

    @Test
    void recordPlanting_shouldReturn500WithoutRunningAgain_whenAnotherConstraintIsViolated() throws Exception {
        // Arrange: not a lost race on a natural key, so not something a second run can fix
        when(plantingService.recordPlanting(any(PlantingRequest.class)))
                .thenThrow(violationOf("fk_population_event_variety"));
        // Act
        ResultActions result = postPlanting("""
                {"blockCode":"A","varietyName":"Keitt","plantingDate":"2023-09-24","treeCount":150}
                """);
        // Assert
        result.andExpect(status().isInternalServerError());
        verify(plantingService, times(1)).recordPlanting(any(PlantingRequest.class));
    }
}
