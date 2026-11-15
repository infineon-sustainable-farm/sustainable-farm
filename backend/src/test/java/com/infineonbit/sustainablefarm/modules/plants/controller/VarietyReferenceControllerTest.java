package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.VarietyReferenceRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyReferenceResponse;
import com.infineonbit.sustainablefarm.modules.plants.exception.VarietyReferenceNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.VarietyReferenceService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error bodies are asserted in full, as for the fertilizer routes.
 *
 * <p>The context is closed after the class. The test context cache keeps 32
 * contexts, and one more cached context would evict a {@code @SpringBootTest}
 * context, whose create-drop then empties the database that the other
 * integration tests still use.
 */
@DirtiesContext
@WebMvcTest(VarietyReferenceController.class)
@Import(CoreExceptionHandler.class)
public class VarietyReferenceControllerTest {

    private static final String URL = "/api/plants/variety-references";
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
    private static final String YIELD_MESSAGE =
            "yieldPerTreeKg must be greater than 0 and at most 1000, with at most 1 decimal";
    private static final String KEITT_BODY = """
            {"varietyName":"Keitt","yieldPerTreeKg":220,"harvestStartMonth":5,"harvestEndMonth":7}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VarietyReferenceService varietyReferenceService;

    private ResultActions postReference(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions putReference(String id, String body) throws Exception {
        return mockMvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static VarietyReferenceResponse keitt(String yieldSource, String seasonSource) {
        return new VarietyReferenceResponse(1L, "Keitt", 220.0, yieldSource, 5, 7, seasonSource, NOW);
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

    private static void assertKeittBody(ResultActions result, String yieldSource, String seasonSource)
            throws Exception {
        result.andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$", aMapWithSize(8)))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.varietyName").value("Keitt"))
                .andExpect(jsonPath("$.yieldPerTreeKg").value(220.0))
                .andExpect(jsonPath("$.yieldSource").value(yieldSource))
                .andExpect(jsonPath("$.harvestStartMonth").value(5))
                .andExpect(jsonPath("$.harvestEndMonth").value(7))
                .andExpect(jsonPath("$.seasonSource").value(seasonSource))
                .andExpect(jsonPath("$.lastUpdated").value("2026-10-08T10:00:00Z"));
    }

    /** A POST refused before the service, with the given messages and no other. */
    private void assertRefused(ResultActions result, String... fieldsAndMessages) throws Exception {
        assertApiError(result, 400, "Bad Request", "Validation failed", URL);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(fieldsAndMessages.length / 2)));
        for (int i = 0; i < fieldsAndMessages.length; i += 2) {
            result.andExpect(jsonPath("$.fieldErrors." + fieldsAndMessages[i]).value(fieldsAndMessages[i + 1]));
        }
        verifyNoInteractions(varietyReferenceService);
    }

    @Test
    void getAllReferences_shouldReturn200WithTheReference() throws Exception {
        // Arrange
        when(varietyReferenceService.getAllReferences()).thenReturn(List.of(
                new VarietyReferenceResponse(3L, "Amelie", 160.0, "Zalka_2025", 2, 4, "FAO_mango_burkina", NOW),
                keitt("Zalka_2025", "varietal_guide_west_africa")));
        // Act
        ResultActions result = mockMvc.perform(get(URL));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].varietyName").value("Amelie"))
                .andExpect(jsonPath("$[1].seasonSource").value("varietal_guide_west_africa"));
    }

    @Test
    void getAllReferences_shouldReturn200WithAnEmptyList_whenNothingIsEntered() throws Exception {
        // Arrange
        when(varietyReferenceService.getAllReferences()).thenReturn(List.of());
        // Act & Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createReference_shouldReturn201WithTheReference() throws Exception {
        // Arrange
        when(varietyReferenceService.createReference(any(VarietyReferenceRequest.class)))
                .thenReturn(keitt("user_entry", "user_entry"));
        // Act: the sources may be left out
        ResultActions result = postReference(KEITT_BODY);
        // Assert: the request reaches the service as sent
        result.andExpect(status().isCreated());
        assertKeittBody(result, "user_entry", "user_entry");
        ArgumentCaptor<VarietyReferenceRequest> captor = ArgumentCaptor.forClass(VarietyReferenceRequest.class);
        verify(varietyReferenceService).createReference(captor.capture());
        assertEquals(new VarietyReferenceRequest("Keitt", 220.0, null, 5, 7, null), captor.getValue());
    }

    @Test
    void createReference_shouldReturn400_whenTheNameIsBlank() throws Exception {
        assertRefused(postReference("""
                {"varietyName":"  ","yieldPerTreeKg":220,"harvestStartMonth":5,"harvestEndMonth":7}
                """), "varietyName", "varietyName is required");
    }

    @Test
    void createReference_shouldReturn400_whenTheValuesAreMissing() throws Exception {
        assertRefused(postReference("""
                        {"varietyName":"Keitt"}
                        """),
                "yieldPerTreeKg", "yieldPerTreeKg is required",
                "harvestStartMonth", "harvestStartMonth is required",
                "harvestEndMonth", "harvestEndMonth is required");
    }

    @Test
    void createReference_shouldReturn400_whenTheYieldIsZero() throws Exception {
        assertRefused(postReference("""
                {"varietyName":"Keitt","yieldPerTreeKg":0,"harvestStartMonth":5,"harvestEndMonth":7}
                """), "yieldPerTreeKg", YIELD_MESSAGE);
    }

    @Test
    void createReference_shouldReturn400_whenTheYieldIsAboveOneThousandKilograms() throws Exception {
        // Act & Assert: 1000 is accepted, a typing slip such as 2200 for 220 is not
        assertRefused(postReference("""
                {"varietyName":"Keitt","yieldPerTreeKg":2200,"harvestStartMonth":5,"harvestEndMonth":7}
                """), "yieldPerTreeKg", YIELD_MESSAGE);
        assertRefused(postReference("""
                {"varietyName":"Keitt","yieldPerTreeKg":1000.1,"harvestStartMonth":5,"harvestEndMonth":7}
                """), "yieldPerTreeKg", YIELD_MESSAGE);
    }

    @Test
    void createReference_shouldReturn400_whenTheYieldHasMoreThanOneDecimal() throws Exception {
        // Act & Assert: -0.15 is also negative, and still gets one message
        assertRefused(postReference("""
                {"varietyName":"Keitt","yieldPerTreeKg":220.25,"harvestStartMonth":5,"harvestEndMonth":7}
                """), "yieldPerTreeKg", YIELD_MESSAGE);
        assertRefused(postReference("""
                {"varietyName":"Keitt","yieldPerTreeKg":-0.15,"harvestStartMonth":5,"harvestEndMonth":7}
                """), "yieldPerTreeKg", YIELD_MESSAGE);
    }

    @Test
    void createReference_shouldReturn400_whenAMonthIsOutsideOneToTwelve() throws Exception {
        assertRefused(postReference("""
                        {"varietyName":"Keitt","yieldPerTreeKg":220,"harvestStartMonth":0,"harvestEndMonth":13}
                        """),
                "harvestStartMonth", "harvestStartMonth must be between 1 and 12",
                "harvestEndMonth", "harvestEndMonth must be between 1 and 12");
    }

    @Test
    void createReference_shouldReturn400_whenATextIsTooLong() throws Exception {
        String tooLong = "s".repeat(256);
        assertRefused(postReference("""
                        {"varietyName":"%s","yieldPerTreeKg":220,"yieldSource":"%s",
                         "harvestStartMonth":5,"harvestEndMonth":7,"seasonSource":"%s"}
                        """.formatted(tooLong, tooLong, tooLong)),
                "varietyName", "varietyName must be at most 255 characters",
                "yieldSource", "yieldSource must be at most 255 characters",
                "seasonSource", "seasonSource must be at most 255 characters");
    }

    @Test
    void createReference_shouldReturn409WithApiError_whenTheNameIsTaken() throws Exception {
        // Arrange
        when(varietyReferenceService.createReference(any(VarietyReferenceRequest.class)))
                .thenThrow(new ConflictException("A variety reference named Keitt already exists"));
        // Act
        ResultActions result = postReference(KEITT_BODY);
        // Assert
        assertApiError(result, 409, "Conflict", "A variety reference named Keitt already exists", URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void updateReference_shouldReturn200WithTheCorrectedReference() throws Exception {
        // Arrange
        when(varietyReferenceService.updateReference(eq(1L), any(VarietyReferenceRequest.class)))
                .thenReturn(keitt("Zalka_2025", "user_entry"));
        // Act
        ResultActions result = putReference("1", KEITT_BODY);
        // Assert
        result.andExpect(status().isOk());
        assertKeittBody(result, "Zalka_2025", "user_entry");
        verify(varietyReferenceService).updateReference(1L, new VarietyReferenceRequest("Keitt", 220.0, null, 5, 7, null));
    }

    @Test
    void updateReference_shouldReturn404WithApiError_forAnUnknownId() throws Exception {
        // Arrange
        when(varietyReferenceService.updateReference(eq(99L), any(VarietyReferenceRequest.class)))
                .thenThrow(new VarietyReferenceNotFoundException(99L));
        // Act
        ResultActions result = putReference("99", KEITT_BODY);
        // Assert
        assertApiError(result, 404, "Not Found", "Variety reference with ID 99 not found", URL + "/99");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void updateReference_shouldReturn409WithApiError_whenAnotherReferenceHasTheName() throws Exception {
        // Arrange
        when(varietyReferenceService.updateReference(eq(1L), any(VarietyReferenceRequest.class)))
                .thenThrow(new ConflictException("A variety reference named Kent already exists"));
        // Act
        ResultActions result = putReference("1", """
                {"varietyName":"kent","yieldPerTreeKg":220,"harvestStartMonth":5,"harvestEndMonth":7}
                """);
        // Assert
        assertApiError(result, 409, "Conflict", "A variety reference named Kent already exists", URL + "/1");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void updateReference_shouldReturn400_whenTheIdIsNotANumber() throws Exception {
        // Act
        ResultActions result = putReference("abc", KEITT_BODY);
        // Assert
        assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'id'", URL + "/abc");
        verify(varietyReferenceService, never()).updateReference(anyLong(), any());
    }
}
