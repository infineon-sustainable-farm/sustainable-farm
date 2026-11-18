package com.infineonbit.sustainablefarm.modules.producttransformation.variety;

import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.BatchStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.block.ProductTransformationBlocks;
import com.infineonbit.sustainablefarm.modules.plants.VarietyCatalogTestData;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.repository.BatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The mango variety of producttransformation is stored as a key of the plants
 * catalogue, with a foreign key, while its API keeps the values of the former enum.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class MangoVarietyApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private ProductTransformationBlocks blocks;

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    @BeforeEach
    void catalogue() {
        VarietyCatalogTestData.ensureDefaults(varietyReferenceRepository);
    }

    private static String batch(String id, String variety) {
        return """
                {"batchId": "%s", "harvestDate": "2026-11-01", "mangoVariety": "%s",
                 "harvestQuantityKg": 120.50, "farmId": "F-VAR", "blockId": "B-VAR"}
                """.formatted(id, variety);
    }

    @Test
    void ameliesIsSentBackAsBeforeAndStoredUnderItsCatalogueKey() throws Exception {
        mockMvc.perform(post("/api/product-transformation/batches").contentType(MediaType.APPLICATION_JSON)
                        .content(batch("B-VAR-AMELIE", "AMÉLIE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mangoVariety").value("AMÉLIE"));

        assertThat(batchRepository.findByBatchId("B-VAR-AMELIE").orElseThrow().getVarietyKey()).isEqualTo("AMELIE");

        mockMvc.perform(get("/api/product-transformation/batches/variety/{variety}", "AMÉLIE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].batchId", hasItem("B-VAR-AMELIE")))
                .andExpect(jsonPath("$[*].mangoVariety", hasItem("AMÉLIE")));
    }

    @Test
    void theOtherFormerValuesAreSentBackUnchanged() throws Exception {
        for (String variety : new String[] {"KEITT", "KENT", "TOMMY", "OTHER"}) {
            mockMvc.perform(post("/api/product-transformation/batches").contentType(MediaType.APPLICATION_JSON)
                            .content(batch("B-VAR-" + variety, variety)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mangoVariety").value(variety));
        }
    }

    @Test
    void anUnknownVarietyIsRefused() throws Exception {
        mockMvc.perform(post("/api/product-transformation/batches").contentType(MediaType.APPLICATION_JSON)
                        .content(batch("B-VAR-UNKNOWN", "ALPHONSO")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/product-transformation/batches/variety/{variety}", "ALPHONSO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theDatabaseRefusesAKeyOutsideTheCatalogue() {
        Batch batch = new Batch();
        batch.setBatchId("B-VAR-FK");
        batch.setHarvestDate(LocalDate.of(2026, 11, 1));
        batch.setVarietyKey("ALPHONSO");
        batch.setHarvestQuantityKg(new BigDecimal("10.00"));
        batch.setCurrentStatus(BatchStatus.CREATED);
        batch.setFarmBlockId(blocks.resolve("F-VAR", "B-VAR"));

        assertThatThrownBy(() -> batchRepository.saveAndFlush(batch))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
