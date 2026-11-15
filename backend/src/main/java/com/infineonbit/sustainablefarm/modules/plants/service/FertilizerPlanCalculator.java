package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovement;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlan;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlanStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Arithmetic of the planned applications of fertilizer, with no data of its
 * own: the last purchase price, the cost of a quantity at that price, the
 * delay of a plan, the comparison of planned and applied quantities, and the
 * stock that the coming plans need.
 *
 * <p>Nothing here recommends a quantity: every quantity is the farmer's.
 */
final class FertilizerPlanCalculator {

    /** Decimals kept in an amount before it is rounded to the franc or to the cent. */
    private static final int AMOUNT_SCALE = 6;

    private FertilizerPlanCalculator() {
    }

    /**
     * The last purchase price of a fertilizer: the cost and the quantity of its
     * most recent purchase that has a cost. The price of one unit is the cost
     * divided by the quantity; both are kept so that a cost is computed from
     * the exact price and rounded once.
     *
     * @param totalCost cost of that purchase, in {@code currency}
     * @param quantity  quantity bought, in the unit of the fertilizer, above 0
     * @param currency  currency of the cost
     * @param date      date of that purchase
     */
    record LastPrice(BigDecimal totalCost, BigDecimal quantity, CurrencyCode currency, LocalDate date) {

        /** What a quantity costs at this price, in {@link #currency}, not rounded to a coin yet. */
        BigDecimal costOf(BigDecimal amountOfFertilizer) {
            return amountOfFertilizer.multiply(totalCost).divide(quantity, AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
    }

    /**
     * An amount in both currencies, as the API sends it.
     *
     * @param xof the amount in FCFA, rounded to the franc, or {@code null}
     * @param eur the amount in euros, rounded to the cent, or {@code null}
     */
    record Amounts(Long xof, Double eur) {

        /** No amount in either currency: the fertilizer has no priced purchase. */
        static final Amounts NONE = new Amounts(null, null);
    }

    /**
     * The last purchase price of each fertilizer that has one.
     *
     * @param pricedPurchases the purchases with a cost, the most recent first for
     *                        each fertilizer, as the repository returns them
     * @return the price by fertilizer identifier; a fertilizer without a priced purchase is absent
     */
    static Map<Long, LastPrice> lastPrices(List<FertilizerMovement> pricedPurchases) {
        Map<Long, LastPrice> prices = new LinkedHashMap<>();
        for (FertilizerMovement purchase : pricedPurchases) {
            prices.putIfAbsent(purchase.getProduct().getId(), new LastPrice(
                    purchase.getTotalCost(),
                    purchase.getQuantity(),
                    CurrencyCode.valueOf(purchase.getCurrency()),
                    purchase.getMovementDate()));
        }
        return prices;
    }

    /**
     * What a quantity of a fertilizer costs at its last purchase price, in both
     * currencies.
     *
     * @param quantity the quantity, in the unit of the fertilizer
     * @param price    the last purchase price, or {@code null} when no purchase has a cost
     * @param eurToXof how many FCFA one euro is worth, or {@code null} when no rate is recorded
     * @return the cost; without a price, no amount; without a rate, only the
     *         amount in the currency of the purchase
     */
    static Amounts costOf(BigDecimal quantity, LastPrice price, BigDecimal eurToXof) {
        if (price == null) {
            return Amounts.NONE;
        }
        BigDecimal cost = price.costOf(quantity);
        BigDecimal eur = CurrencyConverter.toEur(cost, price.currency(), eurToXof);
        return new Amounts(
                CurrencyConverter.toXof(cost, price.currency(), eurToXof),
                eur == null ? null : eur.doubleValue());
    }

    /**
     * Days between today and a planned date: positive before the date, 0 on
     * it, negative once it has passed.
     */
    static int daysFromToday(LocalDate plannedDate, LocalDate today) {
        return (int) ChronoUnit.DAYS.between(today, plannedDate);
    }

    /**
     * Whether a plan is late: still PLANNED after its date. A plan of today is
     * not late, and a done or cancelled plan never is.
     */
    static boolean overdue(FertilizerPlanStatus status, LocalDate plannedDate, LocalDate today) {
        return status == FertilizerPlanStatus.PLANNED && plannedDate.isBefore(today);
    }

    /**
     * The gap between applied and planned quantities, as a percentage of the
     * planned one, with one decimal: -25.0 when a quarter is missing.
     *
     * @return the percentage, or {@code null} when nothing was planned
     */
    static BigDecimal gapPercent(BigDecimal planned, BigDecimal applied) {
        if (planned.signum() == 0) {
            return null;
        }
        return applied.subtract(planned)
                .multiply(BigDecimal.valueOf(100))
                .divide(planned, 1, RoundingMode.HALF_UP);
    }

    /** Planned and applied quantities of one fertilizer on one block. */
    static final class ComparisonLine {

        private final String blockCode;
        private final FertilizerProduct product;
        private BigDecimal planned = BigDecimal.ZERO;
        private BigDecimal remaining = BigDecimal.ZERO;
        private BigDecimal applied = BigDecimal.ZERO;
        private BigDecimal unplanned = BigDecimal.ZERO;

        private ComparisonLine(String blockCode, FertilizerProduct product) {
            this.blockCode = blockCode;
            this.product = product;
        }

        String blockCode() {
            return blockCode;
        }

        FertilizerProduct product() {
            return product;
        }

        /** Total of the PLANNED and DONE plans. */
        BigDecimal planned() {
            return planned;
        }

        /** The part of {@link #planned()} still PLANNED. */
        BigDecimal remaining() {
            return remaining;
        }

        /** Total really applied, planned or not. */
        BigDecimal applied() {
            return applied;
        }

        /** The part of {@link #applied()} applied without a plan. */
        BigDecimal unplanned() {
            return unplanned;
        }

        /** Applied minus planned: negative when less was applied than planned. */
        BigDecimal gap() {
            return applied.subtract(planned);
        }
    }

    /**
     * Compares what was planned with what was applied, by block and fertilizer.
     *
     * <p>A cancelled plan counts for nothing. The real application of a done
     * plan is counted with its plan, whatever its own date: a plan done a few
     * days early or late is compared with what it became, not left without
     * its application because the two dates fall on each side of a period. An
     * application no plan is linked to counts as applied too, and apart, as
     * unplanned. An application linked to a plan is therefore never counted
     * from the list of applications, or it would count twice.
     *
     * @param plans             the plans of the period, of any status, each done one with
     *                          its real application
     * @param applications      the real applications of the period, each with its fertilizer
     * @param linkedMovementIds identifiers of the applications that complete a plan
     * @return one line per block and fertilizer, by block, then fertilizer name, then identifier
     */
    static List<ComparisonLine> compare(List<FertilizerPlan> plans, List<FertilizerMovement> applications,
                                        Set<Long> linkedMovementIds) {
        Map<String, ComparisonLine> lines = new LinkedHashMap<>();
        for (FertilizerPlan plan : plans) {
            if (plan.getStatus() == FertilizerPlanStatus.CANCELLED) {
                continue;
            }
            ComparisonLine line = lineOf(lines, plan.getBlockCode(), plan.getProduct());
            line.planned = line.planned.add(plan.getPlannedQuantity());
            if (plan.getStatus() == FertilizerPlanStatus.PLANNED) {
                line.remaining = line.remaining.add(plan.getPlannedQuantity());
            }
            if (plan.getMovement() != null) {
                line.applied = line.applied.add(plan.getMovement().getQuantity());
            }
        }
        for (FertilizerMovement application : applications) {
            if (linkedMovementIds.contains(application.getId())) {
                continue;
            }
            ComparisonLine line = lineOf(lines, application.getBlockCode(), application.getProduct());
            line.applied = line.applied.add(application.getQuantity());
            line.unplanned = line.unplanned.add(application.getQuantity());
        }
        List<ComparisonLine> sorted = new ArrayList<>(lines.values());
        sorted.sort(Comparator.comparing(ComparisonLine::blockCode)
                .thenComparing(line -> line.product().getName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(line -> line.product().getId()));
        return sorted;
    }

    private static ComparisonLine lineOf(Map<String, ComparisonLine> lines, String blockCode,
                                         FertilizerProduct product) {
        return lines.computeIfAbsent(blockCode + "\u0000" + product.getId(),
                key -> new ComparisonLine(blockCode, product));
    }

    /**
     * What the open plans of one fertilizer need, against its stock.
     *
     * @param product the fertilizer
     * @param planned total of its plans still PLANNED in the window, late ones included
     * @param overdue the part of {@code planned} whose date has passed
     * @param stock   its stock today
     */
    record StockNeed(FertilizerProduct product, BigDecimal planned, BigDecimal overdue, BigDecimal stock) {

        /** How much is missing: planned minus stock. */
        BigDecimal shortfall() {
            return planned.subtract(stock);
        }
    }

    /**
     * The fertilizers whose stock does not cover their open plans.
     *
     * <p>A stock exactly equal to the planned quantity covers it: only a
     * planned quantity larger than the stock is a shortage.
     *
     * @param openPlans the plans still PLANNED up to the end of the window, late
     *                  ones included, each with its fertilizer
     * @param stocks    the stock by fertilizer identifier; a fertilizer without movement is absent
     * @param today     the day the check is computed for
     * @return one need per fertilizer short of stock, by name then identifier
     */
    static List<StockNeed> shortages(List<FertilizerPlan> openPlans, Map<Long, BigDecimal> stocks, LocalDate today) {
        Map<Long, FertilizerProduct> products = new LinkedHashMap<>();
        Map<Long, BigDecimal> planned = new LinkedHashMap<>();
        Map<Long, BigDecimal> overdue = new LinkedHashMap<>();
        for (FertilizerPlan plan : openPlans) {
            Long productId = plan.getProduct().getId();
            products.putIfAbsent(productId, plan.getProduct());
            planned.merge(productId, plan.getPlannedQuantity(), BigDecimal::add);
            if (plan.getPlannedDate().isBefore(today)) {
                overdue.merge(productId, plan.getPlannedQuantity(), BigDecimal::add);
            }
        }
        List<StockNeed> needs = new ArrayList<>();
        for (Map.Entry<Long, FertilizerProduct> entry : products.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal stock = stocks.getOrDefault(productId, BigDecimal.ZERO);
            if (planned.get(productId).compareTo(stock) > 0) {
                needs.add(new StockNeed(entry.getValue(), planned.get(productId),
                        overdue.getOrDefault(productId, BigDecimal.ZERO), stock));
            }
        }
        needs.sort(Comparator.comparing((StockNeed need) -> need.product().getName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(need -> need.product().getId()));
        return needs;
    }
}
