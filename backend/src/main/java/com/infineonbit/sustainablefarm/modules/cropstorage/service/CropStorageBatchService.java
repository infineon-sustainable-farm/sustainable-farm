package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.ValuationSummary;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.CropStorageBatch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Supplier;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.CropStorageBatchRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StorageZoneRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Core inventory service of Crop Storage: batch persistence, FIFO rotation
 * with customer priority, multi-unit inventory valuation (kg, tonne, lb, m3,
 * FCFA, EUR, USD) and expiry listing for Sales and Marketing.
 */
@Service
@RequiredArgsConstructor
public class CropStorageBatchService {

    /** Fixed CFA franc peg: 1 EUR = 655.957 XOF. */
    private static final BigDecimal XOF_PER_EUR = new BigDecimal("655.957");

    /** HYPOTHESIS: indicative USD rate, to be confirmed with finance. */
    private static final BigDecimal XOF_PER_USD = new BigDecimal("600");

    /** HYPOTHESIS: bulk density of packaged dried mango, kg per m3. */
    private static final BigDecimal KG_PER_M3 = new BigDecimal("700");

    /** 1 lb = 0.453592 kg. */
    private static final BigDecimal KG_PER_LB = new BigDecimal("0.453592");

    private final CropStorageBatchRepository batchRepository;
    private final SupplierRepository supplierRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final SharedReferences sharedReferences;

    /**
     * Saves a new batch exactly as received; the batch code follows the team
     * convention B26-01 (block letter + 2-digit year + sequence) and is unique.
     *
     * <p>When the batch is placed in a zone, its stock must fit in the zone: the
     * zone row is locked, the stock already stored is summed from the database,
     * and the batch is written, all in one transaction. Concurrent writes filling
     * the same zone (API-005 entry, IN movement, another creation) take the same
     * lock and are serialized, so they cannot overfill it together.
     *
     * <p>The variety is given by name and stored as its Plants catalogue key; a harvest
     * id must be a known Plants harvest (400 otherwise).
     *
     * @param batch batch parsed from the request body
     * @param varietyName variety name to resolve, or {@code null}
     * @return the saved batch with its generated id
     * @throws ConflictException when the batch stock would exceed the zone capacity
     */
    /** Same as {@link #create(CropStorageBatch, String)} for a batch without a variety. */
    @Transactional
    public CropStorageBatch create(CropStorageBatch batch) {
        return create(batch, null);
    }

    @Transactional
    public CropStorageBatch create(CropStorageBatch batch, String varietyName) {
        batch.setVarietyKey(sharedReferences.varietyKeyOf(varietyName));
        sharedReferences.requireHarvest(batch.getHarvestId());
        if (batch.getSupplier() != null) {
            Long supplierId = requireId(batch.getSupplier().getId(), "supplier");
            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id " + supplierId));
            batch.setSupplier(supplier);
        }
        if (batch.getStorageZone() != null) {
            Long zoneId = requireId(batch.getStorageZone().getId(), "zone");
            StorageZone zone = storageZoneRepository.findForUpdateById(zoneId)
                    .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId));
            checkCapacity(zone, batch);
            batch.setStorageZone(zone);
        }
        return batchRepository.save(batch);
    }

    /**
     * Same rule as an IN movement: stock already in the locked zone plus the new
     * batch stock must stay within capacityKg. The new batch stock is its current
     * quantity, or its initial quantity when no current quantity is given (the
     * value the entity stores on creation).
     */
    private void checkCapacity(StorageZone zone, CropStorageBatch batch) {
        if (zone.getCapacityKg() == null) {
            return;
        }
        BigDecimal incoming = batch.getCurrentQuantityKg() != null
                ? batch.getCurrentQuantityKg()
                : Objects.requireNonNullElse(batch.getInitialQuantityKg(), BigDecimal.ZERO);
        BigDecimal stored = Objects.requireNonNullElse(
                batchRepository.sumCurrentQuantityKgByStorageZoneId(zone.getId()), BigDecimal.ZERO);
        if (stored.add(incoming).compareTo(zone.getCapacityKg()) > 0) {
            throw new ConflictException("Zone capacity exceeded");
        }
    }

    private Long requireId(Long id, String reference) {
        if (id == null) {
            throw new BusinessRuleException("A " + reference + " reference requires an id");
        }
        return id;
    }

    /**
     * Returns every batch, whatever its stage or status.
     *
     * @return all batches of the module
     */
    public List<CropStorageBatch> findAll() {
        return batchRepository.findAll();
    }

    /**
     * Stock rotation queue agreed with Anar: first in, first out, with higher
     * customer priority served first; empty batches are excluded.
     *
     * @return batches with remaining stock, ordered by customerPriority desc
     *         then storageEntryDate asc
     */
    public List<CropStorageBatch> fifo() {
        return batchRepository
                .findByCurrentQuantityKgGreaterThanOrderByCustomerPriorityDescStorageEntryDateAsc(BigDecimal.ZERO);
    }

    /**
     * Multi-unit valuation for international reporting. Only kg and FCFA are
     * stored in the database; the other units are derived here:
     * tonne = kg/1000, lb = kg/0.453592, m3 = kg/700 (HYPOTHESIS density),
     * EUR = FCFA/655.957 (fixed peg), USD = FCFA/600 (HYPOTHESIS rate).
     * The value of one batch is its current quantity multiplied by its
     * value per kg; batches without a value contribute zero.
     *
     * @return the totals in kg, tonne, lb, m3, XOF, EUR and USD
     */
    public ValuationSummary summary() {
        BigDecimal totalKg = BigDecimal.ZERO;
        BigDecimal totalValueFcfa = BigDecimal.ZERO;
        for (CropStorageBatch b : batchRepository.findAll()) {
            BigDecimal qty = b.getCurrentQuantityKg() != null ? b.getCurrentQuantityKg() : BigDecimal.ZERO;
            totalKg = totalKg.add(qty);
            if (b.getValuePerKg() != null) {
                totalValueFcfa = totalValueFcfa.add(qty.multiply(b.getValuePerKg().amount()));
            }
        }
        return new ValuationSummary(
                totalKg,
                totalKg.divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP),
                totalKg.divide(KG_PER_LB, 3, RoundingMode.HALF_UP),
                totalKg.divide(KG_PER_M3, 3, RoundingMode.HALF_UP),
                totalValueFcfa,
                totalValueFcfa.divide(XOF_PER_EUR, 2, RoundingMode.HALF_UP),
                totalValueFcfa.divide(XOF_PER_USD, 2, RoundingMode.HALF_UP));
    }

    /**
     * Batches with a known expiry date, soonest first; this is the expiry
     * view that will feed Sales and Marketing.
     *
     * @return batches whose expiryDate is set, ordered by expiry date asc
     */
    public List<CropStorageBatch> expiring() {
        return batchRepository.findByExpiryDateNotNullOrderByExpiryDateAsc();
    }
}