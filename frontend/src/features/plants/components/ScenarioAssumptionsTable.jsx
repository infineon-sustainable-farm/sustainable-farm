import { Trans, useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { formatEnum, formatKilograms, formatNumber, formatShare, formatText, formatTimestamp, NO_VALUE } from "../utils/format";
import { formatMoney, formatUnitPrice } from "../utils/scenarioRequests";
import { SectionHeading } from "./ScenarioStatus";
import { HEAD_ROW_CLASS, ROW_CLASS, ROW_HEADER_CLASS, TD_CLASS, TH_CLASS } from "./scenarioStyles";

const TABLE_BOX_CLASS = "overflow-x-auto rounded-tile border border-border-subtle bg-surface-raised px-4 py-2";

/** A value followed by its unit, or the dash alone when it is not entered. */
function withUnit(t, value, text, unitKey) {
    return value === null || value === undefined ? NO_VALUE : t(`scenarios.assumptions.units.${unitKey}`, { value: text });
}

/** Translated growth phase of a yield share. */
function phaseLabel(code) {
    return formatEnum("growthPhase", code);
}

/** The assumptions of a scenario, in the order of its form, each with the source the API recorded. */
function assumptionRows(t, scenario) {
    const currency = scenario.currency;
    const currencyName = t(`scenarios.currencies.${currency}`, { defaultValue: formatText(currency) });
    return [
        { key: "variety", value: formatText(scenario.varietyName), source: null },
        { key: "trees", value: formatNumber(scenario.treeCount), source: scenario.treeCountSource },
        {
            key: "yieldPerTree",
            value: withUnit(t, scenario.yieldPerTreeKg, formatKilograms(scenario.yieldPerTreeKg), "perYearFull"),
            source: scenario.yieldSource,
        },
        {
            key: "lossRate",
            value: withUnit(t, scenario.lossRate, formatShare(scenario.lossRate), "ofHarvest"),
            source: scenario.lossRateSource,
        },
        { key: "pricePerKg", value: formatUnitPrice(scenario.pricePerKg, currency), source: scenario.priceSource },
        { key: "currency", value: currency ? currencyName : formatText(currency), source: null },
        {
            key: "fixedCost",
            value: withUnit(t, scenario.annualFixedCost, formatMoney(scenario.annualFixedCost, currency), "perYear"),
            source: scenario.annualFixedCostSource,
        },
        {
            key: "variableCost",
            value: withUnit(t, scenario.variableCostPerKg, formatUnitPrice(scenario.variableCostPerKg, currency), "perKgSold"),
            source: scenario.variableCostSource,
        },
        { key: "investment", value: formatMoney(scenario.initialInvestment, currency), source: scenario.investmentSource },
        {
            key: "costPerTree",
            value: formatMoney(scenario.costPerAdditionalTree, currency),
            source: scenario.costPerAdditionalTreeSource,
        },
    ];
}

/**
 * The assumptions of the scenario as entered, each with its source, then the
 * yield share of each growth phase that its years use, as the API gives them.
 * The shares are those of the forecast: they are corrected on Harvest &
 * Maturity, and a correction counts here at once.
 */
export default function ScenarioAssumptionsTable({ scenario, phaseShares }) {
    const { t } = useTranslation("plants");
    return (
        <section aria-labelledby="scenario-assumptions-title" className="flex flex-col gap-4">
            <SectionHeading id="scenario-assumptions-title" title={t("scenarios.assumptions.title")}>
                {t("scenarios.assumptions.intro", { date: formatTimestamp(scenario.lastUpdated) })}
            </SectionHeading>

            <div className={TABLE_BOX_CLASS}>
                <table className="w-full border-collapse text-sm">
                    <thead>
                        <tr className={HEAD_ROW_CLASS}>
                            <th scope="col" className={TH_CLASS}>{t("scenarios.assumptions.assumption")}</th>
                            <th scope="col" className={TH_CLASS}>{t("scenarios.assumptions.value")}</th>
                            <th scope="col" className={TH_CLASS}>{t("scenarios.assumptions.source")}</th>
                        </tr>
                    </thead>
                    <tbody>
                        {assumptionRows(t, scenario).map((row) => (
                            <tr key={row.key} className={ROW_CLASS}>
                                <th scope="row" className={`${ROW_HEADER_CLASS} whitespace-nowrap`}>
                                    {t(`scenarios.assumptions.rows.${row.key}`)}
                                </th>
                                <td className={TD_CLASS}>{row.value}</td>
                                <td className={`${TD_CLASS} break-words`}>{formatText(row.source)}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <div className="flex flex-col gap-3">
                <h3 className="text-sm font-semibold text-text">{t("scenarios.assumptions.sharesTitle")}</h3>
                {phaseShares ? (
                    <div className={TABLE_BOX_CLASS}>
                        <table className="w-full border-collapse text-sm whitespace-nowrap">
                            <thead>
                                <tr className={HEAD_ROW_CLASS}>
                                    <th scope="col" className={TH_CLASS}>{t("scenarios.assumptions.growthPhase")}</th>
                                    <th scope="col" className={TH_CLASS}>{t("scenarios.assumptions.age")}</th>
                                    <th scope="col" className={TH_CLASS}>{t("scenarios.assumptions.share")}</th>
                                    <th scope="col" className={TH_CLASS}>{t("scenarios.assumptions.source")}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {phaseShares.map((share) => {
                                    const phase = phaseLabel(share.code);
                                    return (
                                        <tr key={share.code} className={ROW_CLASS}>
                                            <th scope="row" className={ROW_HEADER_CLASS}>
                                                {phase}
                                            </th>
                                            <td className={TD_CLASS}>{formatText(share.yearsBand)}</td>
                                            <td className={TD_CLASS}>{formatShare(share.yieldShare)}</td>
                                            <td className={TD_CLASS}>{formatText(share.source)}</td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <p className="text-sm text-text-muted">{t("scenarios.assumptions.sharesPending")}</p>
                )}
                <p className="text-sm text-text-muted">
                    <Trans
                        ns="plants"
                        i18nKey="scenarios.assumptions.sharesNote"
                        components={{
                            page: <Link to="/plants/harvest-maturity" className="font-semibold text-text-accent underline" />,
                        }}
                    />
                </p>
            </div>
        </section>
    );
}
