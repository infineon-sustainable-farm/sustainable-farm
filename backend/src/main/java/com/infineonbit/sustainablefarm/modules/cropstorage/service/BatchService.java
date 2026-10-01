package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core inventory service of Crop Storage: batch persistence, FIFO rotation
 * with customer priority, multi-unit inventory valuation (kg, tonne, lb, m3,
 * FCFA, EUR, USD) and expiry listing for Sales and Marketing.
 */
@Service
@RequiredArgsConstructor
public class BatchService {

    /** Fixed CFA franc peg: 1 EUR = 655.957 XOF. */
    private static final BigDecimal XOF_PER_EUR = new BigDecimal("655.957");

    /** HYPOTHESIS: indicative USD rate, to be confirmed with finance. */
    private static final BigDecimal XOF_PER_USD = new BigDecimal("600");

    /** HYPOTHESIS: bulk density of packaged dried mango, kg per m3. */
    private static final BigDecimal KG_PER_M3 = new BigDecimal("700");

    /** 1 lb = 0.453592 kg. */
    private static final BigDecimal KG_PER_LB = new BigDecimal("0.453592");

    private final BatchRepository batchRepository;

    /**
     * Saves a new batch exactly as received; the batch code follows the team
     * convention B26-01 (block letter + 2-digit year + sequence) and is unique.
     *
     * @param batch batch parsed from the request body
     * @return the saved batch with its generated id
     */
    public Batch create(Batch batch) {
        return batchRepository.save(batch);
    }

    /**
     * Returns every batch, whatever its stage or status.
     *
     * @return all batches of the module
     */
    public List<Batch> findAll() {
        return batchRepository.findAll();
    }

    /**
     * Stock rotation queue agreed with Anar: first in, first out, with higher
     * customer priority served first; empty batches are excluded.
     *
     * @return batches with remaining stock, ordered by customerPriority desc
     *         then storageEntryDate asc
     */
    public List<Batch> fifo() {
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
     * @return ordered map with totalKg, totalTonnes, totalLb, totalM3,
     *         totalValueFcfa, totalValueEur, totalValueUsd
     */
    public Map<String, Object> summary() {
        BigDecimal totalKg = BigDecimal.ZERO;
        BigDecimal totalValueFcfa = BigDecimal.ZERO;
        for (Batch b : batchRepository.findAll()) {
            BigDecimal qty = b.getCurrentQuantityKg() != null ? b.getCurrentQuantityKg() : BigDecimal.ZERO;
            totalKg = totalKg.add(qty);
            if (b.getValuePerKgFcfa() != null) {
                totalValueFcfa = totalValueFcfa.add(qty.multiply(b.getValuePerKgFcfa()));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalKg", totalKg);
        result.put("totalTonnes", totalKg.divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
        result.put("totalLb", totalKg.divide(KG_PER_LB, 3, RoundingMode.HALF_UP));
        result.put("totalM3", totalKg.divide(KG_PER_M3, 3, RoundingMode.HALF_UP));
        result.put("totalValueFcfa", totalValueFcfa);
        result.put("totalValueEur", totalValueFcfa.divide(XOF_PER_EUR, 2, RoundingMode.HALF_UP));
        result.put("totalValueUsd", totalValueFcfa.divide(XOF_PER_USD, 2, RoundingMode.HALF_UP));
        return result;
    }

    /**
     * Batches with a known expiry date, soonest first; this is the expiry
     * view that will feed Sales and Marketing.
     *
     * @return batches whose expiryDate is set, ordered by expiry date asc
     */
    public List<Batch> expiring() {
        return batchRepository.findByExpiryDateNotNullOrderByExpiryDateAsc();
    }
}