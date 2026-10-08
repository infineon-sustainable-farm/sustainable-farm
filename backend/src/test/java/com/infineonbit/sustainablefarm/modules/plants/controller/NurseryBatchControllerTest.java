package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryBatchRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryLossRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.StageChangeRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.TransplantRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryBatchResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryEventResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryOrigin;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;
import com.infineonbit.sustainablefarm.modules.plants.exception.NurseryBatchNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.NurseryBatchService;
import com.infineonbit.sustainablefarm.modules.plants.service.NurseryEventService;
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
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The error bodies are asserted in full, as for the planting and treatment routes. */
@WebMvcTest(NurseryBatchController.class)
@Import(CoreExceptionHandler.class)
public class NurseryBatchControllerTest {

    private static final String URL = "/api/plants/nursery-batches";
    private static final String STAGE_CHANGES_URL = URL + "/1/stage-changes";
    private static final String LOSSES_URL = URL + "/1/losses";
    private static final String TRANSPLANTS_URL = URL + "/1/transplants";
    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final LocalDate MARCH_2 = LocalDate.of(2026, 3, 2);
    private static final LocalDate SEPTEMBER_1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEPTEMBER_15 = LocalDate.of(2026, 9, 15);
    private static final LocalDate SEPTEMBER_20 = LocalDate.of(2026, 9, 20);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NurseryBatchService nurseryBatchService;

    @MockitoBean
    private NurseryEventService nurseryEventService;

    private ResultActions postBatch(String body) throws Exception {
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

    /** A 400 of the validation, on the given path, with the given field errors only. */
    private static void assertValidationErrors(ResultActions result, String path, String... fieldsAndMessages)
            throws Exception {
        assertApiError(result, 400, "Bad Request", "Validation failed", path);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(fieldsAndMessages.length / 2)));
        for (int index = 0; index < fieldsAndMessages.length; index += 2) {
            result.andExpect(jsonPath("$.fieldErrors['" + fieldsAndMessages[index] + "']")
                    .value(fieldsAndMessages[index + 1]));
        }
    }

    /** A 400 of the validation of a new batch, with the given field errors only. */
    private void assertFieldErrors(ResultActions result, String... fieldsAndMessages) throws Exception {
        assertValidationErrors(result, URL, fieldsAndMessages);
        verify(nurseryBatchService, never()).createBatch(any());
    }

    private ResultActions postTo(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** A batch started on March 2, with the given code, origin, supplier fields and initial count. */
    private static String batch(String batchCode, String origin, String supplierFields, String initialCount) {
        return """
                {"batchCode":"%s","varietyName":"Keitt","origin":"%s",%s"startedOn":"2026-03-02",
                 "initialCount":%s,"initialStage":"GERMINATION","plannedTransplantOn":"2026-09-20",
                 "plannedBlockCode":"E"}
                """.formatted(batchCode, origin, supplierFields, initialCount);
    }

    private static NurseryBatchResponse p1() {
        return new NurseryBatchResponse(1L, null, "P1", "Keitt", NurseryOrigin.IN_HOUSE, null, null, MARCH_2, 150,
                SEPTEMBER_20, "E", NurseryStage.GERMINATION, MARCH_2, 0, 0, 150, 150, 100.0, "user_entry", NOW);
    }

    @Test
    void createBatch_shouldReturn201WithTheComputedValues() throws Exception {
        // Arrange
        when(nurseryBatchService.createBatch(any(NurseryBatchRequest.class))).thenReturn(p1());
        // Act
        ResultActions result = postBatch(batch("P1", "IN_HOUSE", "", "150"));
        // Assert: no Location header, the batch in the body
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.farmId").value(nullValue()))
                .andExpect(jsonPath("$.batchCode").value("P1"))
                .andExpect(jsonPath("$.varietyName").value("Keitt"))
                .andExpect(jsonPath("$.origin").value("IN_HOUSE"))
                .andExpect(jsonPath("$.supplier").value(nullValue()))
                .andExpect(jsonPath("$.supplierLotNumber").value(nullValue()))
                .andExpect(jsonPath("$.startedOn").value("2026-03-02"))
                .andExpect(jsonPath("$.initialCount").value(150))
                .andExpect(jsonPath("$.plannedTransplantOn").value("2026-09-20"))
                .andExpect(jsonPath("$.plannedBlockCode").value("E"))
                .andExpect(jsonPath("$.currentStage").value("GERMINATION"))
                .andExpect(jsonPath("$.currentStageSince").value("2026-03-02"))
                .andExpect(jsonPath("$.lossCount").value(0))
                .andExpect(jsonPath("$.transplantedCount").value(0))
                .andExpect(jsonPath("$.currentCount").value(150))
                .andExpect(jsonPath("$.survivedCount").value(150))
                .andExpect(jsonPath("$.survivalRatePct").value(100.0))
                .andExpect(jsonPath("$.source").value("user_entry"))
                .andExpect(jsonPath("$.lastUpdated").value("2026-09-28T10:00:00Z"));
    }

    @Test
    void createBatch_shouldReturn400ListingEveryFailingField() throws Exception {
        // Act: plannedTransplantOn is missing
        ResultActions result = postBatch("""
                {"farmId":0,"batchCode":"P 1","varietyName":" ","origin":"gift","startedOn":"2999-01-01",
                 "initialCount":0,"initialStage":"seed","plannedBlockCode":"Block E"}
                """);
        // Assert: the origin is malformed, so the supplier rule is left to it
        assertFieldErrors(result,
                "farmId", "farmId must be at least 1",
                "batchCode", "batchCode must be up to 20 letters, digits or hyphens, such as P1 or KEITT-2026-01",
                "varietyName", "varietyName is required",
                "origin", "origin must be IN_HOUSE or PURCHASED",
                "startedOn", "startedOn must be today or in the past",
                "initialCount", "initialCount must be a whole number, 1 or more",
                "initialStage", "initialStage must be GERMINATION, ROOTSTOCK_GROWTH, GRAFTED, HARDENING or "
                        + "READY_TO_TRANSPLANT",
                "plannedTransplantOn", "plannedTransplantOn is required",
                "plannedBlockCode", "plannedBlockCode must be a short code such as A or B2, without prefix or space");
    }

    @Test
    void createBatch_shouldReturn400_whenTheInitialCountIsNotAWholeNumber() throws Exception {
        // 150.5 must be refused, not cut to 150
        assertFieldErrors(postBatch(batch("P3", "IN_HOUSE", "", "150.5")),
                "initialCount", "initialCount must be a whole number, 1 or more");
    }

    @Test
    void createBatch_shouldReturn400_whenAPurchasedBatchHasNoSupplier() throws Exception {
        assertFieldErrors(postBatch(batch("P2", "PURCHASED", "", "200")),
                "supplier", "supplier is required when origin is PURCHASED");
    }

    @Test
    void createBatch_shouldReturn400_whenAnInHouseBatchHasASupplierOrALotNumber() throws Exception {
        assertFieldErrors(postBatch(batch("P1", "IN_HOUSE",
                        "\"supplier\":\"Test nursery\",\"supplierLotNumber\":\"L-2026-07\",", "150")),
                "supplier", "supplier must be left out unless origin is PURCHASED",
                "supplierLotNumber", "supplierLotNumber must be left out unless origin is PURCHASED");
    }

    @Test
    void createBatch_shouldReturn400_whenTheSupplierOrTheLotNumberIsTooLong() throws Exception {
        String supplierFields = "\"supplier\":\"%s\",\"supplierLotNumber\":\"%s\","
                .formatted("x".repeat(256), "x".repeat(51));
        assertFieldErrors(postBatch(batch("P2", " purchased ", supplierFields, "200")),
                "supplier", "supplier must be at most 255 characters",
                "supplierLotNumber", "supplierLotNumber must be at most 50 characters");
    }

    @Test
    void createBatch_shouldReturn400_whenTheBatchCodeIsLongerThan20Characters() throws Exception {
        assertFieldErrors(postBatch(batch("KEITT-2026-SEPTEMBER1", "IN_HOUSE", "", "150")),
                "batchCode", "batchCode must be up to 20 letters, digits or hyphens, such as P1 or KEITT-2026-01");
    }

    @Test
    void createBatch_shouldAcceptCodesInAnyCaseAndABlankPlannedBlock() throws Exception {
        // Arrange
        when(nurseryBatchService.createBatch(any(NurseryBatchRequest.class))).thenReturn(p1());
        // Act
        ResultActions result = postBatch("""
                {"batchCode":" p1 ","varietyName":"Keitt","origin":" in_house ","startedOn":"2026-03-02",
                 "initialCount":150,"initialStage":" germination ","plannedTransplantOn":"2026-09-20",
                 "plannedBlockCode":" "}
                """);
        // Assert: the values reach the service as sent; it normalizes them
        result.andExpect(status().isCreated());
        ArgumentCaptor<NurseryBatchRequest> captor = ArgumentCaptor.forClass(NurseryBatchRequest.class);
        verify(nurseryBatchService).createBatch(captor.capture());
        assertEquals(" p1 ", captor.getValue().batchCode());
        assertEquals(" in_house ", captor.getValue().origin());
        assertEquals(" germination ", captor.getValue().initialStage());
        assertEquals(150.0, captor.getValue().initialCount());
        assertEquals(" ", captor.getValue().plannedBlockCode());
    }

    @Test
    void createBatch_shouldReturn409_whenTheCodeExists() throws Exception {
        // Arrange
        when(nurseryBatchService.createBatch(any(NurseryBatchRequest.class)))
                .thenThrow(new ConflictException("A nursery batch with code P1 already exists"));
        // Act
        ResultActions result = postBatch(batch("p1", "IN_HOUSE", "", "150"));
        // Assert
        assertApiError(result, 409, "Conflict", "A nursery batch with code P1 already exists", URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void createBatch_shouldReturn422_whenThePlannedDateIsBeforeTheStart() throws Exception {
        // Arrange
        when(nurseryBatchService.createBatch(any(NurseryBatchRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "The planned transplant date 2026-03-01 is before the start date 2026-03-02 of batch P3"));
        // Act
        ResultActions result = postBatch(batch("P3", "IN_HOUSE", "", "150"));
        // Assert
        assertApiError(result, 422, "Unprocessable Entity",
                "The planned transplant date 2026-03-01 is before the start date 2026-03-02 of batch P3", URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void getAllBatches_shouldPassTheFarmAndStageFilters() throws Exception {
        // Arrange
        when(nurseryBatchService.getAllBatches(1, NurseryStage.GERMINATION)).thenReturn(List.of(p1()));
        // Act & Assert
        mockMvc.perform(get(URL).param("farmId", "1").param("stage", "GERMINATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].batchCode").value("P1"));
        verify(nurseryBatchService).getAllBatches(1, NurseryStage.GERMINATION);
    }

    @Test
    void getAllBatches_shouldReturn400_whenTheStageIsUnknown() throws Exception {
        // An unknown value and a lower-case one are both refused, as for the other enum filters
        for (String stage : List.of("SEED", "hardening")) {
            ResultActions result = mockMvc.perform(get(URL).param("stage", stage));
            assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'stage'", URL);
            result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
        }
        verify(nurseryBatchService, never()).getAllBatches(any(), any());
    }

    @Test
    void getBatchById_shouldReturn404_whenTheBatchIsUnknown() throws Exception {
        // Arrange
        when(nurseryBatchService.getBatchById(999L)).thenThrow(new NurseryBatchNotFoundException(999L));
        // Act
        ResultActions result = mockMvc.perform(get(URL + "/999"));
        // Assert
        assertApiError(result, 404, "Not Found", "Nursery batch with ID 999 not found", URL + "/999");
    }

    @Test
    void recordStageChange_shouldReturn201WithTheEvent() throws Exception {
        // Arrange
        when(nurseryEventService.recordStageChange(eq(1L), any(StageChangeRequest.class)))
                .thenReturn(new NurseryEventResponse(20L, 1L, "P1", null, NurseryEventType.STAGE_CHANGE, SEPTEMBER_1,
                        NurseryStage.GRAFTED, null, null, null, null, "user_entry", NOW));
        // Act
        ResultActions result = postTo(STAGE_CHANGES_URL, """
                {"stage":" grafted ","changedOn":"2026-09-01"}
                """);
        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.batchId").value(1))
                .andExpect(jsonPath("$.batchCode").value("P1"))
                .andExpect(jsonPath("$.farmId").value(nullValue()))
                .andExpect(jsonPath("$.eventType").value("STAGE_CHANGE"))
                .andExpect(jsonPath("$.eventDate").value("2026-09-01"))
                .andExpect(jsonPath("$.stage").value("GRAFTED"))
                .andExpect(jsonPath("$.quantity").value(nullValue()))
                .andExpect(jsonPath("$.reason").value(nullValue()))
                .andExpect(jsonPath("$.blockCode").value(nullValue()))
                .andExpect(jsonPath("$.populationEventId").value(nullValue()))
                .andExpect(jsonPath("$.source").value("user_entry"))
                .andExpect(jsonPath("$.lastUpdated").value("2026-09-28T10:00:00Z"));
        ArgumentCaptor<StageChangeRequest> captor = ArgumentCaptor.forClass(StageChangeRequest.class);
        verify(nurseryEventService).recordStageChange(eq(1L), captor.capture());
        assertEquals(" grafted ", captor.getValue().stage());
    }

    @Test
    void recordStageChange_shouldReturn400ListingEveryFailingField() throws Exception {
        // Act
        ResultActions result = postTo(STAGE_CHANGES_URL, """
                {"stage":"seed","changedOn":"2999-01-01"}
                """);
        // Assert
        assertValidationErrors(result, STAGE_CHANGES_URL,
                "stage", "stage must be GERMINATION, ROOTSTOCK_GROWTH, GRAFTED, HARDENING or READY_TO_TRANSPLANT",
                "changedOn", "changedOn must be today or in the past");
        verify(nurseryEventService, never()).recordStageChange(any(), any());
    }

    @Test
    void recordStageChange_shouldReturn422_whenTheBatchIsAlreadyAtThatStage() throws Exception {
        // Arrange
        when(nurseryEventService.recordStageChange(eq(1L), any(StageChangeRequest.class)))
                .thenThrow(new BusinessRuleException("Batch P1 is already at stage GRAFTED"));
        // Act
        ResultActions result = postTo(STAGE_CHANGES_URL, """
                {"stage":"GRAFTED","changedOn":"2026-09-10"}
                """);
        // Assert
        assertApiError(result, 422, "Unprocessable Entity", "Batch P1 is already at stage GRAFTED",
                STAGE_CHANGES_URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void recordLoss_shouldReturn201WithTheEvent() throws Exception {
        // Arrange
        when(nurseryEventService.recordLoss(eq(1L), any(NurseryLossRequest.class)))
                .thenReturn(new NurseryEventResponse(21L, 1L, "P1", null, NurseryEventType.LOSS, SEPTEMBER_15, null,
                        12, "Graft failure", null, null, "user_entry", NOW));
        // Act
        ResultActions result = postTo(LOSSES_URL, """
                {"lostOn":"2026-09-15","quantity":12,"reason":"Graft failure"}
                """);
        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.eventType").value("LOSS"))
                .andExpect(jsonPath("$.eventDate").value("2026-09-15"))
                .andExpect(jsonPath("$.stage").value(nullValue()))
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.reason").value("Graft failure"));
        ArgumentCaptor<NurseryLossRequest> captor = ArgumentCaptor.forClass(NurseryLossRequest.class);
        verify(nurseryEventService).recordLoss(eq(1L), captor.capture());
        assertEquals(12.0, captor.getValue().quantity());
    }

    @Test
    void recordLoss_shouldReturn400ListingEveryFailingField() throws Exception {
        // Act: lostOn is missing, and 12.5 must be refused, not cut to 12
        ResultActions result = postTo(LOSSES_URL, """
                {"quantity":12.5,"reason":" "}
                """);
        // Assert
        assertValidationErrors(result, LOSSES_URL,
                "lostOn", "lostOn is required",
                "quantity", "quantity must be a whole number, 1 or more",
                "reason", "reason is required");
        verify(nurseryEventService, never()).recordLoss(any(), any());
    }

    @Test
    void recordLoss_shouldReturn422_whenThereAreNotEnoughPlants() throws Exception {
        // Arrange
        when(nurseryEventService.recordLoss(eq(1L), any(NurseryLossRequest.class)))
                .thenThrow(new BusinessRuleException("Not enough plants in batch P1: 0 left, 1 requested"));
        // Act
        ResultActions result = postTo(LOSSES_URL, """
                {"lostOn":"2026-09-22","quantity":1,"reason":"Drought"}
                """);
        // Assert
        assertApiError(result, 422, "Unprocessable Entity", "Not enough plants in batch P1: 0 left, 1 requested",
                LOSSES_URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void recordLoss_shouldReturn404_whenTheBatchIsUnknown() throws Exception {
        // Arrange
        when(nurseryEventService.recordLoss(eq(999L), any(NurseryLossRequest.class)))
                .thenThrow(new NurseryBatchNotFoundException(999L));
        // Act
        ResultActions result = postTo(URL + "/999/losses", """
                {"lostOn":"2026-09-22","quantity":1,"reason":"Drought"}
                """);
        // Assert
        assertApiError(result, 404, "Not Found", "Nursery batch with ID 999 not found", URL + "/999/losses");
    }

    @Test
    void recordTransplant_shouldReturn201WithTheEventAndItsPlanting() throws Exception {
        // Arrange
        when(nurseryEventService.recordTransplant(eq(1L), any(TransplantRequest.class)))
                .thenReturn(new NurseryEventResponse(22L, 1L, "P1", null, NurseryEventType.TRANSPLANT, SEPTEMBER_20,
                        null, 100, null, "E", 7L, "user_entry", NOW));
        // Act
        ResultActions result = postTo(TRANSPLANTS_URL, """
                {"transplantedOn":"2026-09-20","quantity":100,"blockCode":" e "}
                """);
        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(22))
                .andExpect(jsonPath("$.eventType").value("TRANSPLANT"))
                .andExpect(jsonPath("$.eventDate").value("2026-09-20"))
                .andExpect(jsonPath("$.quantity").value(100))
                .andExpect(jsonPath("$.blockCode").value("E"))
                .andExpect(jsonPath("$.populationEventId").value(7));
        ArgumentCaptor<TransplantRequest> captor = ArgumentCaptor.forClass(TransplantRequest.class);
        verify(nurseryEventService).recordTransplant(eq(1L), captor.capture());
        assertEquals(" e ", captor.getValue().blockCode());
        assertEquals(100.0, captor.getValue().quantity());
    }

    @Test
    void recordTransplant_shouldReturn400ListingEveryFailingField() throws Exception {
        // Act: 38.5 must be refused, not cut to 38
        ResultActions result = postTo(TRANSPLANTS_URL, """
                {"transplantedOn":"2999-01-01","quantity":38.5,"blockCode":"Block E"}
                """);
        // Assert
        assertValidationErrors(result, TRANSPLANTS_URL,
                "transplantedOn", "transplantedOn must be today or in the past",
                "quantity", "quantity must be a whole number, 1 or more",
                "blockCode", "blockCode must be a short code such as A or B2, without prefix or space");
        verify(nurseryEventService, never()).recordTransplant(any(), any());
    }

    @Test
    void recordTransplant_shouldReturn409_whenThePlantingIsRefused() throws Exception {
        // Arrange
        when(nurseryEventService.recordTransplant(eq(1L), any(TransplantRequest.class)))
                .thenThrow(new ConflictException("A planting of Keitt is already recorded on block E"));
        // Act
        ResultActions result = postTo(TRANSPLANTS_URL, """
                {"transplantedOn":"2026-09-21","quantity":38,"blockCode":"E"}
                """);
        // Assert
        assertApiError(result, 409, "Conflict", "A planting of Keitt is already recorded on block E",
                TRANSPLANTS_URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void recordTransplant_shouldReturn422_whenThereAreNotEnoughPlants() throws Exception {
        // Arrange
        when(nurseryEventService.recordTransplant(eq(1L), any(TransplantRequest.class)))
                .thenThrow(new BusinessRuleException("Not enough plants in batch P1: 138 left, 200 requested"));
        // Act
        ResultActions result = postTo(TRANSPLANTS_URL, """
                {"transplantedOn":"2026-09-20","quantity":200,"blockCode":"E"}
                """);
        // Assert
        assertApiError(result, 422, "Unprocessable Entity", "Not enough plants in batch P1: 138 left, 200 requested",
                TRANSPLANTS_URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }
}
