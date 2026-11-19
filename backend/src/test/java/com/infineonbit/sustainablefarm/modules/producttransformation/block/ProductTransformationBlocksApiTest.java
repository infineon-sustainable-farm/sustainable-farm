package com.infineonbit.sustainablefarm.modules.producttransformation.block;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.support.SiteTestBlocks;
import com.infineonbit.sustainablefarm.modules.plants.VarietyCatalogTestData;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lots keep their farm and block texts in the API while they reference a farm.block row; the block
 * must exist (owned by the farm module), Product Transformation never creates one.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductTransformationBlocksApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    @Autowired
    private SiteTestBlocks siteTestBlocks;

    @Autowired
    private FarmDirectory farmDirectory;

    @Test
    void aLotShowsTheCodeOfItsBlockAndTheNameOfItsFarm() throws Exception {
        VarietyCatalogTestData.ensureDefaults(varietyReferenceRepository);
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String farmName = "PT Farm " + suffix;
        String code = "PT" + suffix;
        Long farmId = siteTestBlocks.newFarm(farmName);
        siteTestBlocks.ensure(farmId, code);

        mockMvc.perform(post("/api/product-transformation/batches").contentType(MediaType.APPLICATION_JSON)
                        .content(batch("B-" + suffix + "-1", farmName.toLowerCase(), code.toLowerCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.farmId").value(farmName))
                .andExpect(jsonPath("$.blockId").value(code));

        mockMvc.perform(post("/api/product-transformation/batches").contentType(MediaType.APPLICATION_JSON)
                        .content(batch("B-" + suffix + "-2", String.valueOf(farmId), code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.farmId").value(farmName))
                .andExpect(jsonPath("$.blockId").value(code));

        mockMvc.perform(get("/api/product-transformation/batches/farm/{farm}", farmName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/product-transformation/batches/farm/{farm}/block/{block}", farmName, code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/product-transformation/batches/farm/{farm}", "Unknown farm " + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void anUnknownBlockAnUnknownFarmOrABlockOfAnotherFarmIsRefused() throws Exception {
        VarietyCatalogTestData.ensureDefaults(varietyReferenceRepository);
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Long farmId = siteTestBlocks.newFarm("PT Owner " + suffix);
        Long otherFarmId = siteTestBlocks.newFarm("PT Other " + suffix);
        String code = "PO" + suffix;
        siteTestBlocks.ensure(farmId, code);
        String unknown = "PU" + suffix;

        mockMvc.perform(post("/api/product-transformation/batches").contentType(MediaType.APPLICATION_JSON)
                        .content(batch("B-" + suffix + "-U", String.valueOf(farmId), unknown)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/product-transformation/batches").contentType(MediaType.APPLICATION_JSON)
                        .content(batch("B-" + suffix + "-F", "No such farm " + suffix, code)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/product-transformation/batches").contentType(MediaType.APPLICATION_JSON)
                        .content(batch("B-" + suffix + "-O", String.valueOf(otherFarmId), code)))
                .andExpect(status().isBadRequest());

        assertTrue(farmDirectory.findBlockByCode(unknown).isEmpty());
        assertTrue(farmDirectory.listBlocksOfFarm(otherFarmId).isEmpty());
    }

    private static String batch(String id, String farm, String block) {
        return """
                {"batchId": "%s", "harvestDate": "2026-11-01", "mangoVariety": "KENT",
                 "harvestQuantityKg": 120.50, "farmId": "%s", "blockId": "%s"}
                """.formatted(id, farm, block);
    }
}
