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

@Service
@RequiredArgsConstructor
public class BatchService {

    // Fixed CFA franc peg: 1 EUR = 655.957 XOF
    private static final BigDecimal XOF_PER_EUR = new BigDecimal("655.957");

    // HYPOTHESIS: indicative USD rate, to be confirmed with finance
    private static final BigDecimal XOF_PER_USD = new BigDecimal("600");

    // HYPOTHESIS: bulk density of packaged dried mango, kg per m3
    private static final BigDecimal KG_PER_M3 = new BigDecimal("700");

    // 1 lb = 0.453592 kg
    private static final BigDecimal KG_PER_LB = new BigDecimal("0.453592");

    private final BatchRepository batchRepository;

    public Batch create(Batch batch) {
        return batchRepository.save(batch);
    }

    public List<Batch> findAll() {
        return batchRepository.findAll();
    }

    // FIFO with priority to important customers
    public List<Batch> fifo() {
        return batchRepository
                .findByCurrentQuantityKgGreaterThanOrderByCustomerPriorityDescStorageEntryDateAsc(BigDecimal.ZERO);
    }

    // International reporting: mass in kg/tonne/lb/m3, value in FCFA/EUR/USD
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

    // Inventory + shelf life info for Sales and Marketing
    public List<Batch> expiring() {
        return batchRepository.findByExpiryDateNotNullOrderByExpiryDateAsc();
    }
}