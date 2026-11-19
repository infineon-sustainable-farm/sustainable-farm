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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The product transformation equipment API on the single equipment register (ADR 0004).
 * Machinery owns the machines ({@code /api/machinery/equipment}): it alone creates, renames,
 * changes the status of and deletes them. {@code /api/product-transformation/equipment} lists
 * the processing and drying machines, with the JSON of before, and edits only their product
 * transformation attributes.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ProductTransformationEquipmentApiTest {

    private static final String PT_EQUIPMENT = "/api/product-transformation/equipment";

    @Autowired
    private MockMvc mockMvc;

    /** Registers a machine in machinery and returns its business code. */
    private String createMachineryEquipment(String name, String category, String stage) throws Exception {
        String body = mockMvc.perform(post("/api/machinery/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "category": "%s", "stage": "%s", "status": "OPERATIONAL"}
                                """.formatted(name, category, stage)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.code");
    }

    private Number machineryIdOf(String code) throws Exception {
        String body = mockMvc.perform(get("/api/machinery/equipment").param("size", "1000"))
                .andReturn().getResponse().getContentAsString();
        java.util.List<Number> ids = JsonPath.read(body, "$.content[?(@.code == '" + code + "')].id");
        return ids.get(0);
    }

    @Test
    void productTransformationCannotCreateOrDeleteEquipment() throws Exception {
        String code = createMachineryEquipment("Owner washer", "PROCESSING_MACHINERY", "PROCESSING");

        mockMvc.perform(post(PT_EQUIPMENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"equipmentId": "PT-EQ-NEW", "equipmentName": "Washer from PT",
                                 "equipmentType": "WASHING", "capacityKgPerHour": 120.00,
                                 "energyConsumptionKwhPerKg": 0.85, "maintenanceStatus": "ACTIVE"}
                                """))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete(PT_EQUIPMENT + "/{id}", code))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(get("/api/machinery/equipment").param("size", "1000"))
                .andExpect(jsonPath("$.content[*].code", hasItem(code)))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Washer from PT"))));
    }

    @Test
    void machineryProcessingMachinesAreListedWithoutAttributesUntilEdited() throws Exception {
        String code = createMachineryEquipment("Register drum dryer", "PROCESSING_MACHINERY", "DRYING");
        createMachineryEquipment("Register field tractor", "AGRICULTURAL_MACHINERY", "CULTIVATION");

        mockMvc.perform(get(PT_EQUIPMENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].equipmentName", hasItem("Register drum dryer")))
                .andExpect(jsonPath("$[*].equipmentName", not(hasItem("Register field tractor"))));

        mockMvc.perform(get(PT_EQUIPMENT + "/{id}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentId").value(code))
                .andExpect(jsonPath("$.equipmentName").value("Register drum dryer"))
                .andExpect(jsonPath("$.maintenanceStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.equipmentType").doesNotExist());

        // Its product transformation attributes are given on its first edit.
        mockMvc.perform(put(PT_EQUIPMENT + "/{id}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"equipmentType": "TUNNEL_DRYER", "capacityKgPerHour": 90.00,
                                 "energyConsumptionKwhPerKg": 1.2, "location": "Demo hall",
                                 "lastMaintenanceDate": "2026-11-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentType").value("TUNNEL_DRYER"))
                .andExpect(jsonPath("$.capacityKgPerHour").value(90.0))
                .andExpect(jsonPath("$.location").value("Demo hall"))
                .andExpect(jsonPath("$.lastMaintenanceDate").value("2026-11-01"));

        mockMvc.perform(get(PT_EQUIPMENT + "/type/TUNNEL_DRYER"))
                .andExpect(jsonPath("$[*].equipmentId", hasItem(code)));
        mockMvc.perform(get(PT_EQUIPMENT + "/available"))
                .andExpect(jsonPath("$[*].equipmentId", hasItem(code)));
    }

    @Test
    void anEditChangesOnlyTheAttributesNeverTheMachine() throws Exception {
        String code = createMachineryEquipment("Spec only packer", "PROCESSING_MACHINERY", "PROCESSING");
        mockMvc.perform(put(PT_EQUIPMENT + "/{id}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"equipmentType": "PACKAGING", "capacityKgPerHour": 60.00,
                                 "energyConsumptionKwhPerKg": 0.95}
                                """))
                .andExpect(status().isOk());

        // A name or a status sent anyway is ignored: machinery keeps them.
        mockMvc.perform(put(PT_EQUIPMENT + "/{id}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"equipmentName": "Renamed from PT", "maintenanceStatus": "RETIRED",
                                 "capacityKgPerHour": 75.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentName").value("Spec only packer"))
                .andExpect(jsonPath("$.maintenanceStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.equipmentType").value("PACKAGING"))
                .andExpect(jsonPath("$.capacityKgPerHour").value(75.0));

        mockMvc.perform(get("/api/machinery/equipment").param("size", "1000"))
                .andExpect(jsonPath("$.content[?(@.code == '" + code + "')].name").value("Spec only packer"))
                .andExpect(jsonPath("$.content[?(@.code == '" + code + "')].status").value("OPERATIONAL"));
    }

    @Test
    void theFirstEditNeedsTypeCapacityAndEnergy() throws Exception {
        String code = createMachineryEquipment("Incomplete washer", "PROCESSING_MACHINERY", "PROCESSING");
        mockMvc.perform(put(PT_EQUIPMENT + "/{id}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\": \"Hall\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownOrNonProcessingMachineIsNotEdited() throws Exception {
        String tractor = createMachineryEquipment("Spec tractor", "AGRICULTURAL_MACHINERY", "CULTIVATION");
        String spec = """
                {"equipmentType": "WASHING", "capacityKgPerHour": 10, "energyConsumptionKwhPerKg": 1}
                """;
        mockMvc.perform(put(PT_EQUIPMENT + "/{id}", tractor)
                        .contentType(MediaType.APPLICATION_JSON).content(spec))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(PT_EQUIPMENT + "/{id}", "EQ-UNKNOWN")
                        .contentType(MediaType.APPLICATION_JSON).content(spec))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aStatusChangeInMachineryIsTheStatusSeenInProductTransformation() throws Exception {
        String code = createMachineryEquipment("Status dryer", "PROCESSING_MACHINERY", "DRYING");

        mockMvc.perform(patch("/api/machinery/equipment/{id}", machineryIdOf(code))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"OUT_OF_SERVICE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(PT_EQUIPMENT + "/{id}", code))
                .andExpect(jsonPath("$.maintenanceStatus").value("RETIRED"));
        mockMvc.perform(get(PT_EQUIPMENT + "/status/RETIRED"))
                .andExpect(jsonPath("$[*].equipmentId", hasItem(code)));
    }
}
