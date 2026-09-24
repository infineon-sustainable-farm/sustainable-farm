package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The error bodies are asserted in full, as for the planting and harvest routes. */
@WebMvcTest(FertilizerController.class)
@Import(CoreExceptionHandler.class)
public class FertilizerControllerTest {

    private static final String URL = "/api/plants/fertilizers";
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FertilizerService fertilizerService;

    private ResultActions postFertilizer(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Common part of every 4xx body: the application-wide ApiError envelope. */
    private static void assertApiError(ResultActions result, int status, String error, String message, String path)
            throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("uri=" + path))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private static FertilizerResponse npk(double currentStock, boolean belowThreshold) {
        return new FertilizerResponse(1L, "NPK 15-15-15", FertilizerType.MINERAL, "15-15-15", FertilizerUnit.KG,
                50.0, currentStock, belowThreshold, "user_entry", NOW);
    }

    @Test
    void createFertilizer_shouldReturn201WithTheFertilizer_whenRequestIsValid() throws Exception {
        // Arrange
        when(fertilizerService.createFertilizer(any(FertilizerRequest.class))).thenReturn(npk(0.0, true));
        // Act
        ResultActions result = postFertilizer("""
                {"name":"NPK 15-15-15","fertilizerType":"MINERAL","composition":"15-15-15","unit":"KG",
                 "reorderThreshold":50}
                """);
        // Assert: no Location header, the fertilizer in the body
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("NPK 15-15-15"))
                .andExpect(jsonPath("$.fertilizerType").value("MINERAL"))
                .andExpect(jsonPath("$.composition").value("15-15-15"))
                .andExpect(jsonPath("$.unit").value("KG"))
                .andExpect(jsonPath("$.reorderThreshold").value(50.0))
                .andExpect(jsonPath("$.currentStock").value(0.0))
                .andExpect(jsonPath("$.belowThreshold").value(true))
                .andExpect(jsonPath("$.source").value("user_entry"))
                .andExpect(jsonPath("$.lastUpdated").value("2026-09-24T10:00:00Z"));
    }

    @Test
    void createFertilizer_shouldAcceptCodesInAnyCaseWithSurroundingSpaces() throws Exception {
        // Arrange
        when(fertilizerService.createFertilizer(any(FertilizerRequest.class))).thenReturn(npk(0.0, true));
        // Act
        ResultActions result = postFertilizer("""
                {"name":"NPK 15-15-15","fertilizerType":" mineral ","unit":"kg"}
                """);
        // Assert: the values reach the service as sent; it normalizes them
        result.andExpect(status().isCreated());
        ArgumentCaptor<FertilizerRequest> captor = ArgumentCaptor.forClass(FertilizerRequest.class);
        verify(fertilizerService).createFertilizer(captor.capture());
        assertEquals(" mineral ", captor.getValue().fertilizerType());
        assertEquals("kg", captor.getValue().unit());
    }

    @Test
    void createFertilizer_shouldReturn400WithEveryFailingField_whenValuesAreInvalid() throws Exception {
        // Act: unknown codes give field errors, not the text of the JSON reader
        ResultActions result = postFertilizer("""
                {"name":"   ","fertilizerType":"X","composition":"%s","unit":"TONS","reorderThreshold":-1}
                """.formatted("1".repeat(101)));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed", URL);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(5)))
                .andExpect(jsonPath("$.fieldErrors.name").value("name is required"))
                .andExpect(jsonPath("$.fieldErrors.fertilizerType").value("fertilizerType must be MINERAL or ORGANIC"))
                .andExpect(jsonPath("$.fieldErrors.composition").value("composition must be at most 100 characters"))
                .andExpect(jsonPath("$.fieldErrors.unit").value("unit must be KG or L"))
                .andExpect(jsonPath("$.fieldErrors.reorderThreshold")
                        .value("reorderThreshold must be 0 or more, with at most 3 decimals"));
        verify(fertilizerService, never()).createFertilizer(any());
    }

    @Test
    void createFertilizer_shouldReturn400WithRequiredFields_whenBodyIsEmpty() throws Exception {
        // Act
        ResultActions result = postFertilizer("{}");
        // Assert: the composition and the threshold are optional
        assertApiError(result, 400, "Bad Request", "Validation failed", URL);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(3)))
                .andExpect(jsonPath("$.fieldErrors.name").value("name is required"))
                .andExpect(jsonPath("$.fieldErrors.fertilizerType").value("fertilizerType is required"))
                .andExpect(jsonPath("$.fieldErrors.unit").value("unit is required"));
    }

    @Test
    void createFertilizer_shouldReturn400_whenThresholdHasMoreThanThreeDecimals() throws Exception {
        // Act
        ResultActions result = postFertilizer("""
                {"name":"Urea","fertilizerType":"MINERAL","unit":"KG","reorderThreshold":0.1234}
                """);
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed", URL);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.reorderThreshold")
                        .value("reorderThreshold must be 0 or more, with at most 3 decimals"));
    }

    @Test
    void createFertilizer_shouldReturn400_whenUnitIsBlank() throws Exception {
        // Act
        ResultActions result = postFertilizer("""
                {"name":"Urea","fertilizerType":"MINERAL","unit":"  "}
                """);
        // Assert: a blank code fails the pattern, with its own message
        assertApiError(result, 400, "Bad Request", "Validation failed", URL);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.unit").value("unit must be KG or L"));
    }

    @Test
    void createFertilizer_shouldReturn409WithApiError_whenNameExists() throws Exception {
        // Arrange
        when(fertilizerService.createFertilizer(any(FertilizerRequest.class)))
                .thenThrow(new ConflictException("A fertilizer named NPK 15-15-15 already exists"));
        // Act
        ResultActions result = postFertilizer("""
                {"name":"npk 15-15-15","fertilizerType":"MINERAL","unit":"KG"}
                """);
        // Assert
        assertApiError(result, 409, "Conflict", "A fertilizer named NPK 15-15-15 already exists", URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void getAllFertilizers_shouldReturnTheCatalogueWithStocks() throws Exception {
        // Arrange
        when(fertilizerService.getAllFertilizers()).thenReturn(List.of(npk(300.0, false)));
        // Act & Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("NPK 15-15-15"))
                .andExpect(jsonPath("$[0].currentStock").value(300.0))
                .andExpect(jsonPath("$[0].belowThreshold").value(false));
    }

    @Test
    void getFertilizerById_shouldReturnTheFertilizer() throws Exception {
        // Arrange
        when(fertilizerService.getFertilizerById(1L)).thenReturn(npk(50.0, true));
        // Act & Assert
        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.currentStock").value(50.0))
                .andExpect(jsonPath("$.belowThreshold").value(true));
    }

    @Test
    void getFertilizerById_shouldReturn404WithApiError_whenIdDoesNotExist() throws Exception {
        // Arrange
        when(fertilizerService.getFertilizerById(999L)).thenThrow(new FertilizerNotFoundException(999L));
        // Act
        ResultActions result = mockMvc.perform(get(URL + "/999"));
        // Assert
        assertApiError(result, 404, "Not Found", "Fertilizer with ID 999 not found", URL + "/999");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }
}
