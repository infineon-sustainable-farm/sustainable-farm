package com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.dto;

import com.infineonbit.sustainablefarm.modules.producttransformation.exception.GlobalExceptionHandler;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.dto.request.HarvestEventCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level contract: what a client actually receives.
 *
 * <p>The Plants team posted {@code "Keitt"} and got a 500. This test drives the
 * real message conversion and the real advice, so it fails if either the enum
 * binding or the error mapping regresses.
 */
class HarvestEventHttpBindingTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    private static String body(String variety) {
        return """
                {
                  "harvestId": "H-T1",
                  "batchId": "PT-BATCH-1",
                  "harvestDate": "2026-06-20",
                  "mangoVariety": "%s",
                  "farmId": "1",
                  "blockId": "A",
                  "harvestQuantityKg": 7500,
                  "qualityGrade": "A"
                }
                """.formatted(variety);
    }

    @Test
    @DisplayName("\"Keitt\" is accepted (201 in the real controller, 200 on the probe)")
    void acceptsKeitt() throws Exception {
        mockMvc.perform(post("/probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Keitt")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("an unknown variety returns 400 INVALID_PAYLOAD, never 500")
    void unknownVarietyIsAClientError() throws Exception {
        mockMvc.perform(post("/probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Alphonso")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PAYLOAD"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("malformed JSON also returns 400 INVALID_PAYLOAD")
    void malformedJsonIsAClientError() throws Exception {
        mockMvc.perform(post("/probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PAYLOAD"));
    }

    @RestController
    static class ProbeController {

        @PostMapping("/probe")
        ResponseEntity<String> probe(@RequestBody HarvestEventCreateRequest request) {
            return ResponseEntity.ok("ok");
        }
    }
}
