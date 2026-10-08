package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ApplicationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.LossRequest;
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
import java.util.List;
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
     * Block code as stored: trimmed and upper-cased, so {@code " b "} becomes
     * {@code "B"}, as for a planting.
     */
    private static String normalizeBlockCode(String blockCode) {
        return blockCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * An optional text as stored: trimmed, and {@code null} when it is blank.
     * Also turns a blank list filter into no filter, as for the other lists of
     * the module.
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
     * Maps a stored movement to its API representation. The exact decimals of
     * the database are sent as JSON numbers, and a cost is given in both
     * currencies, or only in its own one while no rate is recorded.
     *
     * @param movement the stored movement
     * @param eurToXof how many FCFA one euro is worth, or {@code null} when the
     *                 movement has no cost or no rate is recorded
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
            BigDecimal eur = CurrencyConverter.toEur(totalCost, currency, eurToXof);
            totalCostEur = eur == null ? null : eur.doubleValue();
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
     * A quantity as received, as an exact decimal. It has at most 3 decimals,
     * checked by the request, so its decimal form is exact: 0.1 stays 0.1.
     */
    private static BigDecimal toQuantity(Double quantity) {
        return BigDecimal.valueOf(quantity);
    }

    /** A new movement of a fertilizer, with the columns shared by every type. */
    private static FertilizerMovement newMovement(FertilizerProduct product, FertilizerMovementType type,
                                                  LocalDate date, BigDecimal quantity, Instant now) {
        FertilizerMovement movement = new FertilizerMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setMovementDate(date);
        movement.setQuantity(quantity);
        movement.setSource(USER_ENTRY_SOURCE);
        movement.setLastUpdated(now);
        return movement;
    }

    private FertilizerProduct findProduct(Long fertilizerId) {
        return fertilizerProductRepository.findById(fertilizerId)
                .orElseThrow(() -> new FertilizerNotFoundException(fertilizerId));
    }

    /**
     * Refuses a movement that would take more than the current stock.
     *
     * <p>The check uses the stock today, not the stock on the date of the
     * movement: a late entry dated before a purchase is accepted as long as the
     * stock covers it now.
     *
     * @param product  the fertilizer
     * @param quantity the quantity to take from its stock
     * @throws BusinessRuleException if the quantity is larger than the stock
     */
    private void requireStock(FertilizerProduct product, BigDecimal quantity) {
        BigDecimal stock = FertilizerStockCalculator.stockOf(product.getId(),
                fertilizerMovementRepository.findMovementTotals(List.of(product.getId())));
        if (quantity.compareTo(stock) > 0) {
            throw new BusinessRuleException(FertilizerStockCalculator.notEnoughStockMessage(
                    product.getName(), stock, quantity, product.getUnit()));
        }
    }

    /**
     * How many FCFA one euro is worth, read from {@code currency_rate} on every
     * call and never cached, so a rate entered or replaced counts at once.
     *
     * <p>Outside the dev profile no rate exists until the user enters one, so a
     * missing rate is not an error: each cost is then given only in its own
     * currency, and a warning says how to enter the rate.
     *
     * @return the EUR to XOF rate, or {@code null} when none is recorded yet
     */
    private BigDecimal eurToXofRate() {
        CurrencyRate rate = currencyRateRepository
                .findByBaseCurrencyAndQuoteCurrency(CurrencyCode.EUR.name(), CurrencyCode.XOF.name())
                .orElse(null);
        if (rate == null) {
            log.warn("No EUR to XOF rate recorded yet: each cost is given only in its own currency. "
                    + "Enter the rate with PUT /api/plants/currency-rates/EUR/XOF.");
            return null;
        }
        return rate.getRate();
    }

    /**
     * Records a purchase of a fertilizer, which adds to its stock.
     *
     * <p>A cost without a currency is in XOF, the currency of the farm; a
     * currency without a cost is not stored. The response gives the cost in
     * both currencies, with the rate stored in {@code currency_rate}. While no
     * rate is recorded, the purchase is stored all the same and its cost is
     * given only in its own currency.
     *
     * @param fertilizerId the fertilizer bought
     * @param request      the purchase, already validated
     * @return the recorded purchase
     * @throws FertilizerNotFoundException if no fertilizer has this identifier
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
                request.purchaseDate(), toQuantity(request.quantity()), now);
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

    /**
     * Records an application of a fertilizer on a block, which takes from its stock.
     *
     * <p>The block needs no recorded planting: the base fertilizer goes into the
     * hole before or while the tree is planted.
     *
     * @param fertilizerId the fertilizer applied
     * @param request      the application, already validated
     * @return the recorded application
     * @throws FertilizerNotFoundException if no fertilizer has this identifier
     * @throws BusinessRuleException       if the quantity is larger than the current stock
     */
    @Transactional
    public FertilizerMovementResponse recordApplication(Long fertilizerId, ApplicationRequest request) {
        return recordApplication(fertilizerId, request, Instant.now());
    }

    /**
     * Same as {@link #recordApplication(Long, ApplicationRequest)}, at an
     * explicit write time so the {@code lastUpdated} value can be tested.
     */
    FertilizerMovementResponse recordApplication(Long fertilizerId, ApplicationRequest request, Instant now) {
        FertilizerProduct product = findProduct(fertilizerId);
        BigDecimal quantity = toQuantity(request.quantity());
        requireStock(product, quantity);

        FertilizerMovement application = newMovement(product, FertilizerMovementType.APPLICATION,
                request.applicationDate(), quantity, now);
        application.setFarmId(request.farmId());
        application.setBlockCode(normalizeBlockCode(request.blockCode()));
        application.setApplicator(request.applicator().trim());
        application.setMethod(normalizeOptionalText(request.method()));
        return toResponse(fertilizerMovementRepository.save(application), null);
    }

    /**
     * Records a loss of a fertilizer, which takes from its stock.
     *
     * @param fertilizerId the fertilizer lost
     * @param request      the loss, already validated
     * @return the recorded loss
     * @throws FertilizerNotFoundException if no fertilizer has this identifier
     * @throws BusinessRuleException       if the quantity is larger than the current stock
     */
    @Transactional
    public FertilizerMovementResponse recordLoss(Long fertilizerId, LossRequest request) {
        return recordLoss(fertilizerId, request, Instant.now());
    }

    /**
     * Same as {@link #recordLoss(Long, LossRequest)}, at an explicit write time
     * so the {@code lastUpdated} value can be tested.
     */
    FertilizerMovementResponse recordLoss(Long fertilizerId, LossRequest request, Instant now) {
        FertilizerProduct product = findProduct(fertilizerId);
        BigDecimal quantity = toQuantity(request.quantity());
        requireStock(product, quantity);

        FertilizerMovement loss = newMovement(product, FertilizerMovementType.LOSS, request.lossDate(), quantity, now);
        loss.setReason(request.reason().trim());
        return toResponse(fertilizerMovementRepository.save(loss), null);
    }

    /**
     * Retrieves the movements matching the optional filters.
     *
     * <p>Every filter is independent and optional; a missing farm means every
     * farm, as for the other lists of the module. Both dates are included. A
     * filter that matches nothing, or a {@code from} after {@code to}, returns
     * an empty list and is never an error. The rate is read once, and only when
     * a listed movement has a cost; while no rate is recorded, each cost is
     * given only in its own currency.
     *
     * @param fertilizerId fertilizer identifier, or {@code null} for every fertilizer
     * @param movementType movement type, or {@code null} for every type
     * @param farmId       farm identifier, or {@code null} for every farm
     * @param blockCode    raw block value as stored (for example {@code "B"}),
     *                     or {@code null} for every block
     * @param from         first movement date, included, or {@code null}
     * @param to           last movement date, included, or {@code null}
     * @return the matching movements, ordered by date then identifier
     */
    public List<FertilizerMovementResponse> getAllMovements(Long fertilizerId, FertilizerMovementType movementType,
                                                            Integer farmId, String blockCode,
                                                            LocalDate from, LocalDate to) {
        List<FertilizerMovement> movements = fertilizerMovementRepository.findByOptionalFilters(
                fertilizerId, movementType, farmId, normalizeOptionalText(blockCode), from, to);
        BigDecimal eurToXof = movements.stream().anyMatch(movement -> movement.getTotalCost() != null)
                ? eurToXofRate()
                : null;
        return movements.stream()
                .map(movement -> toResponse(movement, eurToXof))
                .toList();
    }
}
