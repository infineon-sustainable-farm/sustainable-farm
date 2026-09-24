package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovement;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository.MovementTotal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
public class FertilizerMovementRepositoryTest {

    @Autowired
    private FertilizerProductRepository fertilizerProductRepository;

    @Autowired
    private FertilizerMovementRepository fertilizerMovementRepository;

    private FertilizerProduct product(String name) {
        FertilizerProduct product = new FertilizerProduct();
        product.setName(name);
        product.setFertilizerType(FertilizerType.MINERAL);
        product.setUnit(FertilizerUnit.KG);
        product.setSource("user_entry");
        return fertilizerProductRepository.save(product);
    }

    private FertilizerMovement movement(FertilizerProduct product, FertilizerMovementType type, LocalDate date,
                                        String quantity) {
        FertilizerMovement movement = new FertilizerMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setMovementDate(date);
        movement.setQuantity(new BigDecimal(quantity));
        movement.setSource("user_entry");
        return fertilizerMovementRepository.save(movement);
    }

    /** The totals of one fertilizer, by movement type. */
    private static Map<FertilizerMovementType, BigDecimal> totalsOf(Long productId, List<MovementTotal> totals) {
        return totals.stream()
                .filter(total -> total.getProductId().equals(productId))
                .collect(Collectors.toMap(MovementTotal::getMovementType, MovementTotal::getTotal));
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> expected + " expected, got " + actual);
    }

    @Test
    void findMovementTotals_shouldSumEachTypeOfEachFertilizer() {
        // Arrange
        FertilizerProduct npk = product("NPK 15-15-15");
        FertilizerProduct urea = product("Urea");
        movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "200");
        movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 5), "100");
        movement(npk, FertilizerMovementType.APPLICATION, LocalDate.of(2026, 6, 15), "250");
        movement(npk, FertilizerMovementType.LOSS, LocalDate.of(2026, 7, 1), "10");
        movement(urea, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "12.5");
        // Act
        List<MovementTotal> totals = fertilizerMovementRepository.findMovementTotals(List.of(npk.getId(), urea.getId()));
        // Assert: one line per fertilizer and type
        assertEquals(4, totals.size());
        Map<FertilizerMovementType, BigDecimal> npkTotals = totalsOf(npk.getId(), totals);
        assertDecimal("300", npkTotals.get(FertilizerMovementType.PURCHASE));
        assertDecimal("250", npkTotals.get(FertilizerMovementType.APPLICATION));
        assertDecimal("10", npkTotals.get(FertilizerMovementType.LOSS));
        assertDecimal("12.5", totalsOf(urea.getId(), totals).get(FertilizerMovementType.PURCHASE));
    }

    @Test
    void findMovementTotals_shouldKeepTheDecimalsExact() {
        // Arrange: 0.1 + 0.2 as doubles is 0.30000000000000004
        FertilizerProduct npk = product("NPK 15-15-15");
        movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "0.3");
        movement(npk, FertilizerMovementType.APPLICATION, LocalDate.of(2026, 6, 2), "0.1");
        movement(npk, FertilizerMovementType.APPLICATION, LocalDate.of(2026, 6, 3), "0.2");
        // Act
        Map<FertilizerMovementType, BigDecimal> totals =
                totalsOf(npk.getId(), fertilizerMovementRepository.findMovementTotals(List.of(npk.getId())));
        // Assert
        assertDecimal("0.3", totals.get(FertilizerMovementType.PURCHASE));
        assertDecimal("0.3", totals.get(FertilizerMovementType.APPLICATION));
    }

    @Test
    void findMovementTotals_shouldLeaveOutFertilizersWithoutMovementOrNotAskedFor() {
        // Arrange
        FertilizerProduct npk = product("NPK 15-15-15");
        FertilizerProduct compost = product("Compost");
        FertilizerProduct urea = product("Urea");
        movement(urea, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "50");
        // Act & Assert
        assertTrue(fertilizerMovementRepository.findMovementTotals(List.of(npk.getId(), compost.getId())).isEmpty());
    }

    @Test
    void findAllByOrderByNameAsc_shouldListTheCatalogueByName() {
        // Arrange
        product("Urea");
        product("Compost");
        product("NPK 15-15-15");
        // Act & Assert
        assertEquals(List.of("Compost", "NPK 15-15-15", "Urea"),
                fertilizerProductRepository.findAllByOrderByNameAsc().stream().map(FertilizerProduct::getName).toList());
    }
}
