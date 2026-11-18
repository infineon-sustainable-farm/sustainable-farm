package com.infineonbit.sustainablefarm.modules.producttransformation.lot;

import com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.model.PackageType;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.QualityGrade;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.BatchStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infineonbit.sustainablefarm.modules.plants.VarietyCatalogTestData;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.block.ProductTransformationBlocks;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.repository.HarvestEventRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.service.HarvestEventService;
import com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.model.PackagingRecord;
import com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.repository.PackagingRecordRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
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
 * The lot chain (AC-8) is linked by foreign keys, with no new behaviour (A11): a plants harvest
 * record to the product transformation harvest event imported from it, the processing batch to
 * its packaged lots, a sales batch to the packaged lot of the same code, and a water consumption
 * to the processing batch of its lot. The sales and water JSON stay as they were.
 *
 * <p>Block LC1 is used by no other test.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("Lot chain foreign keys")
class LotChainForeignKeyTest {

    private static final String BLOCK = "LC1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    @Autowired
    private ProductTransformationBlocks blocks;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private PackagingRecordRepository packagingRecordRepository;

    @Autowired
    private HarvestEventRepository harvestEventRepository;

    @Autowired
    private HarvestEventService harvestEventService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private long farm;

    @BeforeEach
    void farmOfBlockLc1() throws Exception {
        VarietyCatalogTestData.ensureDefaults(varietyReferenceRepository);
        String createdFarm = mockMvc.perform(post("/api/farms").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lot Chain Farm\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        farm = objectMapper.readTree(createdFarm).get("id").asLong();
    }

    private long plantsHarvest() throws Exception {
        mockMvc.perform(post("/api/plants/plantings").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"farmId": %d, "blockCode": "%s", "varietyName": "Keitt",
                                 "plantingDate": "2020-01-10", "treeCount": 10}
                                """.formatted(farm, BLOCK)))
                .andExpect(status().isCreated());
        String created = mockMvc.perform(post("/api/plants/harvests").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"farmId": %d, "blockCode": "%s", "varietyName": "Keitt",
                                 "harvestDate": "2026-06-01", "quantityKg": 300}
                                """.formatted(farm, BLOCK)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created).get("id").asLong();
    }

    private HarvestEvent harvestEvent(String harvestId, String batchId, Long plantsHarvestId) {
        HarvestEvent event = new HarvestEvent();
        event.setHarvestId(harvestId);
        event.setBatchId(batchId);
        event.setPlantsHarvestId(plantsHarvestId);
        event.setHarvestDate(LocalDate.of(2026, 6, 1));
        event.setVarietyKey("KEITT");
        event.setFarmBlockId(blocks.resolve(String.valueOf(farm), BLOCK));
        event.setHarvestQuantityKg(new BigDecimal("300.00"));
        event.setQualityGrade(QualityGrade.A);
        return event;
    }

    private Batch batch(String batchId) {
        Batch batch = new Batch();
        batch.setBatchId(batchId);
        batch.setHarvestDate(LocalDate.of(2026, 6, 1));
        batch.setVarietyKey("KEITT");
        batch.setHarvestQuantityKg(new BigDecimal("300.00"));
        batch.setCurrentStatus(BatchStatus.PACKAGING);
        batch.setFarmBlockId(blocks.resolve(String.valueOf(farm), BLOCK));
        return batchRepository.saveAndFlush(batch);
    }

    private PackagingRecord packagedLot(String recordId, Batch batch, String lotCode) {
        PackagingRecord record = new PackagingRecord();
        record.setRecordId(recordId);
        record.setBatch(batch);
        record.setPackageType(PackageType.BULK);
        record.setPackageQuantityKg(new BigDecimal("280.00"));
        record.setLotCode(lotCode);
        record.setExportReady(false);
        record.setPackagingDate(LocalDate.of(2026, 6, 3));
        return packagingRecordRepository.saveAndFlush(record);
    }

    @Test
    @DisplayName("A harvest event keeps the plants harvest it comes from, which must exist")
    void harvestEventReferencesThePlantsHarvestRecord() throws Exception {
        long harvestId = plantsHarvest();

        harvestEventService.createHarvestEvent(harvestEvent("HE-LC-1", "LOT-LC-1", harvestId));
        harvestEventRepository.flush();

        assertThat(jdbc.queryForObject("select plants_harvest_id from harvest_event where code = 'HE-LC-1'",
                Long.class)).isEqualTo(harvestId);
        assertThatThrownBy(() -> harvestEventService.createHarvestEvent(harvestEvent("HE-LC-2", "LOT-LC-2", 987654321L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("987654321");
        assertThatThrownBy(() -> harvestEventRepository.saveAndFlush(harvestEvent("HE-LC-3", "LOT-LC-3", 987654321L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("A plants harvest still referenced by a harvest event cannot be deleted")
    void referencedPlantsHarvestCannotBeDeleted() throws Exception {
        long harvestId = plantsHarvest();
        harvestEventRepository.saveAndFlush(harvestEvent("HE-LC-4", "LOT-LC-4", harvestId));

        assertThatThrownBy(() -> jdbc.update("delete from harvest_record where id = ?", harvestId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("A packaged lot cannot reference an unknown processing batch")
    void packagedLotReferencesItsBatch() {
        assertThatThrownBy(() -> jdbc.update("insert into packaging_record (code, batch_id, package_type,"
                + " package_quantity_kg, lot_code, export_ready, packaging_date, created_at, updated_at) values"
                + " ('PKG-LC-X', -1, 'BULK', 1, 'LOT-LC-X-PKG', false, DATE '2026-06-03',"
                + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("A sales batch with the code of a packaged lot is linked to it, with the same JSON")
    void salesBatchIsLinkedToThePackagedLotOfItsCode() throws Exception {
        long packagedLotId = packagedLot("PKG-LC-5", batch("LOT-LC-5"), "LOT-LC-5-PKG").getId();

        String linked = mockMvc.perform(post("/api/product-batches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":\"LOT-LC-5-PKG\",\"stockT\":0.28,\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchCode").value("LOT-LC-5-PKG"))
                .andExpect(jsonPath("$.packagingRecordId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        int linkedId = objectMapper.readTree(linked).get("batchId").asInt();
        String other = mockMvc.perform(post("/api/product-batches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":\"SALES-ONLY-1\",\"stockT\":1,\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int otherId = objectMapper.readTree(other).get("batchId").asInt();
        entityManager.flush();

        assertThat(packagingRecordIdOfSalesBatch(linkedId)).isEqualTo(packagedLotId);
        assertThat(packagingRecordIdOfSalesBatch(otherId)).isNull();
        mockMvc.perform(get("/api/product-batches/{id}", linkedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockT").value(0.28))
                .andExpect(jsonPath("$.packagingRecordId").doesNotExist());

        // Product transformation can still delete the lot: the sales batch keeps its code, the link is cleared.
        jdbc.update("delete from packaging_record where code = 'PKG-LC-5'");
        assertThat(packagingRecordIdOfSalesBatch(linkedId)).isNull();
    }

    @Test
    @DisplayName("A water consumption whose lot is a processing batch is linked to it, with the same JSON")
    void waterConsumptionIsLinkedToTheProcessingBatchOfItsLot() throws Exception {
        long batchId = batch("LOT-LC-6").getId();
        String source = mockMvc.perform(post("/api/water/sources").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"farmId":"%d","name":"Lot Chain Tank","type":"tank","capacityLiters":5000,"currentLevelLiters":2500}
                                """.formatted(farm)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sourceId = objectMapper.readTree(source).get("id").asText();

        String linked = mockMvc.perform(post("/api/water/consumption").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"farmId":"%d","sourceId":"%s","consumptionLiters":120,
                                 "consumptionDate":"2026-06-02T09:00:00Z","lotId":" LOT-LC-6 ","activity":"washing"}
                                """.formatted(farm, sourceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lotId").value("LOT-LC-6"))
                .andExpect(jsonPath("$.processingBatchId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String linkedId = objectMapper.readTree(linked).get("id").asText();
        String free = mockMvc.perform(post("/api/water/consumption").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"farmId":"%d","sourceId":"%s","consumptionLiters":80,
                                 "consumptionDate":"2026-06-02T10:00:00Z","lotId":"EXTERNAL-LOT"}
                                """.formatted(farm, sourceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lotId").value("EXTERNAL-LOT"))
                .andReturn().getResponse().getContentAsString();
        String freeId = objectMapper.readTree(free).get("id").asText();
        entityManager.flush();

        assertThat(processingBatchIdOfConsumption(linkedId)).isEqualTo(batchId);
        assertThat(processingBatchIdOfConsumption(freeId)).isNull();

        // Product transformation can still delete the batch: the consumption keeps its lot, the link is cleared.
        jdbc.update("delete from batch where code = 'LOT-LC-6'");
        assertThat(processingBatchIdOfConsumption(linkedId)).isNull();
        assertThat(jdbc.queryForObject("select lot_id from water_consumption where id = ?", String.class,
                java.util.UUID.fromString(linkedId))).isEqualTo("LOT-LC-6");
    }

    private Long packagingRecordIdOfSalesBatch(int batchId) {
        return jdbc.queryForObject("select packaging_record_id from product_batches where id = ?",
                Long.class, batchId);
    }

    private Long processingBatchIdOfConsumption(String consumptionId) {
        return jdbc.queryForObject("select processing_batch_id from water_consumption where id = ?",
                Long.class, java.util.UUID.fromString(consumptionId));
    }
}
