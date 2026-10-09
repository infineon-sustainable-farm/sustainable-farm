package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.GrowthPhaseYieldShareRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthPhaseYieldShareResponse;
import com.infineonbit.sustainablefarm.modules.plants.exception.GrowthPhaseNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.GrowthPhaseYieldShareService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error bodies are asserted in full, as for the exchange rate.
 *
 * <p>The context is closed after the class. The test context cache keeps 32
 * contexts, and one more cached context would evict a {@code @SpringBootTest}
 * context, whose create-drop then empties the database that the other
 * integration tests still use.
 */
@DirtiesContext
@WebMvcTest(GrowthPhaseYieldShareController.class)
@Import(CoreExceptionHandler.class)
public class GrowthPhaseYieldShareControllerTest {

    private static final String URL = "/api/plants/growth-phase-yield-shares";
    private static final String GRADUAL_URL = URL + "/GRADUAL_PRODUCTION";
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
    private static final String SHARE_MESSAGE = "yieldShare must be between 0 and 1, with at most 3 decimals";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrowthPhaseYieldShareService growthPhaseYieldShareService;

    private ResultActions putShare(String url, String body) throws Exception {
        return mockMvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(body));
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

    private void assertRefused(ResultActions result, String field, String message) throws Exception {
        assertApiError(result, 400, "Bad Request", "Validation failed", GRADUAL_URL);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors." + field).value(message));
        verify(growthPhaseYieldShareService, never()).saveShare(anyString(), any());
    }

    @Test
    void getAllShares_shouldReturn200WithEveryPhase() throws Exception {
        // Arrange: gradual production corrected, the other two by default
        when(growthPhaseYieldShareService.getAllShares()).thenReturn(List.of(
                new GrowthPhaseYieldShareResponse("ESTABLISHMENT", "establishment", "0–2 yrs", 0.0,
                        "orchard_literature", false, 0.0, "orchard_literature", null),
                new GrowthPhaseYieldShareResponse("GRADUAL_PRODUCTION", "gradual production", "3–5 yrs", 0.3,
                        "user_entry", true, 0.25, "Bally_2002", NOW),
                new GrowthPhaseYieldShareResponse("FULL_PRODUCTION", "full production", "6+ yrs", 1.0,
                        "by_definition", false, 1.0, "by_definition", null)));
        // Act
        ResultActions result = mockMvc.perform(get(URL));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[1]", aMapWithSize(9)))
                .andExpect(jsonPath("$[1].code").value("GRADUAL_PRODUCTION"))
                .andExpect(jsonPath("$[1].growthPhase").value("gradual production"))
                .andExpect(jsonPath("$[1].yearsBand").value("3–5 yrs"))
                .andExpect(jsonPath("$[1].yieldShare").value(0.3))
                .andExpect(jsonPath("$[1].source").value("user_entry"))
                .andExpect(jsonPath("$[1].corrected").value(true))
                .andExpect(jsonPath("$[1].defaultYieldShare").value(0.25))
                .andExpect(jsonPath("$[1].defaultSource").value("Bally_2002"))
                .andExpect(jsonPath("$[1].lastUpdated").value("2026-10-08T10:00:00Z"))
                .andExpect(jsonPath("$[0].corrected").value(false))
                .andExpect(jsonPath("$[0].lastUpdated").value(nullValue()))
                .andExpect(jsonPath("$[2].code").value("FULL_PRODUCTION"));
    }

    @Test
    void saveShare_shouldReturn200_evenForTheFirstCorrection() throws Exception {
        // Arrange
        when(growthPhaseYieldShareService.saveShare(anyString(), any(GrowthPhaseYieldShareRequest.class)))
                .thenReturn(new GrowthPhaseYieldShareResponse("GRADUAL_PRODUCTION", "gradual production",
                        "3–5 yrs", 0.3, "user_entry", true, 0.25, "Bally_2002", NOW));
        // Act: the source may be left out
        ResultActions result = putShare(GRADUAL_URL, """
                {"yieldShare":0.3}
                """);
        // Assert: the phase always had a share, so the correction replaces it
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.yieldShare").value(0.3))
                .andExpect(jsonPath("$.corrected").value(true));
        ArgumentCaptor<GrowthPhaseYieldShareRequest> captor =
                ArgumentCaptor.forClass(GrowthPhaseYieldShareRequest.class);
        verify(growthPhaseYieldShareService).saveShare(eq("GRADUAL_PRODUCTION"), captor.capture());
        assertEquals(new GrowthPhaseYieldShareRequest(0.3, null), captor.getValue());
    }

    @Test
    void saveShare_shouldReturn400_whenTheShareIsMissing() throws Exception {
        assertRefused(putShare(GRADUAL_URL, "{}"), "yieldShare", "yieldShare is required");
    }

    @Test
    void saveShare_shouldReturn400_whenTheShareIsAboveOne() throws Exception {
        assertRefused(putShare(GRADUAL_URL, """
                {"yieldShare":1.5}
                """), "yieldShare", SHARE_MESSAGE);
    }

    @Test
    void saveShare_shouldReturn400_whenTheShareIsNegative() throws Exception {
        assertRefused(putShare(GRADUAL_URL, """
                {"yieldShare":-0.1}
                """), "yieldShare", SHARE_MESSAGE);
    }

    @Test
    void saveShare_shouldReturn400_whenTheShareHasMoreThanThreeDecimals() throws Exception {
        assertRefused(putShare(GRADUAL_URL, """
                {"yieldShare":0.1234}
                """), "yieldShare", SHARE_MESSAGE);
    }

    @Test
    void saveShare_shouldReturn400_whenTheSourceIsTooLong() throws Exception {
        assertRefused(putShare(GRADUAL_URL, """
                {"yieldShare":0.3,"source":"%s"}
                """.formatted("s".repeat(256))), "source", "source must be at most 255 characters");
    }

    @Test
    void saveShare_shouldReturn404WithApiError_forAnUnknownCode() throws Exception {
        // Arrange
        when(growthPhaseYieldShareService.saveShare(eq("ADULT"), any(GrowthPhaseYieldShareRequest.class)))
                .thenThrow(new GrowthPhaseNotFoundException("ADULT",
                        List.of("ESTABLISHMENT", "GRADUAL_PRODUCTION", "FULL_PRODUCTION")));
        // Act
        ResultActions result = putShare(URL + "/ADULT", """
                {"yieldShare":0.8}
                """);
        // Assert
        assertApiError(result, 404, "Not Found", "No growth phase with code ADULT. "
                + "The codes are ESTABLISHMENT, GRADUAL_PRODUCTION and FULL_PRODUCTION", URL + "/ADULT");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void saveShare_shouldReturn409WithApiError_whenAnotherFirstCorrectionIsRecordedAtTheSameTime() throws Exception {
        // Arrange
        String message = "Another yield share for the gradual production phase was being recorded at the same "
                + "time. Nothing was saved: please send the request again.";
        when(growthPhaseYieldShareService.saveShare(anyString(), any(GrowthPhaseYieldShareRequest.class)))
                .thenThrow(new ConflictException(message));
        // Act
        ResultActions result = putShare(GRADUAL_URL, """
                {"yieldShare":0.3}
                """);
        // Assert
        assertApiError(result, 409, "Conflict", message, GRADUAL_URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }
}
