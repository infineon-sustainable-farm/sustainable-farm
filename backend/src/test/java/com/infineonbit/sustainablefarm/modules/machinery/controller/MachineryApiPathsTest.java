package com.infineonbit.sustainablefarm.modules.machinery.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.Money;
import com.infineonbit.sustainablefarm.modules.machinery.entity.SparePart;
import com.infineonbit.sustainablefarm.modules.machinery.repository.SparePartRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Every machinery endpoint lives under {@code /api/machinery} (ADR 0002, A5), and the costs are
 * stored as {@link Money} in the machinery currency while the API keeps plain numbers.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MachineryApiPathsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SparePartRepository sparePartRepository;

    @Test
    void equipmentAndOperatorAssignmentsLiveUnderTheModulePrefix() throws Exception {
        String body = mockMvc.perform(post("/api/machinery/equipment").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Paths test tractor","category":"AGRICULTURAL_MACHINERY","stage":"CULTIVATION","status":"OPERATIONAL"}
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number equipmentId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/machinery/equipment").param("size", "1000"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/machinery/operator-assignments").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Paths Test Operator","equipmentId":%d,"jobTitle":"Driver","startDate":"2026-01-01"}
                        """.formatted(equipmentId.longValue())))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/machinery/operator-assignments"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/equipments")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/operator-assignments")).andExpect(status().isNotFound());
    }

    @Test
    void sparePartUnitCostIsStoredInDollarsAndReadAsANumber() throws Exception {
        String body = mockMvc.perform(post("/api/machinery/spare-parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Paths test filter","quantity":5,"reorderThreshold":2,"unitCost":12.5}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitCost").value(12.5))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(patch("/api/machinery/spare-parts/{id}/unit-cost", id).param("unitCost", "14.25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitCost").value(14.25));

        SparePart stored = sparePartRepository.findById(id).orElseThrow();
        assertThat(stored.getUnitCost()).isEqualTo(Money.of("14.25", CurrencyCode.USD));
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
    }
}
