package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.Money;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.AlertStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.SolarGenerationLog;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.GeneratorEventRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.SolarGenerationLogRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.service.AlertService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every energy supply endpoint lives under {@code /api/energy-supply} (ADR 0002, A5). Components
 * have a Long id and keep their business code as the {@code componentId} of the API; costs are
 * stored as {@link Money} in euros while the API keeps plain numbers.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class EnergySupplyApiPathsTest {

    private static final String BASE = "/api/energy-supply";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnergyComponentRepository componentRepository;

    @Autowired
    private SolarGenerationLogRepository solarRepository;

    @Autowired
    private GeneratorEventRepository generatorEventRepository;

    @Autowired
    private AlertService alertService;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void everyListEndpointLivesUnderTheModulePrefix() throws Exception {
        for (String path : new String[] {"/components", "/solar", "/solar/last-7-days", "/consumption",
                "/consumption/today", "/consumption/breakdown/IRRIGATION", "/generator-events", "/carbon",
                "/alerts", "/alerts/open", "/dashboard/summary"}) {
            mockMvc.perform(get(BASE + path)).andExpect(status().isOk());
            mockMvc.perform(get("/api/energy" + path)).andExpect(status().isNotFound());
        }
        mockMvc.perform(post(BASE + "/whatif/simulate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"additionalPanels\":10,\"additionalBatteryKwh\":5,\"productionScaleKgPerDay\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalCapexEur").value(2850.0));
    }

    @Test
    void aComponentIsCreatedOnceUnderItsCodeAndReferencedByItsIdInternally() throws Exception {
        mockMvc.perform(post(BASE + "/components").contentType(MediaType.APPLICATION_JSON).content(component("ENG-951")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith(BASE + "/components/ENG-951")))
                .andExpect(jsonPath("$.componentId").value("ENG-951"));
        mockMvc.perform(post(BASE + "/components").contentType(MediaType.APPLICATION_JSON).content(component("ENG-951")))
                .andExpect(status().isConflict());
        mockMvc.perform(get(BASE + "/components/ENG-951"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paths test array"));

        String body = mockMvc.perform(post(BASE + "/solar").contentType(MediaType.APPLICATION_JSON).content("""
                        {"componentId":"ENG-951","date":"%s","kwhProduced":22.5,"efficiencyPct":80}
                        """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.componentId").value("ENG-951"))
                .andReturn().getResponse().getContentAsString();
        long logId = ((Number) JsonPath.read(body, "$.logId")).longValue();
        mockMvc.perform(get(BASE + "/solar/{id}", logId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.componentName").value("Paths test array"));

        Long componentId = componentRepository.findByCode("ENG-951").orElseThrow().getId();
        Long referencedId = transactions.execute(status -> {
            SolarGenerationLog log = solarRepository.findById(logId).orElseThrow();
            return log.getComponent().getId();
        });
        assertThat(referencedId).isEqualTo(componentId);
    }

    @Test
    void aComponentThatAnAlertIsAboutCannotBeDeleted() throws Exception {
        mockMvc.perform(post(BASE + "/components").contentType(MediaType.APPLICATION_JSON).content(component("ENG-952")))
                .andExpect(status().isCreated());
        alertService.raise(componentRepository.findByCode("ENG-952").orElseThrow(), AlertSeverity.WARNING,
                "Paths test alert", Instant.now(), AlertStatus.OPEN);

        mockMvc.perform(get(BASE + "/alerts/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.message == 'Paths test alert')].componentId").value("ENG-952"));
        mockMvc.perform(delete(BASE + "/components/ENG-952")).andExpect(status().isConflict());

        mockMvc.perform(post(BASE + "/components").contentType(MediaType.APPLICATION_JSON).content(component("ENG-953")))
                .andExpect(status().isCreated());
        mockMvc.perform(delete(BASE + "/components/ENG-953")).andExpect(status().isNoContent());
    }

    @Test
    void generatorCostIsStoredInEurosAndReadAsANumber() throws Exception {
        String body = mockMvc.perform(post(BASE + "/generator-events").contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"%s","triggerReason":"Paths test","durationHours":1.5,"fuelLiters":3,"costEur":6.8}
                        """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costEur").value(6.8))
                .andReturn().getResponse().getContentAsString();
        long eventId = ((Number) JsonPath.read(body, "$.eventId")).longValue();

        assertThat(generatorEventRepository.findById(eventId).orElseThrow().getCost())
                .isEqualTo(Money.of("6.80", CurrencyCode.EUR));
        mockMvc.perform(get(BASE + "/generator-events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costEur").value(6.8));
    }

    private static String component(String code) {
        return """
                {"componentId":"%s","name":"Paths test array","category":"PV_ARRAY","capacityValue":1.5,"status":"OPERATIONAL"}
                """.formatted(code);
    }
}
