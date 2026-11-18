package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovement;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository.LastPurchase;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository.MovementTotal;
import com.infineonbit.sustainablefarm.modules.plants.support.TestBlocks;
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

    /** An application on the block of a code, by its stable test id ({@link TestBlocks#idOf}). */
    private FertilizerMovement application(FertilizerProduct product, LocalDate date, String blockCode) {
        FertilizerMovement application = new FertilizerMovement();
        application.setProduct(product);
        application.setMovementType(FertilizerMovementType.APPLICATION);
        application.setMovementDate(date);
        application.setQuantity(new BigDecimal("10"));
        application.setBlockId(TestBlocks.idOf(blockCode));
        application.setApplicator("Team A");
        application.setSource("user_entry");
        return fertilizerMovementRepository.save(application);
    }

    /** @param blockIds the blocks to keep, or {@code null} for every block */
    private List<Long> find(Long fertilizerId, FertilizerMovementType type, List<Long> blockIds,
                            LocalDate from, LocalDate to) {
        return fertilizerMovementRepository.findByOptionalFilters(fertilizerId, type, blockIds == null,
                        blockIds == null ? List.of(-1L) : blockIds, from, to)
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

    /** The last purchase dates, by fertilizer identifier. */
    private Map<Long, LocalDate> lastPurchaseDates(Long... productIds) {
        return fertilizerMovementRepository.findLastPurchaseDates(List.of(productIds)).stream()
                .collect(Collectors.toMap(LastPurchase::getProductId, LastPurchase::getLastDate));
    }

    @Test
    void findLastPurchaseDates_shouldGiveTheLatestPurchaseDateOfEachFertilizer() {
        // Arrange: the second NPK purchase is saved last but dated before the first one
        FertilizerProduct npk = product("NPK 15-15-15");
        FertilizerProduct urea = product("Urea");
        movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2027, 4, 12), "100");
        movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2027, 3, 1), "50");
        movement(urea, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "12.5");
        // Act & Assert
        assertEquals(Map.of(npk.getId(), LocalDate.of(2027, 4, 12), urea.getId(), LocalDate.of(2026, 6, 1)),
                lastPurchaseDates(npk.getId(), urea.getId()));
    }

    @Test
    void findLastPurchaseDates_shouldCountPurchasesOnly() {
        // Arrange: every other type dated after the purchase
        FertilizerProduct npk = product("NPK 15-15-15");
        movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2027, 4, 12), "300");
        application(npk, LocalDate.of(2027, 5, 2), "A");
        movement(npk, FertilizerMovementType.LOSS, LocalDate.of(2027, 5, 3), "10");
        movement(npk, FertilizerMovementType.ADJUSTMENT_IN, LocalDate.of(2027, 5, 4), "5");
        movement(npk, FertilizerMovementType.ADJUSTMENT_OUT, LocalDate.of(2027, 5, 5), "5");
        // Act & Assert
        assertEquals(Map.of(npk.getId(), LocalDate.of(2027, 4, 12)), lastPurchaseDates(npk.getId()));
    }

    @Test
    void findLastPurchaseDates_shouldLeaveOutFertilizersNeverBoughtOrNotAskedFor() {
        // Arrange: compost only applied, urea bought but not asked for
        FertilizerProduct npk = product("NPK 15-15-15");
        FertilizerProduct compost = product("Compost");
        FertilizerProduct urea = product("Urea");
        application(compost, LocalDate.of(2027, 5, 2), "A");
        movement(urea, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "50");
        // Act & Assert
        assertTrue(lastPurchaseDates(npk.getId(), compost.getId()).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldReturnEveryMovementByDateThenId_whenNoFilterIsGiven() {
        // Arrange: saved out of date order, two on the same day
        FertilizerProduct npk = product("NPK 15-15-15");
        FertilizerMovement loss = movement(npk, FertilizerMovementType.LOSS, LocalDate.of(2026, 7, 1), "10");
        FertilizerMovement purchase = movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "200");
        FertilizerMovement secondPurchase =
                movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "100");
        FertilizerMovement application = application(npk, LocalDate.of(2026, 6, 15), "B");
        // Act & Assert
        assertEquals(List.of(purchase.getId(), secondPurchase.getId(), application.getId(), loss.getId()),
                find(null, null, null, null, null));
    }

    @Test
    void findByOptionalFilters_shouldFilterOnTheFertilizerAndTheType() {
        // Arrange
        FertilizerProduct npk = product("NPK 15-15-15");
        FertilizerProduct compost = product("Compost");
        FertilizerMovement npkPurchase = movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "200");
        FertilizerMovement npkApplication = application(npk, LocalDate.of(2026, 6, 15), "B");
        FertilizerMovement compostPurchase =
                movement(compost, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 2), "40");
        // Act & Assert
        assertEquals(List.of(npkPurchase.getId(), npkApplication.getId()), find(npk.getId(), null, null, null, null));
        assertEquals(List.of(npkPurchase.getId(), compostPurchase.getId()),
                find(null, FertilizerMovementType.PURCHASE, null, null, null));
        assertEquals(List.of(npkApplication.getId()),
                find(npk.getId(), FertilizerMovementType.APPLICATION, null, null, null));
        assertTrue(find(compost.getId(), FertilizerMovementType.LOSS, null, null, null).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldFilterOnTheBlocksOfTheApplications() {
        // Arrange: a purchase has no block
        FertilizerProduct npk = product("NPK 15-15-15");
        movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "200");
        FertilizerMovement firstB = application(npk, LocalDate.of(2026, 6, 15), "B");
        FertilizerMovement secondB = application(npk, LocalDate.of(2026, 6, 16), "B");
        FertilizerMovement onA = application(npk, LocalDate.of(2026, 6, 17), "A");
        // Act & Assert: the blocks of a farm, one block, and a block filter leaves every purchase out
        assertEquals(List.of(firstB.getId(), secondB.getId(), onA.getId()),
                find(null, null, List.of(TestBlocks.idOf("A"), TestBlocks.idOf("B")), null, null));
        assertEquals(List.of(firstB.getId(), secondB.getId()), find(null, null, List.of(TestBlocks.idOf("B")), null, null));
        assertTrue(find(null, null, List.of(-1L), null, null).isEmpty());
        assertTrue(find(null, null, List.of(TestBlocks.idOf("C")), null, null).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldIncludeBothDates() {
        // Arrange
        FertilizerProduct npk = product("NPK 15-15-15");
        FertilizerMovement june1 = movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "200");
        FertilizerMovement june5 = movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 5), "100");
        FertilizerMovement june15 = application(npk, LocalDate.of(2026, 6, 15), "B");
        FertilizerMovement july1 = movement(npk, FertilizerMovementType.LOSS, LocalDate.of(2026, 7, 1), "10");
        // Act & Assert
        assertEquals(List.of(june5.getId(), june15.getId()),
                find(null, null, null, LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 30)));
        assertEquals(List.of(june5.getId(), june15.getId()),
                find(null, null, null, LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 15)));
        assertEquals(List.of(june15.getId(), july1.getId()),
                find(null, null, null, LocalDate.of(2026, 6, 15), null));
        assertEquals(List.of(june1.getId()),
                find(null, null, null, null, LocalDate.of(2026, 6, 1)));
    }

    @Test
    void findByOptionalFilters_shouldMatchNothing_whenFromIsAfterTo() {
        // Arrange
        FertilizerProduct npk = product("NPK 15-15-15");
        movement(npk, FertilizerMovementType.PURCHASE, LocalDate.of(2026, 6, 1), "200");
        // Act & Assert
        assertTrue(find(null, null, null, LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 2)).isEmpty());
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
