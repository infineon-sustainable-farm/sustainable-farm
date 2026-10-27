package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.util.List;

/**
 * What a planting scenario gives, and what a variant of it changes, computed on
 * every read from its assumptions and never stored.
 *
 * <p>{@code basis} names the data layer, always {@code "scenario"}: a plan, not
 * the planted orchard of the yield forecast.
 *
 * <p>Two views of the same plan. The annual figures are those of a year in full
 * production. The years follow the growth phases: the trees of the plan are all
 * planted before year 1, so in year n they are n - 1 years old, and their
 * harvest is the full yield times the share of their phase, with the shares the
 * forecast uses. The fixed cost runs from year 1, harvest or not.
 *
 * <p>A result that needs a value the user has not entered is {@code null}, never
 * 0: without a price there is no revenue, hence no net benefit, return or
 * payback. {@code missingInputs} names the optional values not entered. Without
 * a loss rate the quantities are before losses; without a variable cost the
 * operating cost is the fixed cost alone.
 *
 * <p>The quantities are rounded to 0.1 kg, the amounts to the unit of their
 * currency (the franc, the cent), the percentages and years to 0.1, all halves
 * up. Each total is computed from the rounded values it adds up, so the figures
 * shown always add up.
 *
 * @param scenarioId    identifier of the scenario
 * @param name          name of the scenario
 * @param basis         data layer, always {@code "scenario"}
 * @param currency      currency the scenario is entered in, XOF or EUR
 * @param eurToXofRate  how many FCFA one euro is worth, as recorded; {@code null} when no rate
 *                      is recorded, and then each amount is only in {@code currency}
 * @param horizonYears  number of years of the projection
 * @param missingInputs the optional values not entered, among {@code lossRate},
 *                      {@code pricePerKg}, {@code variableCostPerKg} and
 *                      {@code costPerAdditionalTree}
 * @param phaseShares   the yield share in effect of each growth phase, as the years use it
 * @param baseline      the scenario as entered
 * @param variant       the scenario with the variant applied, {@code null} without a variant
 * @param change        what the variant changes, {@code null} without a variant
 */
public record ScenarioAnalysisResponse(
        Long scenarioId,
        String name,
        String basis,
        String currency,
        Double eurToXofRate,
        Integer horizonYears,
        List<String> missingInputs,
        List<GrowthPhaseYieldShareResponse> phaseShares,
        Projection baseline,
        Projection variant,
        Change change) {

    /**
     * An amount in both currencies: the one of the scenario, rounded, and the
     * other one converted with the recorded rate, {@code null} without a rate.
     * Negative for a loss or a decrease. Each amount is converted on its own,
     * so in the other currency a total may differ by one unit from the sum of
     * its parts.
     *
     * @param xof the amount in FCFA, rounded to the franc
     * @param eur the amount in euros, rounded to the cent
     */
    public record Amount(Long xof, Double eur) {
    }

    /**
     * A plan, in a year of full production and year by year.
     *
     * @param treeCount          number of trees
     * @param yieldPerTreeKg     yearly yield of one tree in full production, in kg
     * @param lossRate           loss rate, {@code null} when not entered
     * @param harvestKg          harvest of a year in full production
     * @param lossKg             part of it lost, {@code null} without a loss rate
     * @param sellableKg         harvest minus losses
     * @param revenue            sellable quantity × price, {@code null} without a price
     * @param operatingCost      fixed cost + variable cost × sellable quantity
     * @param netBenefit         revenue - operating cost
     * @param investment         investment of the plan, {@code null} when an
     *                           additional tree has no cost entered
     * @param roiPct             net benefit × 100 / investment, the yearly return in full production
     * @param simplePaybackYears investment / net benefit, as if every year were in full production;
     *                           {@code null} when the net benefit is not above 0
     * @param paybackYear        first year whose cumulative net benefit, investment deducted, is 0
     *                           or more; {@code null} when it is not reached within the horizon
     * @param years              the plan year by year, from year 1
     */
    public record Projection(
            Integer treeCount,
            Double yieldPerTreeKg,
            Double lossRate,
            Double harvestKg,
            Double lossKg,
            Double sellableKg,
            Amount revenue,
            Amount operatingCost,
            Amount netBenefit,
            Amount investment,
            Double roiPct,
            Double simplePaybackYears,
            Integer paybackYear,
            List<Year> years) {
    }

    /**
     * One year of a plan.
     *
     * @param year                 the year, from 1
     * @param ageYears             completed years of the trees during that year
     * @param growthPhase          growth phase for that age
     * @param yieldShare           share of the full yield in that phase
     * @param harvestKg            harvest of the year
     * @param sellableKg           harvest minus losses
     * @param revenue              revenue of the year, {@code null} without a price
     * @param operatingCost        operating cost of the year
     * @param netBenefit           revenue - operating cost
     * @param cumulativeNetBenefit net benefits of the years so far, investment deducted
     */
    public record Year(
            Integer year,
            Integer ageYears,
            String growthPhase,
            Double yieldShare,
            Double harvestKg,
            Double sellableKg,
            Amount revenue,
            Amount operatingCost,
            Amount netBenefit,
            Amount cumulativeNetBenefit) {
    }

    /**
     * What a variant changes in a year of full production: variant minus
     * scenario, and its annual benefit line by line.
     *
     * @param treeCount                        trees added, negative when removed
     * @param harvestKg                        change of the harvest
     * @param sellableKg                       change of the sellable quantity
     * @param revenue                          change of the revenue
     * @param operatingCost                    change of the operating cost
     * @param roiPctPoints                     change of the return, in percentage points,
     *                                         from the rounded returns
     * @param benefitLines                     the annual benefit by cause; empty without a price
     * @param annualBenefit                    change of the net benefit: the sum of the lines
     * @param additionalInvestment             investment of the variant on top of the scenario's
     * @param additionalInvestmentPaybackYears additional investment / annual benefit; {@code null}
     *                                         without additional investment or benefit
     */
    public record Change(
            Integer treeCount,
            Double harvestKg,
            Double sellableKg,
            Amount revenue,
            Amount operatingCost,
            Double roiPctPoints,
            List<BenefitLine> benefitLines,
            Amount annualBenefit,
            Amount additionalInvestment,
            Double additionalInvestmentPaybackYears) {
    }

    /**
     * One cause of the annual benefit of a variant. The causes are applied one
     * after the other, in the order of {@code type}, and each line holds what
     * its cause adds: so the lines add up exactly to the annual benefit. A line
     * worth 0 is left out.
     *
     * <p>Types, in order: {@code TREE_COUNT} (the trees added or removed),
     * {@code YIELD_PER_TREE}, {@code LOSS_RATE}, then
     * {@code VARIABLE_OPERATING_COST}, which is negative when more is sold.
     *
     * @param type       the cause
     * @param quantityKg sellable quantity the cause adds, negative when it removes some
     * @param unitAmount price per kg for the revenue causes, variable cost per kg for the
     *                   operating cost, in the currency of the scenario
     * @param amount     what the cause adds to the net benefit
     */
    public record BenefitLine(String type, Double quantityKg, Double unitAmount, Amount amount) {
    }
}
