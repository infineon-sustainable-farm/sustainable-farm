package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.CurrencyRateRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.CurrencyRateResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;
import com.infineonbit.sustainablefarm.modules.plants.exception.CurrencyRateNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.CurrencyRateService;
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

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The error bodies are asserted in full, as for the fertilizer routes. */
@WebMvcTest(CurrencyRateController.class)
@Import(CoreExceptionHandler.class)
public class CurrencyRateControllerTest {

    private static final String URL = "/api/plants/currency-rates/EUR/XOF";
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
    private static final String RATE_MESSAGE =
            "rate must be greater than 0 and less than 1000000, with at most 6 decimals";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrencyRateService currencyRateService;

    private ResultActions putRate(String body) throws Exception {
        return mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static CurrencyRateResponse rate(double rate, String source) {
        return new CurrencyRateResponse("EUR", "XOF", rate, source, NOW);
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

    private static void assertRateBody(ResultActions result, double rate, String source) throws Exception {
        result.andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.baseCurrency").value("EUR"))
                .andExpect(jsonPath("$.quoteCurrency").value("XOF"))
                .andExpect(jsonPath("$.rate").value(rate))
                .andExpect(jsonPath("$.source").value(source))
                .andExpect(jsonPath("$.lastUpdated").value("2026-10-08T10:00:00Z"));
    }

    private void assertRefused(ResultActions result, String field, String message) throws Exception {
        assertApiError(result, 400, "Bad Request", "Validation failed", URL);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors." + field).value(message));
        verify(currencyRateService, never()).saveEurToXof(any());
    }

    @Test
    void getEurToXofRate_shouldReturn200WithTheRate() throws Exception {
        // Arrange
        when(currencyRateService.getEurToXof()).thenReturn(rate(655.957, "BCEAO_fixed_parity_1999"));
        // Act
        ResultActions result = mockMvc.perform(get(URL));
        // Assert
        result.andExpect(status().isOk());
        assertRateBody(result, 655.957, "BCEAO_fixed_parity_1999");
    }

    @Test
    void getEurToXofRate_shouldReturn404WithApiError_whenNoRateIsRecorded() throws Exception {
        // Arrange
        when(currencyRateService.getEurToXof())
                .thenThrow(new CurrencyRateNotFoundException(CurrencyCode.EUR, CurrencyCode.XOF));
        // Act
        ResultActions result = mockMvc.perform(get(URL));
        // Assert
        assertApiError(result, 404, "Not Found", "No EUR to XOF rate recorded yet", URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void anotherPair_shouldReturn404_sinceOnlyEurToXofExists() throws Exception {
        // Act & Assert: the path is literal, so neither another pair nor another case is mapped
        assertApiError(mockMvc.perform(get("/api/plants/currency-rates/EUR/USD")), 404, "Not Found",
                "Resource not found", "/api/plants/currency-rates/EUR/USD");
        assertApiError(mockMvc.perform(get("/api/plants/currency-rates/eur/xof")), 404, "Not Found",
                "Resource not found", "/api/plants/currency-rates/eur/xof");
    }

    @Test
    void saveEurToXofRate_shouldReturn201_whenItIsTheFirstRate() throws Exception {
        // Arrange
        when(currencyRateService.saveEurToXof(any(CurrencyRateRequest.class)))
                .thenReturn(new CurrencyRateService.SavedRate(rate(655.957, "BCEAO_fixed_parity_1999"), true));
        // Act
        ResultActions result = putRate("""
                {"rate":655.957,"source":"BCEAO_fixed_parity_1999"}
                """);
        // Assert: the request reaches the service as sent
        result.andExpect(status().isCreated());
        assertRateBody(result, 655.957, "BCEAO_fixed_parity_1999");
        ArgumentCaptor<CurrencyRateRequest> captor = ArgumentCaptor.forClass(CurrencyRateRequest.class);
        verify(currencyRateService).saveEurToXof(captor.capture());
        assertEquals(new CurrencyRateRequest(655.957, "BCEAO_fixed_parity_1999"), captor.getValue());
    }

    @Test
    void saveEurToXofRate_shouldReturn200_whenItReplacesTheRate() throws Exception {
        // Arrange
        when(currencyRateService.saveEurToXof(any(CurrencyRateRequest.class)))
                .thenReturn(new CurrencyRateService.SavedRate(rate(656.0, "user_entry"), false));
        // Act: the source may be left out
        ResultActions result = putRate("""
                {"rate":656}
                """);
        // Assert
        result.andExpect(status().isOk());
        assertRateBody(result, 656.0, "user_entry");
        ArgumentCaptor<CurrencyRateRequest> captor = ArgumentCaptor.forClass(CurrencyRateRequest.class);
        verify(currencyRateService).saveEurToXof(captor.capture());
        assertNull(captor.getValue().source());
    }

    @Test
    void saveEurToXofRate_shouldReturn400_whenTheRateIsMissing() throws Exception {
        assertRefused(putRate("{}"), "rate", "rate is required");
    }

    @Test
    void saveEurToXofRate_shouldReturn400_whenTheRateIsZero() throws Exception {
        assertRefused(putRate("""
                {"rate":0}
                """), "rate", RATE_MESSAGE);
    }

    @Test
    void saveEurToXofRate_shouldReturn400_whenTheRateIsNegative() throws Exception {
        // Act & Assert: -0.1234567 also has too many decimals, and still gets one message
        assertRefused(putRate("""
                {"rate":-0.1234567}
                """), "rate", RATE_MESSAGE);
    }

    @Test
    void saveEurToXofRate_shouldReturn400_whenTheRateHasMoreThanSixDecimals() throws Exception {
        assertRefused(putRate("""
                {"rate":655.9570001}
                """), "rate", RATE_MESSAGE);
    }

    @Test
    void saveEurToXofRate_shouldReturn400_whenTheRateDoesNotFitTheColumn() throws Exception {
        assertRefused(putRate("""
                {"rate":1000000}
                """), "rate", RATE_MESSAGE);
    }

    @Test
    void saveEurToXofRate_shouldReturn400_whenTheSourceIsTooLong() throws Exception {
        assertRefused(putRate("""
                {"rate":655.957,"source":"%s"}
                """.formatted("s".repeat(256))), "source", "source must be at most 255 characters");
    }

    @Test
    void saveEurToXofRate_shouldReturn409WithApiError_whenAnotherFirstRateIsRecordedAtTheSameTime() throws Exception {
        // Arrange
        when(currencyRateService.saveEurToXof(any(CurrencyRateRequest.class))).thenThrow(new ConflictException(
                "Another EUR to XOF rate was being recorded at the same time. "
                        + "Nothing was saved: please send the request again."));
        // Act
        ResultActions result = putRate("""
                {"rate":655.957}
                """);
        // Assert
        assertApiError(result, 409, "Conflict", "Another EUR to XOF rate was being recorded at the same time. "
                + "Nothing was saved: please send the request again.", URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }
}
