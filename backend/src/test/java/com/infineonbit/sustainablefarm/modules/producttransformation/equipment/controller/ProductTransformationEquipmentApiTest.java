package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The product transformation equipment API on the single equipment register (ADR 0004): the
 * {@code /api/product-transformation/equipment} JSON is unchanged, its equipment is the processing and drying part of
 * machinery's register ({@code /api/machinery/equipment}), in both directions.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ProductTransformationEquipmentApiTest {

    private static final String EQUIPMENT_ID = "PT-EQ-API-TEST";

    @Autowired
    private MockMvc mockMvc;

    private String payload(String code, String name, String type) {
        return """
                {
                  "equipmentId": "%s",
                  "equipmentName": "%s",
                  "equipmentType": "%s",
                  "capacityKgPerHour": 120.00,
                  "energyConsumptionKwhPerKg": 0.85,
                  "location": "Demo hall",
                  "maintenanceStatus": "ACTIVE"
                }
                """.formatted(code, name, type);
    }

    private Number createMachineryEquipment(String name, String category, String stage) throws Exception {
        String body = mockMvc.perform(post("/api/machinery/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "category": "%s", "stage": "%s", "status": "OPERATIONAL"}
                                """.formatted(name, category, stage)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void equipmentCrudIsExposedUnderProductTransformationRoute() throws Exception {
        mockMvc.perform(post("/api/product-transformation/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(EQUIPMENT_ID, "Tunnel dryer test", "DRYING")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentId").value(EQUIPMENT_ID))
                .andExpect(jsonPath("$.equipmentName").value("Tunnel dryer test"))
                .andExpect(jsonPath("$.equipmentType").value("DRYING"))
                .andExpect(jsonPath("$.capacityKgPerHour").value(120.0))
                .andExpect(jsonPath("$.location").value("Demo hall"))
                .andExpect(jsonPath("$.maintenanceStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(get("/api/product-transformation/equipment/{id}", EQUIPMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentId").value(EQUIPMENT_ID));

        mockMvc.perform(get("/api/product-transformation/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].equipmentId", hasItem(EQUIPMENT_ID)));

        mockMvc.perform(get("/api/product-transformation/equipment/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].equipmentId", hasItem(EQUIPMENT_ID)));

        mockMvc.perform(get("/api/product-transformation/equipment/type/DRYING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].equipmentId", hasItem(EQUIPMENT_ID)));

        mockMvc.perform(put("/api/product-transformation/equipment/{id}", EQUIPMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"equipmentName": "Tunnel dryer renamed", "maintenanceStatus": "MAINTENANCE",
                                 "capacityKgPerHour": 150.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentName").value("Tunnel dryer renamed"))
                .andExpect(jsonPath("$.maintenanceStatus").value("MAINTENANCE"))
                .andExpect(jsonPath("$.capacityKgPerHour").value(150.0))
                .andExpect(jsonPath("$.location").value("Demo hall"));

        mockMvc.perform(get("/api/product-transformation/equipment/status/MAINTENANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].equipmentId", hasItem(EQUIPMENT_ID)));

        mockMvc.perform(delete("/api/product-transformation/equipment/{id}", EQUIPMENT_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/product-transformation/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].equipmentId", not(hasItem(EQUIPMENT_ID))));
        mockMvc.perform(get("/api/machinery/equipment").param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", not(hasItem(EQUIPMENT_ID))));
    }

    @Test
    void productTransformationEquipmentJoinsTheMachineryRegister() throws Exception {
        mockMvc.perform(post("/api/product-transformation/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("PT-EQ-REG-DRY", "Register solar dryer", "SOLAR_DRYER")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/product-transformation/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("PT-EQ-REG-WASH", "Register washer", "WASHING")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/machinery/equipment").param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.code == 'PT-EQ-REG-DRY')].name").value("Register solar dryer"))
                .andExpect(jsonPath("$.content[?(@.code == 'PT-EQ-REG-DRY')].category").value("PROCESSING_MACHINERY"))
                .andExpect(jsonPath("$.content[?(@.code == 'PT-EQ-REG-DRY')].stage").value("DRYING"))
                .andExpect(jsonPath("$.content[?(@.code == 'PT-EQ-REG-DRY')].status").value("OPERATIONAL"))
                .andExpect(jsonPath("$.content[?(@.code == 'PT-EQ-REG-WASH')].stage").value("PROCESSING"));
    }

    @Test
    void machineryProcessingMachinesAreSelectableInProductTransformation() throws Exception {
        Number dryerId = createMachineryEquipment("Register drum dryer", "PROCESSING_MACHINERY", "DRYING");
        createMachineryEquipment("Register field tractor", "AGRICULTURAL_MACHINERY", "CULTIVATION");

        List<String> codes = JsonPath.read(mockMvc.perform(get("/api/machinery/equipment").param("size", "1000"))
                .andReturn().getResponse().getContentAsString(),
                "$.content[?(@.id == " + dryerId + ")].code");
        String code = codes.get(0);

        mockMvc.perform(get("/api/product-transformation/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].equipmentName", hasItem("Register drum dryer")))
                .andExpect(jsonPath("$[*].equipmentName", not(hasItem("Register field tractor"))));

        mockMvc.perform(get("/api/product-transformation/equipment/{id}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentName").value("Register drum dryer"))
                .andExpect(jsonPath("$.maintenanceStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.equipmentType").doesNotExist());

        // Its product transformation attributes are given on its first edit.
        mockMvc.perform(put("/api/product-transformation/equipment/{id}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"equipmentType": "TUNNEL_DRYER", "capacityKgPerHour": 90.00,
                                 "energyConsumptionKwhPerKg": 1.2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentType").value("TUNNEL_DRYER"))
                .andExpect(jsonPath("$.capacityKgPerHour").value(90.0));

        // A status change in machinery is the status seen in product transformation.
        mockMvc.perform(patch("/api/machinery/equipment/{id}", dryerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"OUT_OF_SERVICE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/product-transformation/equipment/{id}", code))
                .andExpect(jsonPath("$.maintenanceStatus").value("RETIRED"));
    }

    @Test
    void equipmentCodeAndNameStayUniqueInTheRegister() throws Exception {
        mockMvc.perform(post("/api/product-transformation/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("PT-EQ-UNIQUE", "Unique washer", "WASHING")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/product-transformation/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("PT-EQ-UNIQUE", "Another washer", "WASHING")))
                .andExpect(status().isConflict());
    }

    @Test
    void equipmentNameAcceptsOneHundredCharacters() throws Exception {
        String name = "N".repeat(100);
        mockMvc.perform(post("/api/product-transformation/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("PT-EQ-LONG", name, "PACKAGING")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentName").value(name));
        mockMvc.perform(post("/api/machinery/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "category": "AGRICULTURAL_EQUIPMENT", "stage": "HARVEST",
                                 "status": "OPERATIONAL"}
                                """.formatted("m".repeat(100))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/product-transformation/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("PT-EQ-TOO-LONG", "N".repeat(101), "PACKAGING")))
                .andExpect(status().isBadRequest());
    }
}
