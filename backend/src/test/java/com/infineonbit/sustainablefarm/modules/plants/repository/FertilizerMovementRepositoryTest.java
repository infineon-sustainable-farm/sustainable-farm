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

    private FertilizerMovement application(FertilizerProduct product, LocalDate date, Integer farmId,
                                           String blockCode) {
        FertilizerMovement application = new FertilizerMovement();
        application.setProduct(product);
        application.setMovementType(FertilizerMovementType.APPLICATION);
        application.setMovementDate(date);
        application.setQuantity(new BigDecimal("10"));
        application.setFarmId(farmId);
        application.setBlockCode(blockCode);
        application.setApplicator("Team A");
        application.setSource("user_entry");
        return fertilizerMovementRepository.save(application);
    }

    private List<Long> find(Long fertilizerId, FertilizerMovementType type, Integer farmId, String blockCode,
                            LocalDate from, LocalDate to) {
        return fertilizerMovementRepository.findByOptionalFilters(fertilizerId, type, farmId, blockCode, from, to)
                .stream()
                .map(FertilizerMovement::getId)
                .toList();
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
    void findByOptionalFilters_shouldReturnEveryMovementByDateThenId_whenNoFilterIsGiven() {
        // Arrange: saved out of date order, two on the same day
        FertilizerProduct npk = product("NPK 15-15-15");
        FertilizerMovement loss = movement(npk, FertilizerMovementType.LOSS, LocalDate.of(2026, 7, 1), "10");
        FertilizerMovement purchase = movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "200");
        FertilizerMovement secondPurchase =
                movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "100");
        FertilizerMovement application = application(npk, LocalDate.of(2026, 6, 15), null, "B");
        // Act & Assert
        assertEquals(List.of(purchase.getId(), secondPurchase.getId(), application.getId(), loss.getId()),
                find(null, null, null, null, null, null));
    }

    @Test
    void findByOptionalFilters_shouldFilterOnTheFertilizerAndTheType() {
        // Arrange
        FertilizerProduct npk = product("NPK 15-15-15");
        FertilizerProduct compost = product("Compost");
        FertilizerMovement npkPurchase = movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "200");
        FertilizerMovement npkApplication = application(npk, LocalDate.of(2026, 6, 15), null, "B");
        FertilizerMovement compostPurchase =
                movement(compost, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 2), "40");
        // Act & Assert
        assertEquals(List.of(npkPurchase.getId(), npkApplication.getId()), find(npk.getId(), null, null, null, null, null));
        assertEquals(List.of(npkPurchase.getId(), compostPurchase.getId()),
                find(null, FertilizerMovementType.PURCHASE, null, null, null, null));
        assertEquals(List.of(npkApplication.getId()),
                find(npk.getId(), FertilizerMovementType.APPLICATION, null, null, null, null));
        assertTrue(find(compost.getId(), FertilizerMovementType.LOSS, null, null, null, null).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldFilterOnTheFarmAndBlockOfTheApplications() {
        // Arrange: a purchase has no farm nor block
        FertilizerProduct npk = product("NPK 15-15-15");
        movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "200");
        FertilizerMovement noFarmB = application(npk, LocalDate.of(2026, 6, 15), null, "B");
        FertilizerMovement farm1B = application(npk, LocalDate.of(2026, 6, 16), 1, "B");
        FertilizerMovement farm1A = application(npk, LocalDate.of(2026, 6, 17), 1, "A");
        // Act & Assert: farm 1 excludes the rows without a farm, and a block filter every purchase
        assertEquals(List.of(farm1B.getId(), farm1A.getId()), find(null, null, 1, null, null, null));
        assertEquals(List.of(noFarmB.getId(), farm1B.getId()), find(null, null, null, "B", null, null));
        assertEquals(List.of(farm1A.getId()), find(null, null, 1, "A", null, null));
        assertTrue(find(null, null, 2, null, null, null).isEmpty());
        assertTrue(find(null, null, null, "C", null, null).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldIncludeBothDates() {
        // Arrange
        FertilizerProduct npk = product("NPK 15-15-15");
        FertilizerMovement june1 = movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "200");
        FertilizerMovement june5 = movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 5), "100");
        FertilizerMovement june15 = application(npk, LocalDate.of(2026, 6, 15), null, "B");
        FertilizerMovement july1 = movement(npk, FertilizerMovementType.LOSS, LocalDate.of(2026, 7, 1), "10");
        // Act & Assert
        assertEquals(List.of(june5.getId(), june15.getId()),
                find(null, null, null, null, LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 30)));
        assertEquals(List.of(june5.getId(), june15.getId()),
                find(null, null, null, null, LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 15)));
        assertEquals(List.of(june15.getId(), july1.getId()),
                find(null, null, null, null, LocalDate.of(2026, 6, 15), null));
        assertEquals(List.of(june1.getId()),
                find(null, null, null, null, null, LocalDate.of(2026, 6, 1)));
    }

    @Test
    void findByOptionalFilters_shouldMatchNothing_whenFromIsAfterTo() {
        // Arrange
        FertilizerProduct npk = product("NPK 15-15-15");
        movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "200");
        // Act & Assert
        assertTrue(find(null, null, null, null, LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 2)).isEmpty());
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

    @Test
    void findByIdForUpdate_shouldReturnTheFertilizer_orNothingForAnUnknownId() {
        // Arrange
        FertilizerProduct npk = product("NPK 15-15-15");
        // Act & Assert: the locked read runs inside the transaction of the test
        assertEquals("NPK 15-15-15", fertilizerProductRepository.findByIdForUpdate(npk.getId()).orElseThrow().getName());
        assertTrue(fertilizerProductRepository.findByIdForUpdate(npk.getId() + 1000).isEmpty());
    }
}
