package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantScenarioRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantScenarioResponse;
import com.infineonbit.sustainablefarm.modules.plants.exception.PlantScenarioNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantScenarioService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error bodies are asserted in full, as for the variety references.
 *
 * <p>The context is closed after the class. The test context cache keeps 32
 * contexts, and one more cached context would evict a {@code @SpringBootTest}
 * context, whose create-drop then empties the database that the other
 * integration tests still use.
 */
@DirtiesContext
@WebMvcTest(PlantScenarioController.class)
@Import(CoreExceptionHandler.class)
public class PlantScenarioControllerTest {

    private static final String URL = "/api/plants/scenarios";
    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");
    private static final String ZALKA_BODY = """
            {"name":"Zalka 2025","treeCount":200,"yieldPerTreeKg":220,"pricePerKg":0.9072,"currency":"EUR",
             "annualFixedCost":6707,"initialInvestment":171512}
            """;
    private static final PlantScenarioRequest ZALKA_REQUEST = new PlantScenarioRequest("Zalka 2025", null, 200,
            null, 220.0, null, null, null, 0.9072, null, "EUR", 6707.0, null, null, null, 171512.0, null, null, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlantScenarioService plantScenarioService;

    private ResultActions postScenario(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions putScenario(String id, String body) throws Exception {
        return mockMvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static PlantScenarioResponse zalka() {
        return new PlantScenarioResponse(1L, "Zalka 2025", "Keitt", 200, "Zalka_2025_C43", 220.0,
                "derived: Zalka_2025 D4 / B4", null, null, 0.9072, "derived: Zalka_2025 R19 / D4", "EUR",
                6707.0, "Zalka_2025_L23", null, null, 171512.0, "Zalka_2025_H28", null, null, NOW);
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

    /** A request refused before any service, with the given messages and no other. */
    private void assertRefused(ResultActions result, String path, String... fieldsAndMessages) throws Exception {
        assertApiError(result, 400, "Bad Request", "Validation failed", path);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(fieldsAndMessages.length / 2)));
        for (int i = 0; i < fieldsAndMessages.length; i += 2) {
            result.andExpect(jsonPath("$.fieldErrors." + fieldsAndMessages[i]).value(fieldsAndMessages[i + 1]));
        }
        verifyNoInteractions(plantScenarioService);
    }

    @Test
    void getAllScenarios_shouldReturn200WithTheScenarios() throws Exception {
        // Arrange
        when(plantScenarioService.getAllScenarios()).thenReturn(List.of(zalka()));
        // Act & Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Zalka 2025"))
                .andExpect(jsonPath("$[0].priceSource").value("derived: Zalka_2025 R19 / D4"));
    }

    @Test
    void getScenario_shouldReturn200WithEveryValue_andNullForWhatIsNotEntered() throws Exception {
        // Arrange
        when(plantScenarioService.getScenario(1L)).thenReturn(zalka());
        // Act & Assert
        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(21)))
                .andExpect(jsonPath("$.treeCount").value(200))
                .andExpect(jsonPath("$.pricePerKg").value(0.9072))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.lossRate").value(nullValue()))
                .andExpect(jsonPath("$.lossRateSource").value(nullValue()))
                .andExpect(jsonPath("$.lastUpdated").value("2026-10-25T10:00:00Z"));
    }

    @Test
    void getScenario_shouldReturn404WithApiError_forAnUnknownId() throws Exception {
        // Arrange
        when(plantScenarioService.getScenario(99L)).thenThrow(new PlantScenarioNotFoundException(99L));
        // Act & Assert
        assertApiError(mockMvc.perform(get(URL + "/99")), 404, "Not Found", "Scenario with ID 99 not found",
                URL + "/99");
    }

    @Test
    void createScenario_shouldReturn201_andPassTheRequestAsSent() throws Exception {
        // Arrange
        when(plantScenarioService.createScenario(any(PlantScenarioRequest.class))).thenReturn(zalka());
        // Act
        ResultActions result = postScenario(ZALKA_BODY);
        // Assert
        result.andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1));
        ArgumentCaptor<PlantScenarioRequest> captor = ArgumentCaptor.forClass(PlantScenarioRequest.class);
        verify(plantScenarioService).createScenario(captor.capture());
        assertEquals(ZALKA_REQUEST, captor.getValue());
    }

    @Test
    void createScenario_shouldReturn400_whenTheRequiredValuesAreMissing() throws Exception {
        assertRefused(postScenario("{}"), URL,
                "name", "name is required",
                "treeCount", "treeCount is required",
                "yieldPerTreeKg", "yieldPerTreeKg is required",
                "currency", "currency is required",
                "annualFixedCost", "annualFixedCost is required",
                "initialInvestment", "initialInvestment is required");
    }

    @Test
    void createScenario_shouldReturn400_whenAValueIsOutOfBounds() throws Exception {
        assertRefused(postScenario("""
                        {"name":"Zalka 2025","treeCount":0,"yieldPerTreeKg":2200,"lossRate":1.5,"pricePerKg":0,
                         "currency":"USD","annualFixedCost":-1,"variableCostPerKg":-0.1,"initialInvestment":0,
                         "costPerAdditionalTree":-5}
                        """), URL,
                "treeCount", "treeCount must be between 1 and 1000000",
                "yieldPerTreeKg", "yieldPerTreeKg must be greater than 0 and at most 1000, with at most 1 decimal",
                "lossRate", "lossRate must be between 0 and 1, with at most 3 decimals",
                "pricePerKg", "pricePerKg must be greater than 0, with at most 4 decimals",
                "currency", "currency must be XOF or EUR",
                "annualFixedCost", "annualFixedCost must be 0 or more, with at most 2 decimals",
                "variableCostPerKg", "variableCostPerKg must be 0 or more, with at most 4 decimals",
                "initialInvestment", "initialInvestment must be greater than 0, with at most 2 decimals",
                "costPerAdditionalTree", "costPerAdditionalTree must be 0 or more, with at most 2 decimals");
    }

    @Test
    void createScenario_shouldReturn400_whenAValueHasTooManyDecimals() throws Exception {
        assertRefused(postScenario("""
                        {"name":"Zalka 2025","treeCount":200,"yieldPerTreeKg":220.25,"lossRate":0.1234,
                         "pricePerKg":0.90722,"currency":"EUR","annualFixedCost":6707.001,
                         "variableCostPerKg":0.00001,"initialInvestment":171512.001,"costPerAdditionalTree":12.345}
                        """), URL,
                "yieldPerTreeKg", "yieldPerTreeKg must be greater than 0 and at most 1000, with at most 1 decimal",
                "lossRate", "lossRate must be between 0 and 1, with at most 3 decimals",
                "pricePerKg", "pricePerKg must be greater than 0, with at most 4 decimals",
                "annualFixedCost", "annualFixedCost must be 0 or more, with at most 2 decimals",
                "variableCostPerKg", "variableCostPerKg must be 0 or more, with at most 4 decimals",
                "initialInvestment", "initialInvestment must be greater than 0, with at most 2 decimals",
                "costPerAdditionalTree", "costPerAdditionalTree must be 0 or more, with at most 2 decimals");
    }

    @Test
    void createScenario_shouldReturn400_whenATextIsTooLong_orTheNameIsBlank() throws Exception {
        String tooLong = "s".repeat(256);
        assertRefused(postScenario("""
                        {"name":"  ","varietyName":"%s","treeCount":200,"treeCountSource":"%s","yieldPerTreeKg":220,
                         "yieldSource":"%s","lossRateSource":"%s","priceSource":"%s","currency":"EUR",
                         "annualFixedCost":6707,"annualFixedCostSource":"%s","variableCostSource":"%s",
                         "initialInvestment":171512,"investmentSource":"%s","costPerAdditionalTreeSource":"%s"}
                        """.formatted(tooLong, tooLong, tooLong, tooLong, tooLong, tooLong, tooLong, tooLong,
                        tooLong)), URL,
                "name", "name is required",
                "varietyName", "varietyName must be at most 255 characters",
                "treeCountSource", "treeCountSource must be at most 255 characters",
                "yieldSource", "yieldSource must be at most 255 characters",
                "lossRateSource", "lossRateSource must be at most 255 characters",
                "priceSource", "priceSource must be at most 255 characters",
                "annualFixedCostSource", "annualFixedCostSource must be at most 255 characters",
                "variableCostSource", "variableCostSource must be at most 255 characters",
                "investmentSource", "investmentSource must be at most 255 characters",
                "costPerAdditionalTreeSource", "costPerAdditionalTreeSource must be at most 255 characters");
    }

    @Test
    void createScenario_shouldAcceptTheBounds() throws Exception {
        // Arrange
        when(plantScenarioService.createScenario(any(PlantScenarioRequest.class))).thenReturn(zalka());
        // Act & Assert: the smallest and largest values each field takes, and a currency in lower case
        postScenario("""
                {"name":"Edge","treeCount":1,"yieldPerTreeKg":0.1,"lossRate":0,"pricePerKg":0.0001,
                 "currency":" xof ","annualFixedCost":0,"variableCostPerKg":0,"initialInvestment":0.01,
                 "costPerAdditionalTree":0}
                """).andExpect(status().isCreated());
        postScenario("""
                {"name":"Edge","treeCount":1000000,"yieldPerTreeKg":1000,"lossRate":1,"currency":"EUR",
                 "annualFixedCost":6707,"initialInvestment":171512}
                """).andExpect(status().isCreated());
    }

    @Test
    void createScenario_shouldReturn409WithApiError_whenTheNameIsTaken() throws Exception {
        // Arrange
        when(plantScenarioService.createScenario(any(PlantScenarioRequest.class)))
                .thenThrow(new ConflictException("A scenario named Zalka 2025 already exists"));
        // Act
        ResultActions result = postScenario(ZALKA_BODY);
        // Assert
        assertApiError(result, 409, "Conflict", "A scenario named Zalka 2025 already exists", URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void updateScenario_shouldReturn200WithTheCorrectedScenario() throws Exception {
        // Arrange
        when(plantScenarioService.updateScenario(eq(1L), any(PlantScenarioRequest.class))).thenReturn(zalka());
        // Act & Assert
        putScenario("1", ZALKA_BODY).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Zalka 2025"));
        verify(plantScenarioService).updateScenario(1L, ZALKA_REQUEST);
    }

    @Test
    void updateScenario_shouldReturn404Or409WithApiError() throws Exception {
        // Arrange
        when(plantScenarioService.updateScenario(eq(99L), any(PlantScenarioRequest.class)))
                .thenThrow(new PlantScenarioNotFoundException(99L));
        when(plantScenarioService.updateScenario(eq(1L), any(PlantScenarioRequest.class)))
                .thenThrow(new ConflictException("A scenario named Plan B already exists"));
        // Act & Assert
        assertApiError(putScenario("99", ZALKA_BODY), 404, "Not Found", "Scenario with ID 99 not found",
                URL + "/99");
        assertApiError(putScenario("1", ZALKA_BODY), 409, "Conflict", "A scenario named Plan B already exists",
                URL + "/1");
    }

    @Test
    void updateScenario_shouldReturn400_whenTheIdIsNotANumber() throws Exception {
        // Act & Assert
        assertApiError(putScenario("abc", ZALKA_BODY), 400, "Bad Request", "Invalid value for parameter 'id'",
                URL + "/abc");
        verify(plantScenarioService, never()).updateScenario(anyLong(), any());
    }
}
