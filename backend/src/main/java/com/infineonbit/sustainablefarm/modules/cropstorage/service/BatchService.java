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

    // HYPOTHESIS: bulk density of packaged dried mango, kg per m3
    private static final BigDecimal KG_PER_M3 = new BigDecimal("700");

    private final BatchRepository batchRepository;

    public Batch create(Batch batch) {
        return batchRepository.save(batch);
    }

    public List<Batch> findAll() {
        return batchRepository.findAll();
    }

    // Anar: FIFO with priority to important customers
    public List<Batch> fifo() {
        return batchRepository
                .findByCurrentQuantityKgGreaterThanOrderByCustomerPriorityDescStorageEntryDateAsc(BigDecimal.ZERO);
    }

    // Anar: units kg / tonnes / m3 + monetary value for financial reporting
    public Map<String, Object> summary() {
        BigDecimal totalKg = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        for (Batch b : batchRepository.findAll()) {
            BigDecimal qty = b.getCurrentQuantityKg() != null ? b.getCurrentQuantityKg() : BigDecimal.ZERO;
            totalKg = totalKg.add(qty);
            if (b.getValuePerKgFcfa() != null) {
                totalValue = totalValue.add(qty.multiply(b.getValuePerKgFcfa()));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalKg", totalKg);
        result.put("totalTonnes", totalKg.divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
        result.put("totalM3", totalKg.divide(KG_PER_M3, 3, RoundingMode.HALF_UP));
        result.put("totalValueFcfa", totalValue);
        return result;
    }

    // Anar: inventory + shelf life info for Sales and Marketing
    public List<Batch> expiring() {
        return batchRepository.findByExpiryDateNotNullOrderByExpiryDateAsc();
    }
}