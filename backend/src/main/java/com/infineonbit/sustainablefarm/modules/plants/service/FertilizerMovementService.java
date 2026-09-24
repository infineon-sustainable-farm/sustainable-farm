package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PurchaseRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerMovementResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovement;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerProductRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

@Service
@AllArgsConstructor
public class FertilizerMovementService {

    /** {@code source} of every movement written through the API. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    private static final Logger log = LoggerFactory.getLogger(FertilizerMovementService.class);

    private final FertilizerProductRepository fertilizerProductRepository;
    private final FertilizerMovementRepository fertilizerMovementRepository;
    private final CurrencyRateRepository currencyRateRepository;

    /**
     * A code as the enums spell it: trimmed and upper-cased, so {@code " eur "}
     * becomes {@code "EUR"}. The request validation already refused any other value.
     */
    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Maps a stored movement to its API representation. The exact decimals of
     * the database are sent as JSON numbers, and a cost is given in both
     * currencies.
     *
     * @param movement the stored movement
     * @param eurToXof how many FCFA one euro is worth, or {@code null} when the
     *                 movement has no cost
     * @return the API representation of that movement
     */
    private static FertilizerMovementResponse toResponse(FertilizerMovement movement, BigDecimal eurToXof) {
        FertilizerProduct product = movement.getProduct();
        BigDecimal totalCost = movement.getTotalCost();
        Long totalCostXof = null;
        Double totalCostEur = null;
        if (totalCost != null) {
            CurrencyCode currency = CurrencyCode.valueOf(movement.getCurrency());
            totalCostXof = CurrencyConverter.toXof(totalCost, currency, eurToXof);
            totalCostEur = CurrencyConverter.toEur(totalCost, currency, eurToXof).doubleValue();
        }
        return new FertilizerMovementResponse(
                movement.getId(),
                product.getId(),
                product.getName(),
                movement.getMovementType(),
                movement.getMovementDate(),
                movement.getQuantity().doubleValue(),
                product.getUnit(),
                movement.getFarmId(),
                movement.getBlockCode(),
                movement.getApplicator(),
                movement.getMethod(),
                movement.getSupplier(),
                totalCost == null ? null : totalCost.doubleValue(),
                movement.getCurrency(),
                totalCostXof,
                totalCostEur,
                movement.getReason(),
                movement.getSource(),
                movement.getLastUpdated());
    }

    /**
     * A new movement of a fertilizer, with the columns shared by every type.
     *
     * @param quantity the quantity as received; it has at most 3 decimals, so
     *                 its decimal form is exact
     */
    private static FertilizerMovement newMovement(FertilizerProduct product, FertilizerMovementType type,
                                                  LocalDate date, Double quantity, Instant now) {
        FertilizerMovement movement = new FertilizerMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setMovementDate(date);
        movement.setQuantity(BigDecimal.valueOf(quantity));
        movement.setSource(USER_ENTRY_SOURCE);
        movement.setLastUpdated(now);
        return movement;
    }

    private FertilizerProduct findProduct(Long fertilizerId) {
        return fertilizerProductRepository.findById(fertilizerId)
                .orElseThrow(() -> new FertilizerNotFoundException(fertilizerId));
    }

    /**
     * How many FCFA one euro is worth, read from {@code currency_rate} on every
     * call and never cached, so a rate changed in the database counts at once.
     *
     * @return the EUR to XOF rate
     * @throws IllegalStateException if the table has no EUR to XOF row, which
     *                               the loader inserts at every startup
     */
    private BigDecimal eurToXofRate() {
        return currencyRateRepository
                .findByBaseCurrencyAndQuoteCurrency(CurrencyCode.EUR.name(), CurrencyCode.XOF.name())
                .map(CurrencyRate::getRate)
                .orElseThrow(() -> {
                    log.error("No EUR to XOF rate in currency_rate; the costs cannot be converted. "
                            + "Restart the application to load the default rate, or insert it.");
                    return new IllegalStateException("No EUR to XOF rate in currency_rate");
                });
    }

    /**
     * Records a purchase of a fertilizer, which adds to its stock.
     *
     * <p>A cost without a currency is in XOF, the currency of the farm; a
     * currency without a cost is not stored. The response gives the cost in
     * both currencies, with the rate stored in {@code currency_rate}.
     *
     * @param fertilizerId the fertilizer bought
     * @param request      the purchase, already validated
     * @return the recorded purchase
     * @throws FertilizerNotFoundException if no fertilizer has this identifier
     * @throws IllegalStateException       if the purchase has a cost and no rate
     *                                     is stored
     */
    @Transactional
    public FertilizerMovementResponse recordPurchase(Long fertilizerId, PurchaseRequest request) {
        return recordPurchase(fertilizerId, request, Instant.now());
    }

    /**
     * Same as {@link #recordPurchase(Long, PurchaseRequest)}, at an explicit
     * write time so the {@code lastUpdated} value can be tested.
     */
    FertilizerMovementResponse recordPurchase(Long fertilizerId, PurchaseRequest request, Instant now) {
        FertilizerProduct product = findProduct(fertilizerId);
        FertilizerMovement purchase = newMovement(product, FertilizerMovementType.PURCHASE,
                request.purchaseDate(), request.quantity(), now);
        purchase.setSupplier(request.supplier().trim());

        BigDecimal eurToXof = null;
        if (request.totalCost() != null) {
            CurrencyCode currency = request.currency() == null
                    ? CurrencyCode.XOF
                    : CurrencyCode.valueOf(normalizeCode(request.currency()));
            purchase.setTotalCost(BigDecimal.valueOf(request.totalCost()));
            purchase.setCurrency(currency.name());
            eurToXof = eurToXofRate();
        }
        return toResponse(fertilizerMovementRepository.save(purchase), eurToXof);
    }
}
