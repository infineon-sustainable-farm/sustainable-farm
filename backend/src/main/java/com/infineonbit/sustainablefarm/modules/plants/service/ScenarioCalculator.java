package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Cost-benefit arithmetic of a planting scenario, with no data of its own.
 *
 * <p>Every number comes in as a parameter: the assumptions the user entered and
 * the yield share of each growth phase. Nothing here is an agronomic or a
 * financial value. The computation is exact, in decimals, and rounds each
 * quantity to 0.1 kg and each amount to the unit of its currency (the franc,
 * the cent), halves up; every total is computed from the rounded values it
 * adds up, so the figures given always add up.
 *
 * <p>In a year where the trees give {@code share} of their full yield:
 * <ul>
 *     <li>harvest = trees × yield per tree × share;</li>
 *     <li>losses = harvest × loss rate, none without a loss rate;</li>
 *     <li>sellable = harvest - losses;</li>
 *     <li>revenue = sellable × price, unknown without a price;</li>
 *     <li>operating cost = fixed cost + variable cost × sellable, the fixed
 *         cost alone without a variable cost;</li>
 *     <li>net benefit = revenue - operating cost.</li>
 * </ul>
 */
final class ScenarioCalculator {

    /** Benefit line of the trees added or removed by a variant. */
    static final String TREE_COUNT = "TREE_COUNT";
    /** Benefit line of the change of the yield per tree. */
    static final String YIELD_PER_TREE = "YIELD_PER_TREE";
    /** Benefit line of the change of the loss rate. */
    static final String LOSS_RATE = "LOSS_RATE";
    /** Benefit line of the variable cost of the quantity sold in addition. */
    static final String VARIABLE_OPERATING_COST = "VARIABLE_OPERATING_COST";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ScenarioCalculator() {
    }

    /**
     * The assumptions of a plan, as the computation reads them.
     *
     * @param treeCount         number of trees, at least 1
     * @param yieldPerTreeKg    yearly yield of one tree in full production
     * @param lossRate          from 0 to 1, or {@code null} when not entered
     * @param pricePerKg        price of one kg, or {@code null} when not entered
     * @param currency          currency of every amount
     * @param annualFixedCost   operating cost of a year whatever the harvest
     * @param variableCostPerKg operating cost of each kg sold, or {@code null} when not entered
     * @param investment        investment of the plan, or {@code null} when unknown
     */
    record Plan(int treeCount, BigDecimal yieldPerTreeKg, BigDecimal lossRate, BigDecimal pricePerKg,
                CurrencyCode currency, BigDecimal annualFixedCost, BigDecimal variableCostPerKg,
                BigDecimal investment) {
    }

    /**
     * A plan in one year.
     *
     * @param harvestKg     harvest
     * @param lossKg        part of it lost, {@code null} without a loss rate
     * @param sellableKg    harvest minus losses
     * @param revenue       {@code null} without a price
     * @param operatingCost fixed cost plus variable cost
     * @param netBenefit    revenue minus operating cost, {@code null} without a price
     */
    record YearResult(BigDecimal harvestKg, BigDecimal lossKg, BigDecimal sellableKg, BigDecimal revenue,
                      BigDecimal operatingCost, BigDecimal netBenefit) {
    }

    /**
     * The growth phase of the trees in one year of a plan.
     *
     * @param year        the year, from 1
     * @param ageYears    completed years of the trees during that year
     * @param growthPhase the phase for that age
     * @param yieldShare  share of the full yield in that phase, from 0 to 1
     */
    record YearPhase(int year, int ageYears, String growthPhase, BigDecimal yieldShare) {
    }

    /**
     * One year of a plan, with the net benefits of the years so far.
     *
     * @param phase                the growth phase of the year
     * @param result               the plan in that year
     * @param cumulativeNetBenefit net benefits so far minus the investment, {@code null} when
     *                             a net benefit or the investment is unknown
     */
    record PlanYear(YearPhase phase, YearResult result, BigDecimal cumulativeNetBenefit) {
    }

    /**
     * What a variant changes.
     *
     * @param treeCountChange       trees added, negative when removed
     * @param yieldPerTreeChangePct change of the yield per tree in percent, or {@code null}
     * @param lossRate              loss rate of the variant, or {@code null} to keep the plan's
     * @param additionalInvestment  investment on top of the plan's and its added trees', or {@code null}
     */
    record Variant(int treeCountChange, BigDecimal yieldPerTreeChangePct, BigDecimal lossRate,
                   BigDecimal additionalInvestment) {
    }

    /**
     * One cause of the annual benefit of a variant.
     *
     * @param type       one of the line types of this class
     * @param quantityKg sellable quantity the cause adds
     * @param unitAmount price, or variable cost, per kg
     * @param amount     what the cause adds to the net benefit
     */
    record Line(String type, BigDecimal quantityKg, BigDecimal unitAmount, BigDecimal amount) {
    }

    /**
     * What a variant changes in a year of full production.
     *
     * @param treeCountChange                  trees added, negative when removed
     * @param harvestKg                        change of the harvest
     * @param sellableKg                       change of the sellable quantity
     * @param revenue                          change of the revenue, {@code null} without a price
     * @param operatingCost                    change of the operating cost
     * @param roiPctPoints                     change of the rounded return, {@code null} when one is unknown
     * @param lines                            the annual benefit by cause, empty without a price
     * @param annualBenefit                    change of the net benefit, {@code null} without a price
     * @param additionalInvestment             investment of the variant on top of the plan's,
     *                                         {@code null} when unknown
     * @param additionalInvestmentPaybackYears additional investment / annual benefit, or {@code null}
     */
    record Change(int treeCountChange, BigDecimal harvestKg, BigDecimal sellableKg, BigDecimal revenue,
                  BigDecimal operatingCost, BigDecimal roiPctPoints, List<Line> lines, BigDecimal annualBenefit,
                  BigDecimal additionalInvestment, BigDecimal additionalInvestmentPaybackYears) {
    }

    /** Rounds a quantity to 0.1 kg, halves up. */
    static BigDecimal roundKg(BigDecimal kg) {
        return kg.setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * Rounds an amount to the unit of its currency, halves up: the franc for
     * XOF, the cent for EUR, as {@link CurrencyConverter} does.
     */
    static BigDecimal roundMoney(BigDecimal amount, CurrencyCode currency) {
        return amount.setScale(currency == CurrencyCode.XOF ? 0 : 2, RoundingMode.HALF_UP);
    }

    /**
     * A plan in a year where the trees give a share of their full yield.
     *
     * @param plan       the plan
     * @param yieldShare from 0 to 1; 1 for a year of full production
     * @return the year, rounded
     */
    static YearResult year(Plan plan, BigDecimal yieldShare) {
        BigDecimal harvest = roundKg(BigDecimal.valueOf(plan.treeCount())
                .multiply(plan.yieldPerTreeKg())
                .multiply(yieldShare));
        BigDecimal loss = plan.lossRate() == null ? null : roundKg(harvest.multiply(plan.lossRate()));
        BigDecimal sellable = loss == null ? harvest : harvest.subtract(loss);
        BigDecimal revenue = plan.pricePerKg() == null
                ? null
                : roundMoney(sellable.multiply(plan.pricePerKg()), plan.currency());
        BigDecimal variableCost = plan.variableCostPerKg() == null
                ? BigDecimal.ZERO
                : sellable.multiply(plan.variableCostPerKg());
        BigDecimal operatingCost = roundMoney(plan.annualFixedCost().add(variableCost), plan.currency());
        BigDecimal netBenefit = revenue == null ? null : revenue.subtract(operatingCost);
        return new YearResult(harvest, loss, sellable, revenue, operatingCost, netBenefit);
    }

    /** A plan in a year of full production. */
    static YearResult fullProduction(Plan plan) {
        return year(plan, BigDecimal.ONE);
    }

    /**
     * Yearly return of a plan in full production.
     *
     * @return net benefit × 100 / investment, to 0.1, or {@code null} when either is unknown
     */
    static BigDecimal roiPct(BigDecimal netBenefit, BigDecimal investment) {
        if (netBenefit == null || investment == null) {
            return null;
        }
        return netBenefit.multiply(HUNDRED).divide(investment, 1, RoundingMode.HALF_UP);
    }

    /**
     * Years to recover an investment from a constant yearly benefit.
     *
     * @return investment / benefit, to 0.1 year, or {@code null} when either is
     *         unknown or the benefit is not above 0: it is never recovered
     */
    static BigDecimal paybackYears(BigDecimal investment, BigDecimal benefit) {
        if (investment == null || benefit == null || benefit.signum() <= 0) {
            return null;
        }
        return investment.divide(benefit, 1, RoundingMode.HALF_UP);
    }

    /**
     * A plan year by year, the investment spent before year 1.
     *
     * @param plan   the plan
     * @param phases the growth phase of each year, in order
     * @return one entry per phase, with the cumulative net benefit
     */
    static List<PlanYear> years(Plan plan, List<YearPhase> phases) {
        List<PlanYear> years = new ArrayList<>(phases.size());
        BigDecimal cumulative = plan.investment() == null ? null : plan.investment().negate();
        for (YearPhase phase : phases) {
            YearResult result = year(plan, phase.yieldShare());
            cumulative = cumulative == null || result.netBenefit() == null
                    ? null
                    : cumulative.add(result.netBenefit());
            years.add(new PlanYear(phase, result, cumulative));
        }
        return years;
    }

    /**
     * The first year whose cumulative net benefit, investment deducted, is 0 or more.
     *
     * @param years a plan year by year
     * @return that year, or {@code null} when it is not reached or unknown
     */
    static Integer paybackYear(List<PlanYear> years) {
        return years.stream()
                .filter(year -> year.cumulativeNetBenefit() != null && year.cumulativeNetBenefit().signum() >= 0)
                .map(year -> year.phase().year())
                .findFirst()
                .orElse(null);
    }

    /**
     * Investment of a variant on top of that of its plan: the added trees at
     * their unit cost, plus the additional investment given. Removed trees save
     * nothing: the investment of the plan is already spent.
     *
     * @param variant               the variant
     * @param costPerAdditionalTree investment for each added tree, or {@code null} when not entered
     * @param currency              currency of the amounts
     * @return the additional investment, rounded, or {@code null} when trees are
     *         added without a cost per tree
     */
    static BigDecimal additionalInvestment(Variant variant, BigDecimal costPerAdditionalTree,
                                           CurrencyCode currency) {
        BigDecimal addedTrees = BigDecimal.ZERO;
        if (variant.treeCountChange() > 0) {
            if (costPerAdditionalTree == null) {
                return null;
            }
            addedTrees = costPerAdditionalTree.multiply(BigDecimal.valueOf(variant.treeCountChange()));
        }
        BigDecimal given = variant.additionalInvestment() == null ? BigDecimal.ZERO : variant.additionalInvestment();
        return roundMoney(addedTrees.add(given), currency);
    }

    /**
     * A plan with a variant applied. The yield per tree of the variant is
     * rounded to 0.1 kg, the precision of an entered yield.
     *
     * @param plan                 the plan
     * @param variant              the variant
     * @param additionalInvestment what {@link #additionalInvestment} gives, possibly {@code null}
     * @return the plan of the variant; its investment is {@code null} when the
     *         additional investment is
     */
    static Plan apply(Plan plan, Variant variant, BigDecimal additionalInvestment) {
        BigDecimal yieldPerTreeKg = variant.yieldPerTreeChangePct() == null
                ? plan.yieldPerTreeKg()
                : roundKg(plan.yieldPerTreeKg().multiply(HUNDRED.add(variant.yieldPerTreeChangePct()))
                        .divide(HUNDRED));
        BigDecimal investment = plan.investment() == null || additionalInvestment == null
                ? null
                : plan.investment().add(additionalInvestment);
        return new Plan(plan.treeCount() + variant.treeCountChange(), yieldPerTreeKg,
                variant.lossRate() == null ? plan.lossRate() : variant.lossRate(),
                plan.pricePerKg(), plan.currency(), plan.annualFixedCost(), plan.variableCostPerKg(), investment);
    }

    /**
     * What a variant changes in a year of full production.
     *
     * <p>The annual benefit is split by cause, applying the changes one after
     * the other: the trees, then the yield per tree, then the loss rate. Each
     * revenue line is what its change adds to the revenue at that point, and the
     * last line is the variable cost of the quantity sold in addition. So the
     * lines add up exactly to the change of the net benefit. A line worth 0 is
     * left out.
     *
     * @param plan                 the plan
     * @param variantPlan          the plan with the variant applied
     * @param additionalInvestment the investment of the variant on top of the plan's, possibly {@code null}
     * @return the change
     */
    static Change change(Plan plan, Plan variantPlan, BigDecimal additionalInvestment) {
        YearResult before = fullProduction(plan);
        YearResult after = fullProduction(variantPlan);

        List<Line> lines = new ArrayList<>();
        if (plan.pricePerKg() != null) {
            Plan withTrees = new Plan(variantPlan.treeCount(), plan.yieldPerTreeKg(), plan.lossRate(),
                    plan.pricePerKg(), plan.currency(), plan.annualFixedCost(), plan.variableCostPerKg(),
                    plan.investment());
            Plan withYield = new Plan(variantPlan.treeCount(), variantPlan.yieldPerTreeKg(), plan.lossRate(),
                    plan.pricePerKg(), plan.currency(), plan.annualFixedCost(), plan.variableCostPerKg(),
                    plan.investment());
            YearResult afterTrees = fullProduction(withTrees);
            YearResult afterYield = fullProduction(withYield);
            addRevenueLine(lines, TREE_COUNT, before, afterTrees, plan.pricePerKg());
            addRevenueLine(lines, YIELD_PER_TREE, afterTrees, afterYield, plan.pricePerKg());
            addRevenueLine(lines, LOSS_RATE, afterYield, after, plan.pricePerKg());
            if (plan.variableCostPerKg() != null) {
                BigDecimal amount = before.operatingCost().subtract(after.operatingCost());
                if (amount.signum() != 0) {
                    lines.add(new Line(VARIABLE_OPERATING_COST, after.sellableKg().subtract(before.sellableKg()),
                            plan.variableCostPerKg(), amount));
                }
            }
        }

        BigDecimal annualBenefit = before.netBenefit() == null
                ? null
                : after.netBenefit().subtract(before.netBenefit());
        BigDecimal roiBefore = roiPct(before.netBenefit(), plan.investment());
        BigDecimal roiAfter = roiPct(after.netBenefit(), variantPlan.investment());
        BigDecimal paybackOfAdditional = additionalInvestment == null || additionalInvestment.signum() <= 0
                ? null
                : paybackYears(additionalInvestment, annualBenefit);
        return new Change(
                variantPlan.treeCount() - plan.treeCount(),
                after.harvestKg().subtract(before.harvestKg()),
                after.sellableKg().subtract(before.sellableKg()),
                before.revenue() == null ? null : after.revenue().subtract(before.revenue()),
                after.operatingCost().subtract(before.operatingCost()),
                roiBefore == null || roiAfter == null ? null : roiAfter.subtract(roiBefore),
                lines,
                annualBenefit,
                additionalInvestment,
                paybackOfAdditional);
    }

    private static void addRevenueLine(List<Line> lines, String type, YearResult from, YearResult to,
                                       BigDecimal pricePerKg) {
        BigDecimal amount = to.revenue().subtract(from.revenue());
        if (amount.signum() != 0) {
            lines.add(new Line(type, to.sellableKg().subtract(from.sellableKg()), pricePerKg, amount));
        }
    }
}
