package com.infineonbit.sustainablefarm.modules.producttransformation.integration.cropstorage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The crop storage endpoints of Product Transformation, while no Crop Storage
 * module receives the entries: a storage entry is refused with a 500 error,
 * as when the entry was posted over HTTP to the application itself, but at
 * once; the capacity check answers 503.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CropStorageIntegrationApiTest {

    private static final String ENTRY = """
            {"batch_id": "B-CS-1", "dried_quantity_kg": 10, "product_stage": "DRIED",
             "storage_location_id": "ZONE-A1"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aStorageEntry_isRefusedAtOnce_withA500() throws Exception {
        long start = System.nanoTime();
        mockMvc.perform(post("/api/crop-storage/entries").contentType(MediaType.APPLICATION_JSON).content(ENTRY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
        // The former HTTP call to the application itself ended on its 5 s read timeout.
        assertThat(System.nanoTime() - start).isLessThan(2_000_000_000L);
    }

    @Test
    void theCapacityCheck_answers503() throws Exception {
        mockMvc.perform(post("/api/crop-storage/entries/check-capacity").contentType(MediaType.APPLICATION_JSON)
                        .content(ENTRY))
                .andExpect(status().isServiceUnavailable());
    }
}
