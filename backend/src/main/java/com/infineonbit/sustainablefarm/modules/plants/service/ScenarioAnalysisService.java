package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ScenarioVariantParams;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthPhaseYieldShareResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse.Amount;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse.BenefitLine;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse.Change;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse.Projection;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse.Year;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantScenario;
import com.infineonbit.sustainablefarm.modules.plants.exception.PlantScenarioNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PlantScenarioRepository;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.Plan;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.PlanYear;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.Variant;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.YearPhase;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.YearResult;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The cost-benefit analysis of a planting scenario, and of a variant of it,
 * computed on every read.
 *
 * <p>It reads the scenario, the yield share in effect of each growth phase,
 * the same as the yield forecast, and the EUR to XOF rate; a correction of
 * any of them counts at the next read. The arithmetic is in
 * {@link ScenarioCalculator}.
 */
@Service
@AllArgsConstructor
public class ScenarioAnalysisService {

    /** {@code basis} of every analysis: a plan, never the recorded orchard. */
    static final String SCENARIO_BASIS = "scenario";

    /**
     * Number of years of the projection when none is given: the length of the
     * present-value table of the Zalka 2025 study (cells P3 to P12).
     */
    static final int DEFAULT_HORIZON_YEARS = 10;

    private final PlantScenarioRepository plantScenarioRepository;
    private final GrowthPhaseYieldShareService growthPhaseYieldShareService;
    private final CurrencyRateRepository currencyRateRepository;

    private static BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /** An amount in both currencies, or {@code null} when it is unknown. */
    private static Amount amount(BigDecimal value, CurrencyCode currency, BigDecimal eurToXof) {
        if (value == null) {
            return null;
        }
        BigDecimal eur = CurrencyConverter.toEur(value, currency, eurToXof);
        return new Amount(CurrencyConverter.toXof(value, currency, eurToXof), toDouble(eur));
    }

    /** The optional values of the scenario that are not entered, in the order of the request. */
    private static List<String> missingInputs(PlantScenario scenario) {
        List<String> missing = new ArrayList<>();
        if (scenario.getLossRate() == null) {
            missing.add("lossRate");
        }
        if (scenario.getPricePerKg() == null) {
            missing.add("pricePerKg");
        }
        if (scenario.getVariableCostPerKg() == null) {
            missing.add("variableCostPerKg");
        }
        if (scenario.getCostPerAdditionalTree() == null) {
            missing.add("costPerAdditionalTree");
        }
        return missing;
    }

    private static Plan planOf(PlantScenario scenario, CurrencyCode currency) {
        return new Plan(
                scenario.getTreeCount(),
                decimal(scenario.getYieldPerTreeKg()),
                decimal(scenario.getLossRate()),
                scenario.getPricePerKg(),
                currency,
                scenario.getAnnualFixedCost(),
                scenario.getVariableCostPerKg(),
                scenario.getInitialInvestment());
    }

    /**
     * The variant asked for, checked against the scenario.
     *
     * @throws BusinessRuleException if it would leave no tree, or changes a loss
     *                               rate the scenario does not have
     */
    private static Variant variantOf(PlantScenario scenario, ScenarioVariantParams params) {
        int treeCountChange = params.treeCountChange() == null ? 0 : params.treeCountChange();
        if (scenario.getTreeCount() + treeCountChange < 1) {
            throw new BusinessRuleException("The scenario has " + scenario.getTreeCount()
                    + " trees, so treeCountChange must be " + (1 - scenario.getTreeCount()) + " or more");
        }
        if (params.lossRate() != null && scenario.getLossRate() == null) {
            throw new BusinessRuleException(
                    "The scenario has no loss rate: enter it before simulating another loss rate");
        }
        return new Variant(treeCountChange, decimal(params.yieldPerTreeChangePct()), decimal(params.lossRate()),
                decimal(params.additionalInvestment()));
    }

    /**
     * The growth phase of the trees of a plan in each year: in year n they are
     * n - 1 years old, as all are planted before year 1.
     */
    private static List<YearPhase> phasesOf(int horizonYears, Map<String, GrowthPhaseYieldShareResponse> shares) {
        List<YearPhase> phases = new ArrayList<>(horizonYears);
        for (int year = 1; year <= horizonYears; year++) {
            int ageYears = year - 1;
            String growthPhase = GrowthPhaseCalculator.computePhase(Period.ofYears(ageYears));
            phases.add(new YearPhase(year, ageYears, growthPhase,
                    BigDecimal.valueOf(shares.get(growthPhase).yieldShare())));
        }
        return phases;
    }

    private static Projection projection(Plan plan, List<YearPhase> phases, BigDecimal eurToXof) {
        CurrencyCode currency = plan.currency();
        YearResult full = ScenarioCalculator.fullProduction(plan);
        List<PlanYear> planYears = ScenarioCalculator.years(plan, phases);
        List<Year> years = planYears.stream()
                .map(planYear -> new Year(
                        planYear.phase().year(),
                        planYear.phase().ageYears(),
                        planYear.phase().growthPhase(),
                        planYear.phase().yieldShare().doubleValue(),
                        planYear.result().harvestKg().doubleValue(),
                        planYear.result().sellableKg().doubleValue(),
                        amount(planYear.result().revenue(), currency, eurToXof),
                        amount(planYear.result().operatingCost(), currency, eurToXof),
                        amount(planYear.result().netBenefit(), currency, eurToXof),
                        amount(planYear.cumulativeNetBenefit(), currency, eurToXof)))
                .toList();
        return new Projection(
                plan.treeCount(),
                plan.yieldPerTreeKg().doubleValue(),
                toDouble(plan.lossRate()),
                full.harvestKg().doubleValue(),
                toDouble(full.lossKg()),
                full.sellableKg().doubleValue(),
                amount(full.revenue(), currency, eurToXof),
                amount(full.operatingCost(), currency, eurToXof),
                amount(full.netBenefit(), currency, eurToXof),
                amount(plan.investment(), currency, eurToXof),
                toDouble(ScenarioCalculator.roiPct(full.netBenefit(), plan.investment())),
                toDouble(ScenarioCalculator.paybackYears(plan.investment(), full.netBenefit())),
                ScenarioCalculator.paybackYear(planYears),
                years);
    }

    private static Change toChange(ScenarioCalculator.Change change, CurrencyCode currency, BigDecimal eurToXof) {
        List<BenefitLine> lines = change.lines().stream()
                .map(line -> new BenefitLine(line.type(), line.quantityKg().doubleValue(),
                        line.unitAmount().doubleValue(), amount(line.amount(), currency, eurToXof)))
                .toList();
        return new Change(
                change.treeCountChange(),
                change.harvestKg().doubleValue(),
                change.sellableKg().doubleValue(),
                amount(change.revenue(), currency, eurToXof),
                amount(change.operatingCost(), currency, eurToXof),
                toDouble(change.roiPctPoints()),
                lines,
                amount(change.annualBenefit(), currency, eurToXof),
                amount(change.additionalInvestment(), currency, eurToXof),
                toDouble(change.additionalInvestmentPaybackYears()));
    }

    /**
     * Analyses a scenario, and a variant of it when one is asked for.
     *
     * @param id     the identifier of the scenario
     * @param params the variant and the number of years, already validated
     * @return the analysis
     * @throws PlantScenarioNotFoundException if no scenario has this identifier
     * @throws BusinessRuleException          if the variant would leave no tree, or
     *                                        changes a loss rate the scenario does not have
     */
    public ScenarioAnalysisResponse getAnalysis(Long id, ScenarioVariantParams params) {
        PlantScenario scenario = plantScenarioRepository.findById(id)
                .orElseThrow(() -> new PlantScenarioNotFoundException(id));
        Variant variant = params.hasVariant() ? variantOf(scenario, params) : null;
        int horizonYears = params.horizonYears() == null ? DEFAULT_HORIZON_YEARS : params.horizonYears();

        List<GrowthPhaseYieldShareResponse> shares = growthPhaseYieldShareService.getAllShares();
        List<YearPhase> phases = phasesOf(horizonYears, shares.stream()
                .collect(Collectors.toMap(GrowthPhaseYieldShareResponse::growthPhase, Function.identity())));
        BigDecimal eurToXof = currencyRateRepository
                .findByBaseCurrencyAndQuoteCurrency(CurrencyCode.EUR.name(), CurrencyCode.XOF.name())
                .map(CurrencyRate::getRate)
                .orElse(null);
        CurrencyCode currency = CurrencyCode.valueOf(scenario.getCurrency());

        Plan plan = planOf(scenario, currency);
        Projection variantProjection = null;
        Change change = null;
        if (variant != null) {
            BigDecimal additionalInvestment = ScenarioCalculator.additionalInvestment(
                    variant, scenario.getCostPerAdditionalTree(), currency);
            Plan variantPlan = ScenarioCalculator.apply(plan, variant, additionalInvestment);
            variantProjection = projection(variantPlan, phases, eurToXof);
            change = toChange(ScenarioCalculator.change(plan, variantPlan, additionalInvestment), currency, eurToXof);
        }
        return new ScenarioAnalysisResponse(
                scenario.getId(),
                scenario.getName(),
                SCENARIO_BASIS,
                scenario.getCurrency(),
                toDouble(eurToXof),
                horizonYears,
                missingInputs(scenario),
                shares,
                projection(plan, phases, eurToXof),
                variantProjection,
                change);
    }
}
