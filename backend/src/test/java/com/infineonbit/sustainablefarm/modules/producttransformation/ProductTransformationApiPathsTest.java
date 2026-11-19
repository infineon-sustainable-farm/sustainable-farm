package com.infineonbit.sustainablefarm.modules.producttransformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.infineonbit.sustainablefarm.modules.plants.VarietyCatalogTestData;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.historicalharvest.model.HistoricalHarvest;
import com.infineonbit.sustainablefarm.modules.producttransformation.historicalharvest.repository.HistoricalHarvestRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.infineonbit.sustainablefarm.modules.farm.support.SiteTestBlocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The product transformation API lives under {@code /api/product-transformation} (ADR 0002, T29),
 * its rows have a Long key and keep the identifier the clients send as a unique business code.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("Product transformation API paths and keys")
class ProductTransformationApiPathsTest {

    private static final List<String> RESOURCES = List.of("batches", "equipment", "operators", "harvest-events",
            "raw-intakes", "wash-sort-records", "drying-runs", "qc-checkpoints", "compliance-records",
            "packaging-records", "historical-harvests");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    @Autowired
    private HistoricalHarvestRepository historicalHarvestRepository;

    @Autowired
    private SiteTestBlocks siteTestBlocks;

    @Test
    @DisplayName("Every resource answers under the module prefix, no longer without it")
    void resourcesLiveUnderTheModulePrefix() throws Exception {
        for (String resource : RESOURCES) {
            mockMvc.perform(get("/api/product-transformation/" + resource)).andExpect(status().isOk());
            mockMvc.perform(get("/api/" + resource)).andExpect(status().isNotFound());
        }
        mockMvc.perform(get("/api/product-transformation/harvest-events/plants/all")).andExpect(status().isOk());
        mockMvc.perform(get("/api/harvest-events/plants/all")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/product-transformation/dashboard/kpis")).andExpect(status().isOk());
        mockMvc.perform(get("/api/producttransformation/dashboard/kpis")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/crop-storage/entries").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A batch has a Long key, its code is the batchId of the JSON and of the path")
    void aBatchKeepsItsCodeNextToItsLongKey() throws Exception {
        VarietyCatalogTestData.ensureDefaults(varietyReferenceRepository);
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String farm = "Paths Farm " + suffix;
        String code = "LOT-PATHS-" + suffix;
        String block = "PP" + suffix;
        siteTestBlocks.ensure(siteTestBlocks.newFarm(farm), block);

        mockMvc.perform(post("/api/product-transformation/batches").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchId": "%s", "harvestDate": "2026-11-01", "mangoVariety": "KENT",
                                 "harvestQuantityKg": 120.50, "farmId": "%s", "blockId": "%s"}
                                """.formatted(code, farm, block)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchId").value(code))
                .andExpect(jsonPath("$.id").doesNotExist());
        mockMvc.perform(get("/api/product-transformation/batches/{batchId}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(code));

        Map<String, Object> row = jdbc.queryForMap("select id, created_at, updated_at from batch where code = ?", code);
        assertThat(row.get("id")).isInstanceOf(Long.class);
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();

        mockMvc.perform(post("/api/product-transformation/batches").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchId": "%s", "harvestDate": "2026-11-01", "mangoVariety": "KENT",
                                 "harvestQuantityKg": 1, "farmId": "%s", "blockId": "%s"}
                                """.formatted(code, farm, block)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("A historical harvest is unique by year, month, week and variety")
    void aHistoricalHarvestIsUniqueByPeriodAndVariety() {
        VarietyCatalogTestData.ensureDefaults(varietyReferenceRepository);
        historicalHarvestRepository.saveAndFlush(historicalHarvest());

        assertThat(historicalHarvestRepository.findByYearAndMonthAndWeekAndVarietyKey(1990, 1, 2, "KENT"))
                .get().extracting(HistoricalHarvest::getId).isNotNull();
        assertThatThrownBy(() -> historicalHarvestRepository.saveAndFlush(historicalHarvest()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static HistoricalHarvest historicalHarvest() {
        HistoricalHarvest harvest = new HistoricalHarvest();
        harvest.setYear(1990);
        harvest.setMonth(1);
        harvest.setWeek(2);
        harvest.setVarietyKey("KENT");
        harvest.setHarvestQuantityKg(new BigDecimal("10.00"));
        return harvest;
    }
}
