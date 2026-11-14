package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.QualityGrade;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.repository.HarvestEventRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Import a harvest recorded in Plants as a Product Transformation harvest event.
 *
 * <p>Identity convention (option B, agreed with the Plants team): Product
 * Transformation generates its own batch identifier and keeps the Plants lot
 * number and harvest id in dedicated fields. The two identifiers are derived
 * deterministically from the Plants harvest id, which makes them stable and
 * readable in logs:
 *
 * <ul>
 *   <li>harvest event id: {@code H-PL-<plantsHarvestId>}</li>
 *   <li>batch id: {@code PT-BATCH-<plantsHarvestId>}</li>
 * </ul>
 *
 * <p>The import is idempotent: importing the same Plants harvest twice returns
 * the event created the first time, without writing anything. The guarantee is
 * enforced twice — by a lookup before insert, and by the unique index on
 * {@code plants_harvest_id} — so two concurrent imports cannot create a
 * duplicate either.
 *
 * <p>No transaction is declared at method level on purpose: the duplicate-key
 * recovery has to re-read after a failed insert, and a rolled-back transaction
 * would refuse that read.
 */
@Service
public class PlantsHarvestImportService {

    private static final String HARVEST_ID_PREFIX = "H-PL-";
    private static final String BATCH_ID_PREFIX = "PT-BATCH-";

    private final PlantsIntegrationService plantsIntegrationService;
    private final HarvestEventRepository harvestEventRepository;

    public PlantsHarvestImportService(PlantsIntegrationService plantsIntegrationService,
                                      HarvestEventRepository harvestEventRepository) {
        this.plantsIntegrationService = plantsIntegrationService;
        this.harvestEventRepository = harvestEventRepository;
    }

    /**
     * Import one Plants harvest.
     *
     * @param plantsHarvestId identifier of the harvest in Plants
     * @param qualityGrade    grade assigned at reception; Plants does not record one,
     *                        and Product Transformation requires it, so the caller
     *                        states it rather than the import inventing a value
     * @return the existing or newly created event, with a flag telling which
     */
    public ImportOutcome importHarvest(Long plantsHarvestId, QualityGrade qualityGrade) {
        if (plantsHarvestId == null) {
            throw new IllegalArgumentException("Plants harvest id is required");
        }
        if (qualityGrade == null) {
            throw new IllegalArgumentException("Quality grade is required: Plants does not record one");
        }

        return harvestEventRepository.findByPlantsHarvestId(plantsHarvestId)
                .map(existing -> new ImportOutcome(existing, false))
                .orElseGet(() -> createFromPlants(plantsHarvestId, qualityGrade));
    }

    private ImportOutcome createFromPlants(Long plantsHarvestId, QualityGrade qualityGrade) {
        PlantsHarvestResponse harvest = plantsIntegrationService.getHarvestById(plantsHarvestId);
        if (harvest == null) {
            throw new ResourceNotFoundException("Plants harvest not found: " + plantsHarvestId);
        }

        HarvestEvent event = plantsIntegrationService.toHarvestEvent(harvest, BATCH_ID_PREFIX + plantsHarvestId);
        event.setHarvestId(HARVEST_ID_PREFIX + plantsHarvestId);
        event.setQualityGrade(qualityGrade);

        try {
            return new ImportOutcome(harvestEventRepository.save(event), true);
        } catch (DataIntegrityViolationException ex) {
            // A concurrent import may have won the race: return what it wrote.
            // Any other constraint violation is a real failure and must surface as
            // itself, not be disguised as a duplicate id.
            return harvestEventRepository.findByPlantsHarvestId(plantsHarvestId)
                    .map(existing -> new ImportOutcome(existing, false))
                    .orElseThrow(() -> ex);
        }
    }

    /**
     * Result of an import attempt.
     *
     * @param event   the stored harvest event
     * @param created true when this call created it, false when it already existed
     */
    public record ImportOutcome(HarvestEvent event, boolean created) {
    }
}
