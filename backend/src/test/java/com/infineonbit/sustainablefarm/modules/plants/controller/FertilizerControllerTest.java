package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ApplicationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.LossRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PurchaseRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerMovementResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerMovementService;
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

    @MockitoBean
    private FertilizerMovementService fertilizerMovementService;

    private ResultActions postFertilizer(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions postMovement(long fertilizerId, String kind, String body) throws Exception {
        return mockMvc.perform(post(URL + "/" + fertilizerId + "/" + kind)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static FertilizerMovementResponse npkPurchaseInEuros() {
        return new FertilizerMovementResponse(11L, 1L, "NPK 15-15-15", FertilizerMovementType.PURCHASE,
                LocalDate.of(2026, 6, 5), 100.0, FertilizerUnit.KG, null, null, null, null, "Supplier B", 120.0, "EUR",
                78715L, 120.0, null, "user_entry", NOW);
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

    @Test
    void recordPurchase_shouldReturn201WithTheCostInBothCurrencies() throws Exception {
        // Arrange
        when(fertilizerMovementService.recordPurchase(eq(1L), any(PurchaseRequest.class)))
                .thenReturn(npkPurchaseInEuros());
        // Act
        ResultActions result = postMovement(1L, "purchases", """
                {"purchaseDate":"2026-06-05","quantity":100,"supplier":"Supplier B","totalCost":120,"currency":"eur"}
                """);
        // Assert: no Location header, the purchase in the body
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.fertilizerId").value(1))
                .andExpect(jsonPath("$.fertilizerName").value("NPK 15-15-15"))
                .andExpect(jsonPath("$.movementType").value("PURCHASE"))
                .andExpect(jsonPath("$.movementDate").value("2026-06-05"))
                .andExpect(jsonPath("$.quantity").value(100.0))
                .andExpect(jsonPath("$.unit").value("KG"))
                .andExpect(jsonPath("$.blockCode").value(nullValue()))
                .andExpect(jsonPath("$.supplier").value("Supplier B"))
                .andExpect(jsonPath("$.totalCost").value(120.0))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.totalCostXof").value(78715))
                .andExpect(jsonPath("$.totalCostEur").value(120.0))
                .andExpect(jsonPath("$.reason").value(nullValue()))
                .andExpect(jsonPath("$.source").value("user_entry"))
                .andExpect(jsonPath("$.lastUpdated").value("2026-09-24T10:00:00Z"));
        // Assert: the currency reaches the service as sent; it normalizes it
        ArgumentCaptor<PurchaseRequest> captor = ArgumentCaptor.forClass(PurchaseRequest.class);
        verify(fertilizerMovementService).recordPurchase(eq(1L), captor.capture());
        assertEquals("eur", captor.getValue().currency());
    }

    @Test
    void recordPurchase_shouldReturn201WithAnEmptyFcfaAmount_whenNoRateIsRecorded() throws Exception {
        // Arrange: the service leaves out the amount that needs the rate
        when(fertilizerMovementService.recordPurchase(eq(1L), any(PurchaseRequest.class)))
                .thenReturn(new FertilizerMovementResponse(11L, 1L, "NPK 15-15-15", FertilizerMovementType.PURCHASE,
                        LocalDate.of(2026, 6, 5), 100.0, FertilizerUnit.KG, null, null, null, null, "Supplier B",
                        120.0, "EUR", null, 120.0, null, "user_entry", NOW));
        // Act
        ResultActions result = postMovement(1L, "purchases", """
                {"purchaseDate":"2026-06-05","quantity":100,"supplier":"Supplier B","totalCost":120,"currency":"EUR"}
                """);
        // Assert: the purchase is recorded, with its FCFA amount empty
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCost").value(120.0))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.totalCostXof").value(nullValue()))
                .andExpect(jsonPath("$.totalCostEur").value(120.0));
    }

    @Test
    void recordPurchase_shouldReturn400WithEveryFailingField_whenValuesAreInvalid() throws Exception {
        // Arrange
        String future = LocalDate.now().plusYears(1).toString();
        // Act
        ResultActions result = postMovement(1L, "purchases", """
                {"purchaseDate":"%s","quantity":0,"supplier":"","totalCost":-5}
                """.formatted(future));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed", URL + "/1/purchases");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(4)))
                .andExpect(jsonPath("$.fieldErrors.purchaseDate").value("purchaseDate must be today or in the past"))
                .andExpect(jsonPath("$.fieldErrors.quantity")
                        .value("quantity must be greater than 0, with at most 3 decimals"))
                .andExpect(jsonPath("$.fieldErrors.supplier").value("supplier is required"))
                .andExpect(jsonPath("$.fieldErrors.totalCost")
                        .value("totalCost must be greater than 0, with at most 2 decimals"));
        verify(fertilizerMovementService, never()).recordPurchase(any(), any());
    }

    @Test
    void recordPurchase_shouldReturn400WithRequiredFields_whenBodyIsEmpty() throws Exception {
        // Act
        ResultActions result = postMovement(1L, "purchases", "{}");
        // Assert: the cost and its currency are optional
        assertApiError(result, 400, "Bad Request", "Validation failed", URL + "/1/purchases");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(3)))
                .andExpect(jsonPath("$.fieldErrors.purchaseDate").value("purchaseDate is required"))
                .andExpect(jsonPath("$.fieldErrors.quantity").value("quantity is required"))
                .andExpect(jsonPath("$.fieldErrors.supplier").value("supplier is required"));
    }

    @Test
    void recordPurchase_shouldReturn400_whenCurrencyIsUnknownOrAmountsHaveTooManyDecimals() throws Exception {
        // Act
        ResultActions result = postMovement(1L, "purchases", """
                {"purchaseDate":"2026-06-05","quantity":0.1234,"supplier":"Supplier B","totalCost":10.005,
                 "currency":"USD"}
                """);
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed", URL + "/1/purchases");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(3)))
                .andExpect(jsonPath("$.fieldErrors.quantity")
                        .value("quantity must be greater than 0, with at most 3 decimals"))
                .andExpect(jsonPath("$.fieldErrors.totalCost")
                        .value("totalCost must be greater than 0, with at most 2 decimals"))
                .andExpect(jsonPath("$.fieldErrors.currency").value("currency must be XOF or EUR"));
    }

    @Test
    void recordPurchase_shouldReturn404WithApiError_whenFertilizerIsUnknown() throws Exception {
        // Arrange
        when(fertilizerMovementService.recordPurchase(eq(999L), any(PurchaseRequest.class)))
                .thenThrow(new FertilizerNotFoundException(999L));
        // Act
        ResultActions result = postMovement(999L, "purchases", """
                {"purchaseDate":"2026-06-05","quantity":100,"supplier":"Supplier B"}
                """);
        // Assert
        assertApiError(result, 404, "Not Found", "Fertilizer with ID 999 not found", URL + "/999/purchases");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void recordApplication_shouldReturn201WithTheApplication() throws Exception {
        // Arrange
        when(fertilizerMovementService.recordApplication(eq(1L), any(ApplicationRequest.class))).thenReturn(
                new FertilizerMovementResponse(13L, 1L, "NPK 15-15-15", FertilizerMovementType.APPLICATION,
                        LocalDate.of(2026, 6, 15), 250.0, FertilizerUnit.KG, null, "B", "Team A",
                        "around the tree base", null, null, null, null, null, null, "user_entry", NOW));
        // Act
        ResultActions result = postMovement(1L, "applications", """
                {"applicationDate":"2026-06-15","quantity":250,"blockCode":" b ","applicator":"Team A",
                 "method":"around the tree base"}
                """);
        // Assert: no Location header, the application in the body
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(13))
                .andExpect(jsonPath("$.movementType").value("APPLICATION"))
                .andExpect(jsonPath("$.movementDate").value("2026-06-15"))
                .andExpect(jsonPath("$.quantity").value(250.0))
                .andExpect(jsonPath("$.unit").value("KG"))
                .andExpect(jsonPath("$.farmId").value(nullValue()))
                .andExpect(jsonPath("$.blockCode").value("B"))
                .andExpect(jsonPath("$.applicator").value("Team A"))
                .andExpect(jsonPath("$.method").value("around the tree base"))
                .andExpect(jsonPath("$.supplier").value(nullValue()))
                .andExpect(jsonPath("$.totalCostXof").value(nullValue()))
                .andExpect(jsonPath("$.totalCostEur").value(nullValue()));
        // Assert: the block reaches the service as sent; it normalizes it
        ArgumentCaptor<ApplicationRequest> captor = ArgumentCaptor.forClass(ApplicationRequest.class);
        verify(fertilizerMovementService).recordApplication(eq(1L), captor.capture());
        assertEquals(" b ", captor.getValue().blockCode());
    }

    @Test
    void recordApplication_shouldReturn400WithEveryFailingField_whenValuesAreInvalid() throws Exception {
        // Arrange
        String future = LocalDate.now().plusYears(1).toString();
        // Act
        ResultActions result = postMovement(1L, "applications", """
                {"applicationDate":"%s","quantity":0,"farmId":0,"blockCode":"Block B","applicator":"  ",
                 "method":"%s"}
                """.formatted(future, "m".repeat(256)));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed", URL + "/1/applications");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(6)))
                .andExpect(jsonPath("$.fieldErrors.applicationDate")
                        .value("applicationDate must be today or in the past"))
                .andExpect(jsonPath("$.fieldErrors.quantity")
                        .value("quantity must be greater than 0, with at most 3 decimals"))
                .andExpect(jsonPath("$.fieldErrors.farmId").value("farmId must be at least 1"))
                .andExpect(jsonPath("$.fieldErrors.blockCode")
                        .value("blockCode must be a short code such as A or B2, without prefix or space"))
                .andExpect(jsonPath("$.fieldErrors.applicator").value("applicator is required"))
                .andExpect(jsonPath("$.fieldErrors.method").value("method must be at most 255 characters"));
        verify(fertilizerMovementService, never()).recordApplication(any(), any());
    }

    @Test
    void recordApplication_shouldReturn400WithRequiredFields_whenBodyIsEmpty() throws Exception {
        // Act
        ResultActions result = postMovement(1L, "applications", "{}");
        // Assert: the farm and the method are optional
        assertApiError(result, 400, "Bad Request", "Validation failed", URL + "/1/applications");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(4)))
                .andExpect(jsonPath("$.fieldErrors.applicationDate").value("applicationDate is required"))
                .andExpect(jsonPath("$.fieldErrors.quantity").value("quantity is required"))
                .andExpect(jsonPath("$.fieldErrors.blockCode").value("blockCode is required"))
                .andExpect(jsonPath("$.fieldErrors.applicator").value("applicator is required"));
    }

    @Test
    void recordApplication_shouldReturn422WithApiError_whenStockIsTooLow() throws Exception {
        // Arrange
        String message = "Not enough stock of NPK 15-15-15: 50 kg left, 60 kg requested";
        when(fertilizerMovementService.recordApplication(eq(1L), any(ApplicationRequest.class)))
                .thenThrow(new BusinessRuleException(message));
        // Act
        ResultActions result = postMovement(1L, "applications", """
                {"applicationDate":"2026-06-16","quantity":60,"blockCode":"B","applicator":"Team A"}
                """);
        // Assert
        assertApiError(result, 422, "Unprocessable Entity", message, URL + "/1/applications");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void recordApplication_shouldReturn404WithApiError_whenFertilizerIsUnknown() throws Exception {
        // Arrange
        when(fertilizerMovementService.recordApplication(eq(999L), any(ApplicationRequest.class)))
                .thenThrow(new FertilizerNotFoundException(999L));
        // Act
        ResultActions result = postMovement(999L, "applications", """
                {"applicationDate":"2026-06-16","quantity":60,"blockCode":"B","applicator":"Team A"}
                """);
        // Assert
        assertApiError(result, 404, "Not Found", "Fertilizer with ID 999 not found", URL + "/999/applications");
    }

    @Test
    void recordLoss_shouldReturn201WithTheLoss() throws Exception {
        // Arrange
        when(fertilizerMovementService.recordLoss(eq(1L), any(LossRequest.class))).thenReturn(
                new FertilizerMovementResponse(16L, 1L, "NPK 15-15-15", FertilizerMovementType.LOSS,
                        LocalDate.of(2026, 7, 1), 10.0, FertilizerUnit.KG, null, null, null, null, null, null, null,
                        null, null, "expired", "user_entry", NOW));
        // Act
        ResultActions result = postMovement(1L, "losses", """
                {"lossDate":"2026-07-01","quantity":10,"reason":"expired"}
                """);
        // Assert
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(16))
                .andExpect(jsonPath("$.movementType").value("LOSS"))
                .andExpect(jsonPath("$.quantity").value(10.0))
                .andExpect(jsonPath("$.reason").value("expired"))
                .andExpect(jsonPath("$.blockCode").value(nullValue()));
    }

    @Test
    void recordLoss_shouldReturn400WithEveryFailingField_whenValuesAreInvalid() throws Exception {
        // Arrange
        String future = LocalDate.now().plusYears(1).toString();
        // Act
        ResultActions result = postMovement(1L, "losses", """
                {"lossDate":"%s","quantity":-1,"reason":""}
                """.formatted(future));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed", URL + "/1/losses");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(3)))
                .andExpect(jsonPath("$.fieldErrors.lossDate").value("lossDate must be today or in the past"))
                .andExpect(jsonPath("$.fieldErrors.quantity")
                        .value("quantity must be greater than 0, with at most 3 decimals"))
                .andExpect(jsonPath("$.fieldErrors.reason").value("reason is required"));
        verify(fertilizerMovementService, never()).recordLoss(any(), any());
    }

    @Test
    void recordLoss_shouldReturn422WithApiError_whenStockIsTooLow() throws Exception {
        // Arrange
        String message = "Not enough stock of NPK 15-15-15: 40 kg left, 41 kg requested";
        when(fertilizerMovementService.recordLoss(eq(1L), any(LossRequest.class)))
                .thenThrow(new BusinessRuleException(message));
        // Act
        ResultActions result = postMovement(1L, "losses", """
                {"lossDate":"2026-07-02","quantity":41,"reason":"expired"}
                """);
        // Assert
        assertApiError(result, 422, "Unprocessable Entity", message, URL + "/1/losses");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void recordLoss_shouldReturn404WithApiError_whenFertilizerIsUnknown() throws Exception {
        // Arrange
        when(fertilizerMovementService.recordLoss(eq(999L), any(LossRequest.class)))
                .thenThrow(new FertilizerNotFoundException(999L));
        // Act
        ResultActions result = postMovement(999L, "losses", """
                {"lossDate":"2026-07-02","quantity":1,"reason":"expired"}
                """);
        // Assert
        assertApiError(result, 404, "Not Found", "Fertilizer with ID 999 not found", URL + "/999/losses");
    }
}
