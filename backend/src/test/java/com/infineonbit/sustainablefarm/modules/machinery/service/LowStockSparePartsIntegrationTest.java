package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.SparePartObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.entity.SparePart;
import com.infineonbit.sustainablefarm.modules.machinery.repository.SparePartRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(SparePartService.class)
class LowStockSparePartsIntegrationTest {

    @Autowired
    private SparePartService sparePartService;

    @Autowired
    private SparePartRepository sparePartRepository;

    @Test
    void returnsOnlySparePartsAtOrBelowTheirThreshold() {
        saveSparePart("Below threshold", 2, 5);
        saveSparePart("At threshold", 5, 5);
        saveSparePart("Above threshold", 10, 5);

        List<SparePartObtainingResponse> result = sparePartService.obtainLowStockSpareParts();

        assertEquals(2, result.size());
        assertEquals("Below threshold", result.get(0).name());
        assertEquals("At threshold", result.get(1).name());
    }

    @Test
    void returnsEmptyList_whenNoSparePartIsLowOnStock() {
        saveSparePart("Healthy part", 30, 10);

        List<SparePartObtainingResponse> result = sparePartService.obtainLowStockSpareParts();

        assertEquals(0, result.size());
    }

    private void saveSparePart(String name, int quantity, int reorderThreshold) {
        SparePart sparePart = new SparePart();
        sparePart.setName(name);
        sparePart.setQuantity(quantity);
        sparePart.setReorderThreshold(reorderThreshold);
        sparePart.setUnitCost(new BigDecimal("1.00"));
        sparePartRepository.save(sparePart);
    }
}
