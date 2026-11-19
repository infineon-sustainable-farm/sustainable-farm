import { useTranslation } from "react-i18next";
import { useDebouncedValue, useScenarioAnalysis } from "../hooks/useScenarioAnalysis";
import { formatKilograms, formatNumber, formatShare, formatText } from "../utils/format";
import {
    analysisParams,
    benefitLineDetails,
    benefitLineLabel,
    currencyUnit,
    formatAmount,
    formatMoney,
    formatOtherAmount,
    formatSignedAmount,
    formatSignedOtherAmount,
    formatSignedPercent,
    formatSignedTrees,
    formatUnitPrice,
    formatYears,
    otherCurrency,
    parseLossRate,
    parseTreeCountChange,
    parseYieldChangePct,
} from "../utils/scenarioRequests";
import { ErrorBox, LoadingBox, RetryButton, SectionHeading } from "./ScenarioStatus";
import { FIGURE_LABEL_CLASS, HEAD_ROW_CLASS, NOTE_CLASS, ROW_CLASS, ROW_HEADER_CLASS, TD_CLASS, TH_CLASS, toneOf } from "./scenarioStyles";

const BOX_CLASS = "rounded-tile border border-border-subtle bg-surface-raised";

function amountTone(amount, currency) {
    return toneOf(amount?.[currency === "XOF" ? "xof" : "eur"]);
}

/** What the benefit is computed from: the assumptions of the baseline and the levers entered, with their sources. */
function assumptionItems(t, scenario, levers) {
    const currency = scenario.currency;
    const sourced = (value, source) => t("scenarios.benefit.sourced", { value, source: formatText(source) });
    const items = [
        t("scenarios.benefit.trees", { value: sourced(formatNumber(scenario.treeCount), scenario.treeCountSource) }),
        t("scenarios.benefit.yieldPerTree", { value: sourced(formatKilograms(scenario.yieldPerTreeKg), scenario.yieldSource) }),
        t("scenarios.benefit.pricePerKg", { value: sourced(formatUnitPrice(scenario.pricePerKg, currency), scenario.priceSource) }),
        scenario.lossRate === null
            ? t("scenarios.benefit.lossRateMissing")
            : t("scenarios.benefit.lossRate", { value: sourced(formatShare(scenario.lossRate), scenario.lossRateSource) }),
        scenario.variableCostPerKg === null
            ? t("scenarios.benefit.variableCostMissing")
            : t("scenarios.benefit.variableCost", {
                value: sourced(
                    t("scenarios.benefit.perKg", { value: formatUnitPrice(scenario.variableCostPerKg, currency) }),
                    scenario.variableCostSource
                ),
            }),
        scenario.costPerAdditionalTree === null
            ? t("scenarios.benefit.costPerTreeMissing")
            : t("scenarios.benefit.costPerTree", {
                value: sourced(formatMoney(scenario.costPerAdditionalTree, currency), scenario.costPerAdditionalTreeSource),
            }),
    ];
    if (levers.treeCountChange !== null) {
        items.push(t("scenarios.benefit.treesAdded", { value: formatSignedTrees(levers.treeCountChange) }));
    }
    if (levers.yieldPerTreeChangePct !== null) {
        items.push(t("scenarios.benefit.yieldChange", { value: formatSignedPercent(levers.yieldPerTreeChangePct) }));
    }
    if (levers.lossRate !== null) items.push(t("scenarios.benefit.variantLossRate", { value: formatShare(levers.lossRate) }));
    return items;
}

function AmountCell({ amount, currency, bold = false }) {
    const other = formatSignedOtherAmount(amount, currency);
    return (
        <td className={`${TD_CLASS} whitespace-nowrap ${bold ? "font-bold" : "font-semibold"} ${amountTone(amount, currency)}`}>
            {formatSignedAmount(amount, currency)}
            {other && <span className="block text-xs font-normal text-text-muted">{other}</span>}
        </td>
    );
}

function BenefitTable({ analysis }) {
    const { t } = useTranslation("plants");
    const { change, currency } = analysis;
    return (
        <div className={`overflow-x-auto px-5 py-2 ${BOX_CLASS}`}>
            <table className="w-full border-collapse text-sm">
                <thead>
                    <tr className={HEAD_ROW_CLASS}>
                        <th scope="col" className={TH_CLASS}>{t("scenarios.benefit.typeColumn")}</th>
                        <th scope="col" className={TH_CLASS}>
                            {t("scenarios.benefit.amountColumn", { unit: currencyUnit(currency) })}
                        </th>
                        <th scope="col" className={TH_CLASS}>{t("scenarios.benefit.detailsColumn")}</th>
                    </tr>
                </thead>
                <tbody>
                    {change.benefitLines.map((line) => (
                        <tr key={line.type} className={ROW_CLASS}>
                            <th scope="row" className={ROW_HEADER_CLASS}>
                                {benefitLineLabel(line)}
                            </th>
                            <AmountCell amount={line.amount} currency={currency} />
                            <td className={TD_CLASS}>{benefitLineDetails(line, currency)}</td>
                        </tr>
                    ))}
                    <tr className="bg-surface-muted">
                        <th scope="row" className={`${ROW_HEADER_CLASS} font-bold tracking-wide`}>
                            {t("scenarios.benefit.total")}
                        </th>
                        <AmountCell amount={change.annualBenefit} currency={currency} bold />
                        <td className={TD_CLASS}>{t("scenarios.benefit.totalDetails")}</td>
                    </tr>
                </tbody>
            </table>
        </div>
    );
}

/**
 * The annual benefit of the three levers applied together, in the format of
 * the kick-off example: one line per cause, the total, and the assumptions it
 * rests on, each with its source. The API applies the levers one after the
 * other (trees, yield per tree, losses), then counts the variable cost of what
 * is sold in addition, so its lines add up to the total; a line worth 0 is not
 * sent. The additional investment and its payback come below the table, as in
 * the example.
 */
export default function AnnualBenefitTable({ scenario, levers }) {
    const { t } = useTranslation("plants");
    const settledTrees = useDebouncedValue(levers.trees);
    const settledYield = useDebouncedValue(levers.yieldPct);
    const settledLoss = useDebouncedValue(levers.loss);
    const treeChange = parseTreeCountChange(settledTrees);
    const yieldChange = parseYieldChangePct(settledYield);
    const lossChange = parseLossRate(settledLoss);
    const values = {
        treeCountChange: treeChange.value,
        yieldPerTreeChangePct: yieldChange.value,
        lossRate: scenario.lossRate === null ? null : lossChange.value,
    };
    const hasLever = Object.values(values).some((value) => value !== null);
    const query = useScenarioAnalysis(scenario.id, analysisParams(values), { enabled: hasLever });
    const { data, isError, error, isFetching, refetch } = query;
    const analysis = data?.analysis;
    const currency = scenario.currency;

    return (
        <section aria-labelledby="annual-benefit-title" className="flex flex-col gap-4">
            <SectionHeading id="annual-benefit-title" title={t("scenarios.benefit.title")}>
                {t("scenarios.benefit.intro")}
            </SectionHeading>

            {!hasLever && <p className={`px-5 py-4 text-sm text-text-muted ${BOX_CLASS}`}>{t("scenarios.benefit.noLever")}</p>}

            {hasLever && !data && !isError && <LoadingBox label={t("scenarios.benefit.computing")} />}

            {hasLever && isError && (
                <ErrorBox
                    title={t("scenarios.benefit.error")}
                    message={error.message || t("scenarios.requestFailed")}
                    action={<RetryButton onClick={() => refetch()} isFetching={isFetching} />}
                />
            )}

            {hasLever && analysis && !isError && (
                <>
                    {analysis.change.annualBenefit === null ? (
                        <p className={`px-5 py-4 text-sm text-text-muted ${BOX_CLASS}`}>{t("scenarios.benefit.noPrice")}</p>
                    ) : (
                        <BenefitTable analysis={analysis} />
                    )}
                    {formatSignedOtherAmount(analysis.change.annualBenefit, currency) && (
                        <p className={`-mt-2 px-1 ${NOTE_CLASS}`}>
                            {t("scenarios.benefit.conversionNote", { unit: currencyUnit(otherCurrency(currency)) })}
                        </p>
                    )}

                    <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <div className={`px-4 py-3 ${BOX_CLASS}`}>
                            <dt className={FIGURE_LABEL_CLASS}>{t("scenarios.benefit.additionalInvestment")}</dt>
                            <dd className="mt-1 text-sm font-semibold text-text">
                                {formatAmount(analysis.change.additionalInvestment, currency)}
                                {formatOtherAmount(analysis.change.additionalInvestment, currency)
                                    && ` (${formatOtherAmount(analysis.change.additionalInvestment, currency)})`}
                            </dd>
                            {analysis.change.additionalInvestment === null && (
                                <dd className={`mt-1 ${NOTE_CLASS}`}>{t("scenarios.enterCostPerTree")}</dd>
                            )}
                        </div>
                        <div className={`px-4 py-3 ${BOX_CLASS}`}>
                            <dt className={FIGURE_LABEL_CLASS}>{t("scenarios.benefit.additionalPayback")}</dt>
                            <dd className="mt-1 text-sm font-semibold text-text">
                                {formatYears(analysis.change.additionalInvestmentPaybackYears)}
                            </dd>
                        </div>
                    </dl>

                    <div className={`px-5 py-4 ${BOX_CLASS}`}>
                        <h3 className="text-sm font-semibold text-text">{t("scenarios.benefit.assumptions")}</h3>
                        <ul className="mt-2 flex list-disc flex-col gap-1 pl-5 text-sm text-text">
                            {assumptionItems(t, scenario, values).map((item) => (
                                <li key={item}>{item}</li>
                            ))}
                        </ul>
                    </div>
                </>
            )}
        </section>
    );
}
