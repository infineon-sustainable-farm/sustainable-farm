package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.MaturityObservationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.MaturityObservationResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityMethod;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.service.MaturityObservationService;
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
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error bodies are asserted in full, as for the harvest route.
 *
 * <p>The context is closed after the class, as for the harvest status: one
 * more cached context would evict a {@code @SpringBootTest} context, whose
 * create-drop then empties the database that the other integration tests
 * still use.
 */
@DirtiesContext
@WebMvcTest(MaturityObservationController.class)
@Import(CoreExceptionHandler.class)
public class MaturityObservationControllerTest {

    private static final String URL = "/api/plants/maturity-observations";
    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");
    private static final String VALUE_RANGE = "measuredValue must be above 0 and at most 100, with at most one decimal";
    private static final String VALUE_LEFT_OUT = "measuredValue must be left out unless method is BRIX or DRY_MATTER";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaturityObservationService maturityObservationService;

    private ResultActions postObservation(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** A valid body with the given method and value fields, for example {@code "method":"BRIX","measuredValue":14.2}. */
    private ResultActions postWith(String methodAndValue) throws Exception {
        return postObservation("""
                {"blockCode":"A","varietyName":"Keitt","observedOn":"2026-10-12","maturityStatus":"RIPE",%s}
                """.formatted(methodAndValue));
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

    /** A 400 whose only failing field is the measured value, with the given message. */
    private static void assertValueRefused(ResultActions result, String message) throws Exception {
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.measuredValue").value(message));
    }

    private static MaturityObservationResponse brixObservation() {
        return new MaturityObservationResponse(300L, null, "A", 10L, "Keitt", LocalDate.of(2026, 10, 12),
                MaturityStatus.READY_FOR_HARVEST, MaturityMethod.BRIX, 14.2, "°Bx", "Awa", "user_entry", NOW);
    }

    @Test
    void recordObservation_shouldReturn201WithTheObservation_whenRequestIsValid() throws Exception {
        // Arrange
        when(maturityObservationService.recordObservation(any(MaturityObservationRequest.class)))
                .thenReturn(brixObservation());
        // Act
        ResultActions result = postObservation("""
                {"blockCode":" a ","varietyName":"Keitt","observedOn":"2026-10-12",
                 "maturityStatus":"READY_FOR_HARVEST","method":"BRIX","measuredValue":14.2,"observer":"Awa"}
                """);
        // Assert: no Location header, every field of the observation in the body
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$", aMapWithSize(13)))
                .andExpect(jsonPath("$.id").value(300))
                .andExpect(jsonPath("$.farmId").value(nullValue()))
                .andExpect(jsonPath("$.blockCode").value("A"))
                .andExpect(jsonPath("$.varietyId").value(10))
                .andExpect(jsonPath("$.varietyName").value("Keitt"))
                .andExpect(jsonPath("$.observedOn").value("2026-10-12"))
                .andExpect(jsonPath("$.maturityStatus").value("READY_FOR_HARVEST"))
                .andExpect(jsonPath("$.method").value("BRIX"))
                .andExpect(jsonPath("$.measuredValue").value(14.2))
                .andExpect(jsonPath("$.measuredUnit").value("°Bx"))
                .andExpect(jsonPath("$.observer").value("Awa"))
                .andExpect(jsonPath("$.source").value("user_entry"))
                .andExpect(jsonPath("$.lastUpdated").value("2026-10-25T10:00:00Z"));
        // Assert: surrounding spaces pass the validation and reach the service as sent
        ArgumentCaptor<MaturityObservationRequest> captor = ArgumentCaptor.forClass(MaturityObservationRequest.class);
        verify(maturityObservationService).recordObservation(captor.capture());
        assertNull(captor.getValue().farmId());
        assertEquals(" a ", captor.getValue().blockCode());
        assertEquals(14.2, captor.getValue().measuredValue());
    }

    @Test
    void recordObservation_shouldReturn400WithRequiredFields_whenBodyIsEmpty() throws Exception {
        // Act
        ResultActions result = postObservation("{}");
        // Assert: the farm, the value and the observer are optional, every other field is required
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(5)))
                .andExpect(jsonPath("$.fieldErrors.blockCode").value("blockCode is required"))
                .andExpect(jsonPath("$.fieldErrors.varietyName").value("varietyName is required"))
                .andExpect(jsonPath("$.fieldErrors.observedOn").value("observedOn is required"))
                .andExpect(jsonPath("$.fieldErrors.maturityStatus").value("maturityStatus is required"))
                .andExpect(jsonPath("$.fieldErrors.method").value("method is required"));
        verify(maturityObservationService, never()).recordObservation(any());
    }

    @Test
    void recordObservation_shouldReturn400WithEveryFailingField_whenValuesAreInvalid() throws Exception {
        // Arrange
        String future = LocalDate.now().plusYears(1).toString();
        String longObserver = "A".repeat(256);
        // Act
        ResultActions result = postObservation("""
                {"farmId":0,"blockCode":"Block A","varietyName":"   ","observedOn":"%s",
                 "maturityStatus":"RIPE","method":"VISUAL","observer":"%s"}
                """.formatted(future, longObserver));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(5)))
                .andExpect(jsonPath("$.fieldErrors.farmId").value("farmId must be at least 1"))
                .andExpect(jsonPath("$.fieldErrors.blockCode")
                        .value("blockCode must be a short code such as A or B2, without prefix or space"))
                .andExpect(jsonPath("$.fieldErrors.varietyName").value("varietyName is required"))
                .andExpect(jsonPath("$.fieldErrors.observedOn").value("observedOn must be today or in the past"))
                .andExpect(jsonPath("$.fieldErrors.observer").value("observer must be at most 255 characters"));
        verify(maturityObservationService, never()).recordObservation(any());
    }

    @Test
    void recordObservation_shouldReturn400_whenTheStatusIsHarvested() throws Exception {
        // Act: HARVESTED is not one of the three statuses of the requirements
        ResultActions result = postObservation("""
                {"blockCode":"A","varietyName":"Keitt","observedOn":"2026-10-12",
                 "maturityStatus":"HARVESTED","method":"VISUAL"}
                """);
        // Assert: the listed values, not the raw text of the JSON reader
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.maturityStatus")
                        .value("maturityStatus must be IMMATURE, RIPE or READY_FOR_HARVEST"));
        verifyNoInteractions(maturityObservationService);
    }

    @Test
    void recordObservation_shouldReturn400_whenTheMethodIsUnknown() throws Exception {
        // Act
        ResultActions result = postWith("\"method\":\"FIRMNESS\"");
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.method")
                        .value("method must be FLESH_COLOR, BRIX, DRY_MATTER or VISUAL"));
        verifyNoInteractions(maturityObservationService);
    }

    @Test
    void recordObservation_shouldAcceptTheStatusAndTheMethodInAnyCase() throws Exception {
        // Arrange
        when(maturityObservationService.recordObservation(any(MaturityObservationRequest.class)))
                .thenReturn(brixObservation());
        // Act
        ResultActions result = postObservation("""
                {"blockCode":"A","varietyName":"Keitt","observedOn":"2026-10-12",
                 "maturityStatus":" ready_for_harvest ","method":" brix ","measuredValue":14.2}
                """);
        // Assert: the service receives them as sent and normalizes them
        result.andExpect(status().isCreated());
        ArgumentCaptor<MaturityObservationRequest> captor = ArgumentCaptor.forClass(MaturityObservationRequest.class);
        verify(maturityObservationService).recordObservation(captor.capture());
        assertEquals(" ready_for_harvest ", captor.getValue().maturityStatus());
        assertEquals(" brix ", captor.getValue().method());
    }

    @Test
    void recordObservation_shouldReturn400_whenAValueComesWithAMethodThatMeasuresNone() throws Exception {
        // Act & Assert: the flesh colour and the visual observation read no value
        assertValueRefused(postWith("\"method\":\"VISUAL\",\"measuredValue\":12"), VALUE_LEFT_OUT);
        assertValueRefused(postWith("\"method\":\" flesh_color \",\"measuredValue\":12"), VALUE_LEFT_OUT);
        verifyNoInteractions(maturityObservationService);
    }

    @Test
    void recordObservation_shouldReturn400WithOneMessage_whenTheValueIsOutOfRange() throws Exception {
        // Act & Assert: 0, negative, above 100, or with two decimals
        for (String value : List.of("0", "-1", "100.1", "12.34", "0.05")) {
            assertValueRefused(postWith("\"method\":\"BRIX\",\"measuredValue\":" + value), VALUE_RANGE);
        }
        assertValueRefused(postWith("\"method\":\"DRY_MATTER\",\"measuredValue\":150"), VALUE_RANGE);
        verifyNoInteractions(maturityObservationService);
    }

    @Test
    void recordObservation_shouldReportTheMethodRuleOnly_whenBothRulesFail() throws Exception {
        // Act & Assert: one message, whatever the order of the checks
        assertValueRefused(postWith("\"method\":\"VISUAL\",\"measuredValue\":150"), VALUE_LEFT_OUT);
    }

    @Test
    void recordObservation_shouldCheckTheRange_whenTheMethodIsMissing() throws Exception {
        // Act
        ResultActions result = postWith("\"measuredValue\":150");
        // Assert: the method reports its own error, the value its range
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(2)))
                .andExpect(jsonPath("$.fieldErrors.method").value("method is required"))
                .andExpect(jsonPath("$.fieldErrors.measuredValue").value(VALUE_RANGE));
    }

    @Test
    void recordObservation_shouldAcceptTheBoundsOfTheValue_andNoValueWithAnyMethod() throws Exception {
        // Arrange
        when(maturityObservationService.recordObservation(any(MaturityObservationRequest.class)))
                .thenReturn(brixObservation());
        // Act & Assert
        for (String methodAndValue : List.of(
                "\"method\":\"BRIX\",\"measuredValue\":0.1",
                "\"method\":\"DRY_MATTER\",\"measuredValue\":100",
                "\"method\":\"DRY_MATTER\",\"measuredValue\":15.0",
                "\"method\":\"BRIX\"",
                "\"method\":\"VISUAL\",\"measuredValue\":null",
                "\"method\":\"FLESH_COLOR\"")) {
            postWith(methodAndValue).andExpect(status().isCreated());
        }
    }

    @Test
    void recordObservation_shouldReturn422WithApiError_whenVarietyHasNoPlanting() throws Exception {
        // Arrange
        when(maturityObservationService.recordObservation(any(MaturityObservationRequest.class)))
                .thenThrow(new BusinessRuleException("No planting of Kent is recorded on block C"));
        // Act
        ResultActions result = postWith("\"method\":\"VISUAL\"");
        // Assert
        assertApiError(result, 422, "Unprocessable Entity", "No planting of Kent is recorded on block C");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void recordObservation_shouldReturn422WithApiError_whenObservationIsBeforeThePlanting() throws Exception {
        // Arrange
        String message = "The observation date 2014-01-01 is before the planting date 2015-03-01 of Keitt on block A";
        when(maturityObservationService.recordObservation(any(MaturityObservationRequest.class)))
                .thenThrow(new BusinessRuleException(message));
        // Act
        ResultActions result = postObservation("""
                {"blockCode":"A","varietyName":"Keitt","observedOn":"2014-01-01",
                 "maturityStatus":"IMMATURE","method":"VISUAL"}
                """);
        // Assert
        assertApiError(result, 422, "Unprocessable Entity", message);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void getAllObservations_shouldPassEveryFilter() throws Exception {
        // Arrange
        when(maturityObservationService.getAllObservations(1, "A", LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31))).thenReturn(List.of(brixObservation()));
        // Act
        ResultActions result = mockMvc.perform(get(URL)
                .param("farmId", "1")
                .param("blockCode", "A")
                .param("from", "2026-10-01")
                .param("to", "2026-10-31"));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(300))
                .andExpect(jsonPath("$[0].observedOn").value("2026-10-12"))
                .andExpect(jsonPath("$[0].measuredUnit").value("°Bx"));
    }

    @Test
    void getAllObservations_shouldPassNulls_whenNoFilterIsGiven() throws Exception {
        // Arrange
        when(maturityObservationService.getAllObservations(null, null, null, null)).thenReturn(List.of());
        // Act & Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllObservations_shouldReturn400WithApiError_whenADateIsNotIso() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("from", "12/10/2026"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'from'");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
        verifyNoInteractions(maturityObservationService);
    }
}
