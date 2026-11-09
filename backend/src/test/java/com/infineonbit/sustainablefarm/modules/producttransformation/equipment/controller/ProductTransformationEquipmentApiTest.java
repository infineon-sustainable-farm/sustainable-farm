package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that the Product Transformation equipment API is actually exposed
 * (the controller carried no {@code @RestController} annotation before) and
 * that its route does not collide with Machinery's {@code /api/equipments}.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ProductTransformationEquipmentApiTest {

    private static final String EQUIPMENT_ID = "PT-EQ-API-TEST";

    @Autowired
    private MockMvc mockMvc;

    private String payload() {
        return """
                {
                  "equipmentId": "%s",
                  "equipmentName": "Tunnel dryer test",
                  "equipmentType": "DRYING",
                  "capacityKgPerHour": 120.00,
                  "energyConsumptionKwhPerKg": 0.85,
                  "location": "Demo hall",
                  "maintenanceStatus": "ACTIVE"
                }
                """.formatted(EQUIPMENT_ID);
    }

    @Test
    void equipmentCrudIsExposedUnderProductTransformationRoute() throws Exception {
        mockMvc.perform(post("/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentId").value(EQUIPMENT_ID))
                .andExpect(jsonPath("$.equipmentName").value("Tunnel dryer test"))
                .andExpect(jsonPath("$.maintenanceStatus").value("ACTIVE"));

        mockMvc.perform(get("/api/equipment/{id}", EQUIPMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentId").value(EQUIPMENT_ID));

        mockMvc.perform(get("/api/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].equipmentId", hasItem(EQUIPMENT_ID)));

        mockMvc.perform(get("/api/equipment/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].equipmentId", hasItem(EQUIPMENT_ID)));

        mockMvc.perform(delete("/api/equipment/{id}", EQUIPMENT_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].equipmentId", not(hasItem(EQUIPMENT_ID))));
    }

    @Test
    void machineryEquipmentRouteRemainsSeparate() throws Exception {
        mockMvc.perform(get("/api/equipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
