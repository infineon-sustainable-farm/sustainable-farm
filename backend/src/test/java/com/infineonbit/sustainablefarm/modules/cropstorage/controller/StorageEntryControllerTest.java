package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.StorageEntryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API-005 HTTP contract on the Crop Storage side only (service mocked):
 * validation, snake_case, UTC timestamps and the common error envelope.
 * This does not prove the Product Transformation integration.
 */
@WebMvcTest(controllers = StorageEntryController.class)
@Import(CoreExceptionHandler.class)
class StorageEntryControllerTest {

    private static final String URL = "/api/crop-storage/entries";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageEntryService service;

    /**
     * Valid payload where each override ("key":value, comma separated) replaces
     * the base value of that key, so no JSON key is ever duplicated.
     */
    private static String body(String overrides) {
        java.util.Map<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("batch_id", "\"PT-B-1\"");
        fields.put("packaging_id", "\"PKG-1\"");
        fields.put("dried_quantity_kg", "25.5");
        fields.put("packaging_date", "\"2026-10-30T10:00:00Z\"");
        fields.put("packaging_operator_id", "\"op-1\"");
        fields.put("packaging_location", "\"line-1\"");
        fields.put("product_grade", "\"A\"");
        fields.put("farm_id", "\"farm-1\"");
        fields.put("block_id", "\"block-1\"");
        fields.put("variety_id", "\"variety-1\"");
        if (!overrides.isEmpty()) {
            for (String pair : overrides.split(",(?=\")")) {
                int colon = pair.indexOf("\":");
                fields.put(pair.substring(1, colon), pair.substring(colon + 2));
            }
        }
        StringBuilder json = new StringBuilder("{");
        fields.forEach((key, value) -> json.append(json.length() > 1 ? "," : "")
                .append('"').append(key).append("\":").append(value));
        return json.append('}').toString();
    }

    private ResultActions postEntry(String json) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static StorageEntryResponse response() {
        return new StorageEntryResponse("11", "3",
                OffsetDateTime.of(2026, 11, 19, 9, 30, 0, 0, ZoneOffset.UTC),
                new BigDecimal("974.50"), "STORED");
    }

    @Test
    void validEntryReturns201InSnakeCaseWithUtcTimestamp() throws Exception {
        when(service.registerEntry(any())).thenReturn(response());

        postEntry(body("\"harvest_id\":\"42\",\"target_storage_location_id\":\"3\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storage_entry_id").value("11"))
                .andExpect(jsonPath("$.storage_location_id").value("3"))
                .andExpect(jsonPath("$.entry_timestamp").value(endsWith("Z")))
                .andExpect(jsonPath("$.available_capacity_kg").value(974.5))
                .andExpect(jsonPath("$.status").value("STORED"));

        ArgumentCaptor<StorageEntryRequest> captor = ArgumentCaptor.forClass(StorageEntryRequest.class);
        verify(service).registerEntry(captor.capture());
        assertEquals(0, new BigDecimal("25.5").compareTo(captor.getValue().driedQuantityKg()));
        assertEquals("42", captor.getValue().harvestId());
        assertNull(captor.getValue().wasteQuantityKg(), "an absent waste quantity stays null, not 0");
    }

    @Test
    void zeroWasteIsAcceptedAndKeptDistinctFromNull() throws Exception {
        when(service.registerEntry(any())).thenReturn(response());

        postEntry(body("\"waste_quantity_kg\":0")).andExpect(status().isCreated());

        ArgumentCaptor<StorageEntryRequest> captor = ArgumentCaptor.forClass(StorageEntryRequest.class);
        verify(service).registerEntry(captor.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getValue().wasteQuantityKg()));
    }

    @Test
    void missingRequiredFieldsAnswer400WithEveryFieldNamed() throws Exception {
        postEntry("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors", hasKey("batchId")))
                .andExpect(jsonPath("$.fieldErrors", hasKey("packagingId")))
                .andExpect(jsonPath("$.fieldErrors", hasKey("driedQuantityKg")))
                .andExpect(jsonPath("$.fieldErrors", hasKey("packagingDate")))
                .andExpect(jsonPath("$.fieldErrors", hasKey("packagingOperatorId")))
                .andExpect(jsonPath("$.fieldErrors", hasKey("packagingLocation")))
                .andExpect(jsonPath("$.fieldErrors", hasKey("productGrade")))
                .andExpect(jsonPath("$.fieldErrors", hasKey("farmId")))
                .andExpect(jsonPath("$.fieldErrors", hasKey("blockId")))
                .andExpect(jsonPath("$.fieldErrors", hasKey("varietyId")));
        verify(service, never()).registerEntry(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"dried_quantity_kg\":0",
            "\"dried_quantity_kg\":-1",
            "\"dried_quantity_kg\":null",
            "\"dried_quantity_kg\":1.234",
            "\"waste_quantity_kg\":-0.5",
            "\"harvest_id\":\"HARV-42\"",
            "\"harvest_id\":\"\"",
            "\"batch_id\":\" \""
    })
    void invalidValuesAnswer400AndNeverReachTheService(String override) throws Exception {
        // A bean-validation failure (fieldErrors), not a JSON parse error.
        postEntry(body(override))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isMap());
        verify(service, never()).registerEntry(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-30T10:00:00", "2026-10-30", "yesterday"})
    void packagingDateWithoutExplicitOffsetIsRefused(String date) throws Exception {
        postEntry(body("\"packaging_date\":\"" + date + "\"")).andExpect(status().isBadRequest());
        verify(service, never()).registerEntry(any());
    }

    @Test
    void unknownZoneAnswers404() throws Exception {
        when(service.registerEntry(any())).thenThrow(new ResourceNotFoundException("Storage location not found"));
        postEntry(body("\"target_storage_location_id\":\"ZONE-A1\""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void fullZoneOrKnownDuplicateAnswers409() throws Exception {
        when(service.registerEntry(any())).thenThrow(new ConflictException("Zone capacity exceeded"));
        postEntry(body("")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void anUntranslatedConstraintViolationStillAnswers409ThroughTheCoreHandler() throws Exception {
        // The service turns the unique batch code refusal into a ConflictException
        // (CropStorageBusinessRulesJpaTest); this only checks the core fallback.
        when(service.registerEntry(any())).thenThrow(new DataIntegrityViolationException("uk_batch_code"));
        postEntry(body(""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void readsOneEntryAndTheEntriesOfABatch() throws Exception {
        when(service.findEntry(11L)).thenReturn(response());
        when(service.findEntriesByBatchId("PT-B-1")).thenReturn(List.of(response()));
        when(service.findEntriesByBatchId("UNKNOWN")).thenReturn(List.of());
        when(service.findEntry(99L)).thenThrow(new ResourceNotFoundException("Storage entry not found"));

        mockMvc.perform(get(URL + "/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storage_entry_id").value("11"));
        mockMvc.perform(get(URL + "/batch/PT-B-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storage_location_id").value("3"));
        mockMvc.perform(get(URL + "/batch/UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(URL + "/99")).andExpect(status().isNotFound());
        mockMvc.perform(get(URL + "/not-a-number")).andExpect(status().isBadRequest());
    }
}
