package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository.LastPurchase;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class FertilizerService {

    /** {@code source} of every fertilizer written through the API. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    private final FertilizerProductRepository fertilizerProductRepository;
    private final FertilizerMovementRepository fertilizerMovementRepository;

    /**
     * A code as the enums spell it: trimmed and upper-cased, so {@code " kg "}
     * becomes {@code "KG"}. The request validation already refused any other value.
     */
    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * An optional text as stored: trimmed, and {@code null} when it is blank.
     *
     * @param value the text as received, possibly {@code null}
     * @return the trimmed text, or {@code null} if it was null or blank
     */
    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Maps a stored fertilizer, its stock and its last purchase to its API
     * representation. The exact decimals of the database are sent as JSON numbers.
     *
     * @param product          the stored fertilizer
     * @param stock            its current stock, 0 when it has no movement
     * @param lastPurchaseDate date of its most recent purchase, or {@code null} when it was never bought
     * @return the API representation of that fertilizer
     */
    private static FertilizerResponse toResponse(FertilizerProduct product, BigDecimal stock,
                                                 LocalDate lastPurchaseDate) {
        BigDecimal threshold = product.getReorderThreshold();
        return new FertilizerResponse(
                product.getId(),
                product.getName(),
                product.getFertilizerType(),
                product.getComposition(),
                product.getUnit(),
                threshold == null ? null : threshold.doubleValue(),
                stock.doubleValue(),
                FertilizerStockCalculator.belowThreshold(stock, threshold),
                lastPurchaseDate,
                product.getSource(),
                product.getLastUpdated());
    }

    /**
     * Date of the most recent purchase of each given fertilizer, from one query.
     *
     * @param productIds identifiers of the fertilizers, not empty
     * @return the date by fertilizer identifier; a fertilizer never bought is absent
     */
    private Map<Long, LocalDate> lastPurchaseDates(List<Long> productIds) {
        return fertilizerMovementRepository.findLastPurchaseDates(productIds).stream()
                .collect(Collectors.toMap(LastPurchase::getProductId, LastPurchase::getLastDate));
    }

    /**
     * Adds a fertilizer to the catalogue, with no stock.
     *
     * <p>A name already in the catalogue is refused, ignoring case, accents and
     * surrounding spaces: "Urée" and "uree" are the same product. The names are
     * compared in Java, as for the agronomic reference, because accent folding
     * in SQL would depend on the database.
     *
     * @param request the fertilizer, already validated
     * @return the created fertilizer, with a stock of 0 and no purchase
     * @throws ConflictException if a fertilizer with the same name exists
     */
    @Transactional
    public FertilizerResponse createFertilizer(FertilizerRequest request) {
        return createFertilizer(request, Instant.now());
    }

    /**
     * Same as {@link #createFertilizer(FertilizerRequest)}, at an explicit write
     * time so the {@code lastUpdated} value can be tested.
     */
    FertilizerResponse createFertilizer(FertilizerRequest request, Instant now) {
        String name = request.name().trim();
        String key = VarietyReferenceMatcher.matchKey(name);
        fertilizerProductRepository.findAll().stream()
                .filter(existing -> VarietyReferenceMatcher.matchKey(existing.getName()).equals(key))
                .min(Comparator.comparing(FertilizerProduct::getId))
                .ifPresent(existing -> {
                    throw new ConflictException("A fertilizer named " + existing.getName() + " already exists");
                });

        FertilizerProduct product = new FertilizerProduct();
        product.setName(name);
        product.setFertilizerType(FertilizerType.valueOf(normalizeCode(request.fertilizerType())));
        product.setComposition(normalizeOptionalText(request.composition()));
        product.setUnit(FertilizerUnit.valueOf(normalizeCode(request.unit())));
        product.setReorderThreshold(request.reorderThreshold() == null
                ? null
                : BigDecimal.valueOf(request.reorderThreshold()));
        product.setSource(USER_ENTRY_SOURCE);
        product.setLastUpdated(now);
        return toResponse(fertilizerProductRepository.save(product), BigDecimal.ZERO, null);
    }

    /**
     * Retrieves the whole catalogue, each fertilizer with its current stock and
     * the date of its last purchase. The stocks of every fertilizer come from
     * one query, and the purchase dates from another.
     *
     * @return the fertilizers ordered by name, possibly empty
     */
    public List<FertilizerResponse> getAllFertilizers() {
        List<FertilizerProduct> products = fertilizerProductRepository.findAllByOrderByNameAsc();
        if (products.isEmpty()) {
            return List.of();
        }
        List<Long> ids = products.stream().map(FertilizerProduct::getId).toList();
        Map<Long, BigDecimal> stocks = FertilizerStockCalculator.stocksByProduct(
                fertilizerMovementRepository.findMovementTotals(ids));
        Map<Long, LocalDate> lastPurchaseDates = lastPurchaseDates(ids);
        return products.stream()
                .map(product -> toResponse(product, stocks.getOrDefault(product.getId(), BigDecimal.ZERO),
                        lastPurchaseDates.get(product.getId())))
                .toList();
    }

    /**
     * Retrieves one fertilizer with its current stock and the date of its last purchase.
     *
     * @param id the fertilizer identifier
     * @return the fertilizer
     * @throws FertilizerNotFoundException if no fertilizer has this identifier
     */
    public FertilizerResponse getFertilizerById(Long id) {
        FertilizerProduct product = fertilizerProductRepository.findById(id)
                .orElseThrow(() -> new FertilizerNotFoundException(id));
        BigDecimal stock = FertilizerStockCalculator.stockOf(id,
                fertilizerMovementRepository.findMovementTotals(List.of(id)));
        return toResponse(product, stock, lastPurchaseDates(List.of(id)).get(id));
    }
}
