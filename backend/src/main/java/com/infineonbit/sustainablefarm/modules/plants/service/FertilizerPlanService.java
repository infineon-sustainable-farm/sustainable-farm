package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ApplicationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerPlanCompletionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerPlanRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerMovementResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanComparisonResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanStockCheckResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovement;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlan;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlanStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerPlanNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerPlanRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerProductRepository;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerPlanCalculator.Amounts;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerPlanCalculator.ComparisonLine;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerPlanCalculator.LastPrice;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerPlanCalculator.StockNeed;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The applications of fertilizer the farmer plans ahead: planning one, marking
 * it as done or cancelled, comparing planned and applied quantities by block,
 * and checking that the stock covers the coming plans.
 *
 * <p>A plan takes nothing from the stock. Marking it as done records the real
 * application through {@link FertilizerMovementService}, with its own stock
 * check, and links it to the plan. No quantity is recommended anywhere: every
 * figure is the farmer's.
 */
@Service
public class FertilizerPlanService {

    /** {@code source} of every plan written through the API. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    /** Days the stock check looks ahead when none is asked: a default, which no source sets. */
    static final int DEFAULT_STOCK_CHECK_DAYS = 30;

    private final FertilizerPlanRepository fertilizerPlanRepository;
    private final FertilizerProductRepository fertilizerProductRepository;
    private final FertilizerMovementRepository fertilizerMovementRepository;
    private final CurrencyRateRepository currencyRateRepository;
    private final FertilizerMovementService fertilizerMovementService;
    private final Clock clock;

    public FertilizerPlanService(FertilizerPlanRepository fertilizerPlanRepository,
                                 FertilizerProductRepository fertilizerProductRepository,
                                 FertilizerMovementRepository fertilizerMovementRepository,
                                 CurrencyRateRepository currencyRateRepository,
                                 FertilizerMovementService fertilizerMovementService,
                                 @Qualifier("plantsClock") Clock clock) {
        this.fertilizerPlanRepository = fertilizerPlanRepository;
        this.fertilizerProductRepository = fertilizerProductRepository;
        this.fertilizerMovementRepository = fertilizerMovementRepository;
        this.currencyRateRepository = currencyRateRepository;
        this.fertilizerMovementService = fertilizerMovementService;
        this.clock = clock;
    }

    /**
     * Block code as stored: trimmed and upper-cased, so {@code " b "} becomes
     * {@code "B"}, as for an application.
     */
    private static String normalizeBlockCode(String blockCode) {
        return blockCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * An optional text as stored: trimmed, and {@code null} when it is blank.
     * Also turns a blank list filter into no filter.
     */
    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
     * Maps a stored plan to its API representation.
     *
     * @param plan     the stored plan, with its fertilizer and its real application if any
     * @param price    the last purchase price of its fertilizer, or {@code null}
     * @param eurToXof how many FCFA one euro is worth, or {@code null} when no rate is recorded
     * @param today    the day the delay is computed for
     */
    private static FertilizerPlanResponse toResponse(FertilizerPlan plan, LastPrice price, BigDecimal eurToXof,
                                                     LocalDate today) {
        FertilizerProduct product = plan.getProduct();
        FertilizerMovement movement = plan.getMovement();
        Amounts unitPrice = FertilizerPlanCalculator.costOf(BigDecimal.ONE, price, eurToXof);
        Amounts plannedCost = FertilizerPlanCalculator.costOf(plan.getPlannedQuantity(), price, eurToXof);
        boolean planned = plan.getStatus() == FertilizerPlanStatus.PLANNED;
        return new FertilizerPlanResponse(
                plan.getId(),
                product.getId(),
                product.getName(),
                product.getUnit(),
                plan.getFarmId(),
                plan.getBlockCode(),
                plan.getPlannedDate(),
                plan.getPlannedQuantity().doubleValue(),
                plan.getStatus(),
                FertilizerPlanCalculator.overdue(plan.getStatus(), plan.getPlannedDate(), today),
                planned ? FertilizerPlanCalculator.daysFromToday(plan.getPlannedDate(), today) : null,
                unitPrice.xof(),
                unitPrice.eur(),
                price == null ? null : price.date(),
                plannedCost.xof(),
                plannedCost.eur(),
                movement == null ? null : movement.getId(),
                movement == null ? null : movement.getMovementDate(),
                movement == null ? null : movement.getQuantity().doubleValue(),
                plan.getNote(),
                plan.getSource(),
                plan.getLastUpdated());
    }

    /**
     * The last purchase price of each given fertilizer, from one query.
     *
     * @param productIds identifiers of the fertilizers, possibly empty
     * @return the price by fertilizer identifier; a fertilizer without a priced purchase is absent
     */
    private Map<Long, LastPrice> lastPrices(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return FertilizerPlanCalculator.lastPrices(fertilizerMovementRepository.findPricedPurchases(productIds));
    }

    /**
     * How many FCFA one euro is worth, read from {@code currency_rate} on every
     * call and never cached. Read only when a price exists; while no rate is
     * recorded, each cost is given only in the currency of its purchase.
     *
     * @return the EUR to XOF rate, or {@code null} when no price needs it or none is recorded
     */
    private BigDecimal eurToXofRate(Map<Long, LastPrice> prices) {
        if (prices.isEmpty()) {
            return null;
        }
        return currencyRateRepository
                .findByBaseCurrencyAndQuoteCurrency(CurrencyCode.EUR.name(), CurrencyCode.XOF.name())
                .map(rate -> rate.getRate())
                .orElse(null);
    }

    /** One plan as the API sends it, with the price of its fertilizer read now. */
    private FertilizerPlanResponse toResponse(FertilizerPlan plan, LocalDate today) {
        Long productId = plan.getProduct().getId();
        Map<Long, LastPrice> prices = lastPrices(Set.of(productId));
        return toResponse(plan, prices.get(productId), eurToXofRate(prices), today);
    }

    /**
     * Finds a plan and locks its row until the end of the transaction, so a
     * plan cannot be completed twice, or completed and cancelled at once.
     *
     * @throws FertilizerPlanNotFoundException if no plan has this identifier
     */
    private FertilizerPlan lockPlan(Long id) {
        return fertilizerPlanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new FertilizerPlanNotFoundException(id));
    }

    /**
     * Refuses to change a plan that is already done or cancelled: both states are final.
     *
     * @throws ConflictException if the plan is not PLANNED
     */
    private static void requirePlanned(FertilizerPlan plan) {
        if (plan.getStatus() != FertilizerPlanStatus.PLANNED) {
            throw new ConflictException("Planned application with ID " + plan.getId() + " is already "
                    + plan.getStatus().name().toLowerCase(Locale.ROOT)
                    + ": only a planned application can be marked as done or cancelled");
        }
    }

    /**
     * Plans an application of a fertilizer on a block.
     *
     * <p>The block needs no recorded planting, as for an application. The
     * quantity is not compared with the stock: planning takes nothing from it,
     * and the fertilizer may be bought before the date.
     *
     * @param request the plan, already validated
     * @return the created plan, PLANNED
     * @throws FertilizerNotFoundException if no fertilizer has the given identifier
     */
    @Transactional
    public FertilizerPlanResponse createPlan(FertilizerPlanRequest request) {
        return createPlan(request, Instant.now(), LocalDate.now(clock));
    }

    /**
     * Same as {@link #createPlan(FertilizerPlanRequest)}, at an explicit write
     * time and day so {@code lastUpdated} and the delay can be tested.
     */
    FertilizerPlanResponse createPlan(FertilizerPlanRequest request, Instant now, LocalDate today) {
        FertilizerProduct product = fertilizerProductRepository.findById(request.fertilizerId())
                .orElseThrow(() -> new FertilizerNotFoundException(request.fertilizerId()));

        FertilizerPlan plan = new FertilizerPlan();
        plan.setProduct(product);
        plan.setFarmId(request.farmId());
        plan.setBlockCode(normalizeBlockCode(request.blockCode()));
        plan.setPlannedDate(request.plannedDate());
        plan.setPlannedQuantity(BigDecimal.valueOf(request.plannedQuantity()));
        plan.setStatus(FertilizerPlanStatus.PLANNED);
        plan.setNote(normalizeOptionalText(request.note()));
        plan.setSource(USER_ENTRY_SOURCE);
        plan.setLastUpdated(now);
        return toResponse(fertilizerPlanRepository.save(plan), today);
    }

    /**
     * Retrieves the plans matching the optional filters.
     *
     * <p>Every filter is independent and optional. Both dates are included and
     * compare the planned date. A filter that matches nothing, or a
     * {@code from} after {@code to}, returns an empty list and is never an
     * error. The prices of the listed fertilizers come from one query.
     *
     * @param fertilizerId fertilizer identifier, or {@code null} for every fertilizer
     * @param status       plan status, or {@code null} for every status
     * @param farmId       farm identifier, or {@code null} for every farm
     * @param blockCode    raw block value as stored (for example {@code "B"}),
     *                     or {@code null} for every block
     * @param from         first planned date, included, or {@code null}
     * @param to           last planned date, included, or {@code null}
     * @return the matching plans, ordered by planned date then identifier
     */
    @Transactional(readOnly = true)
    public List<FertilizerPlanResponse> getAllPlans(Long fertilizerId, FertilizerPlanStatus status, Integer farmId,
                                                    String blockCode, LocalDate from, LocalDate to) {
        return getAllPlans(fertilizerId, status, farmId, blockCode, from, to, LocalDate.now(clock));
    }

    /** Same as the public method, for an explicit day so the delays can be tested. */
    List<FertilizerPlanResponse> getAllPlans(Long fertilizerId, FertilizerPlanStatus status, Integer farmId,
                                             String blockCode, LocalDate from, LocalDate to, LocalDate today) {
        List<FertilizerPlan> plans = fertilizerPlanRepository.findByOptionalFilters(
                fertilizerId, status, farmId, normalizeOptionalText(blockCode), from, to);
        Map<Long, LastPrice> prices = lastPrices(
                plans.stream().map(plan -> plan.getProduct().getId()).collect(Collectors.toSet()));
        BigDecimal eurToXof = eurToXofRate(prices);
        return plans.stream()
                .map(plan -> toResponse(plan, prices.get(plan.getProduct().getId()), eurToXof, today))
                .toList();
    }

    /**
     * Marks a plan as done by recording the real application.
     *
     * <p>The application is recorded as any other one, on the fertilizer, the
     * farm and the block of the plan: it takes its quantity from the stock and
     * is refused when the stock does not cover it. The plan then links to it
     * and becomes DONE. Both writes share one transaction: a refused
     * application leaves the plan PLANNED.
     *
     * @param id      the plan identifier
     * @param request what was really applied, already validated
     * @return the plan, DONE, with its real application
     * @throws FertilizerPlanNotFoundException if no plan has this identifier
     * @throws ConflictException               if the plan is already done or cancelled
     * @throws BusinessRuleException           if the quantity is larger than the current stock
     */
    @Transactional
    public FertilizerPlanResponse completePlan(Long id, FertilizerPlanCompletionRequest request) {
        return completePlan(id, request, Instant.now(), LocalDate.now(clock));
    }

    /**
     * Same as {@link #completePlan(Long, FertilizerPlanCompletionRequest)}, at
     * an explicit write time and day.
     */
    FertilizerPlanResponse completePlan(Long id, FertilizerPlanCompletionRequest request, Instant now,
                                        LocalDate today) {
        FertilizerPlan plan = lockPlan(id);
        requirePlanned(plan);

        ApplicationRequest application = new ApplicationRequest(
                request.applicationDate(),
                request.quantity(),
                plan.getFarmId(),
                plan.getBlockCode(),
                request.applicator(),
                request.method());
        FertilizerMovementResponse movement =
                fertilizerMovementService.recordApplication(plan.getProduct().getId(), application);

        plan.setMovement(fertilizerMovementRepository.getReferenceById(movement.id()));
        plan.setStatus(FertilizerPlanStatus.DONE);
        plan.setLastUpdated(now);
        return toResponse(fertilizerPlanRepository.save(plan), today);
    }

    /**
     * Cancels a plan. Nothing is recorded in the stock, and the plan no longer
     * counts in the comparison or in the stock check.
     *
     * @param id the plan identifier
     * @return the plan, CANCELLED
     * @throws FertilizerPlanNotFoundException if no plan has this identifier
     * @throws ConflictException               if the plan is already done or cancelled
     */
    @Transactional
    public FertilizerPlanResponse cancelPlan(Long id) {
        return cancelPlan(id, Instant.now(), LocalDate.now(clock));
    }

    /** Same as {@link #cancelPlan(Long)}, at an explicit write time and day. */
    FertilizerPlanResponse cancelPlan(Long id, Instant now, LocalDate today) {
        FertilizerPlan plan = lockPlan(id);
        requirePlanned(plan);
        plan.setStatus(FertilizerPlanStatus.CANCELLED);
        plan.setLastUpdated(now);
        return toResponse(fertilizerPlanRepository.save(plan), today);
    }

    /**
     * Compares planned and applied quantities by block and fertilizer over a period.
     *
     * <p>A plan counts in the period of its planned date, and its real
     * application with it, even when applied a few days outside the period. An
     * application made without a plan counts in the period of its own date.
     * Without any date, the period is the
     * current calendar year; with one date only, the other side is open. Both
     * costs use the last purchase price of the fertilizer.
     *
     * @param from first day, included, or {@code null}
     * @param to   last day, included, or {@code null}
     * @return the period used and one line per block and fertilizer, possibly none
     */
    @Transactional(readOnly = true)
    public FertilizerPlanComparisonResponse getComparison(LocalDate from, LocalDate to) {
        return getComparison(from, to, LocalDate.now(clock));
    }

    /** Same as {@link #getComparison(LocalDate, LocalDate)}, for an explicit day. */
    FertilizerPlanComparisonResponse getComparison(LocalDate from, LocalDate to, LocalDate today) {
        LocalDate first = from;
        LocalDate last = to;
        if (from == null && to == null) {
            first = today.withDayOfYear(1);
            last = today.withDayOfYear(today.lengthOfYear());
        }
        List<FertilizerPlan> plans =
                fertilizerPlanRepository.findByOptionalFilters(null, null, null, null, first, last);
        List<FertilizerMovement> applications = fertilizerMovementRepository.findByOptionalFilters(
                null, FertilizerMovementType.APPLICATION, null, null, first, last);
        Set<Long> linkedMovementIds = new HashSet<>(fertilizerPlanRepository.findLinkedMovementIds());

        List<ComparisonLine> lines = FertilizerPlanCalculator.compare(plans, applications, linkedMovementIds);
        Map<Long, LastPrice> prices = lastPrices(
                lines.stream().map(line -> line.product().getId()).collect(Collectors.toSet()));
        BigDecimal eurToXof = eurToXofRate(prices);

        List<FertilizerPlanComparisonResponse.Row> rows = lines.stream()
                .map(line -> {
                    FertilizerProduct product = line.product();
                    LastPrice price = prices.get(product.getId());
                    Amounts plannedCost = FertilizerPlanCalculator.costOf(line.planned(), price, eurToXof);
                    Amounts appliedCost = FertilizerPlanCalculator.costOf(line.applied(), price, eurToXof);
                    return new FertilizerPlanComparisonResponse.Row(
                            line.blockCode(),
                            product.getId(),
                            product.getName(),
                            product.getUnit(),
                            line.planned().doubleValue(),
                            line.remaining().doubleValue(),
                            line.applied().doubleValue(),
                            line.unplanned().doubleValue(),
                            line.gap().doubleValue(),
                            toDouble(FertilizerPlanCalculator.gapPercent(line.planned(), line.applied())),
                            plannedCost.xof(),
                            plannedCost.eur(),
                            appliedCost.xof(),
                            appliedCost.eur(),
                            price == null ? null : price.date());
                })
                .toList();
        return new FertilizerPlanComparisonResponse(first, last, rows);
    }

    /**
     * Lists the fertilizers whose stock does not cover the applications
     * planned in the coming days.
     *
     * <p>Counts the plans still PLANNED up to today plus {@code days}, late
     * ones included: they still need their fertilizer. It is a warning only;
     * no plan is refused because of it.
     *
     * @param days how many days ahead, or {@code null} for {@value #DEFAULT_STOCK_CHECK_DAYS}
     * @return the window used and one line per fertilizer short of stock, possibly none
     */
    @Transactional(readOnly = true)
    public FertilizerPlanStockCheckResponse getStockCheck(Integer days) {
        return getStockCheck(days, LocalDate.now(clock));
    }

    /** Same as {@link #getStockCheck(Integer)}, for an explicit day. */
    FertilizerPlanStockCheckResponse getStockCheck(Integer days, LocalDate today) {
        int window = days == null ? DEFAULT_STOCK_CHECK_DAYS : days;
        LocalDate until = today.plusDays(window);
        List<FertilizerPlan> openPlans = fertilizerPlanRepository.findOpenUntil(until);
        if (openPlans.isEmpty()) {
            return new FertilizerPlanStockCheckResponse(window, today, until, List.of());
        }
        Set<Long> productIds = openPlans.stream().map(plan -> plan.getProduct().getId()).collect(Collectors.toSet());
        Map<Long, BigDecimal> stocks = FertilizerStockCalculator.stocksByProduct(
                fertilizerMovementRepository.findMovementTotals(productIds));

        List<FertilizerPlanStockCheckResponse.Warning> warnings =
                FertilizerPlanCalculator.shortages(openPlans, stocks, today).stream()
                        .map(need -> toWarning(need))
                        .toList();
        return new FertilizerPlanStockCheckResponse(window, today, until, warnings);
    }

    private static FertilizerPlanStockCheckResponse.Warning toWarning(StockNeed need) {
        FertilizerProduct product = need.product();
        return new FertilizerPlanStockCheckResponse.Warning(
                product.getId(),
                product.getName(),
                product.getUnit(),
                need.planned().doubleValue(),
                need.overdue().doubleValue(),
                need.stock().doubleValue(),
                need.shortfall().doubleValue());
    }
}
