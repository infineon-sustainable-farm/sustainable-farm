import { useTranslation } from "react-i18next";
import { useFertilizers } from "../hooks/useFertilizers";
import { formatQuantity, formatUnit } from "../utils/format";
import OverviewCard from "./OverviewCard";
import StockStatusBadge from "./StockStatusBadge";

// kg first, then L, then a unit the API adds later, in the order the API sends them.
const UNIT_ORDER = ["KG", "L"];

function unitRank(unit) {
    const index = UNIT_ORDER.indexOf(unit);
    return index === -1 ? UNIT_ORDER.length : index;
}

/** The fertilizers by unit: kg and L never share a scale. Each group keeps the API's order, by name. */
function groupsByUnit(fertilizers) {
    const units = [...new Set(fertilizers.map((fertilizer) => fertilizer.unit))]
        .sort((first, second) => unitRank(first) - unitRank(second));
    return units.map((unit) => ({ unit, fertilizers: fertilizers.filter((fertilizer) => fertilizer.unit === unit) }));
}

/**
 * The fertilizers of one unit, one row each: its name, a bar as long as its
 * stock with a mark at its alert threshold, both on the scale of the largest
 * stock or threshold of the unit, then the stock and threshold written out and
 * the API's "Low stock" badge. Only the lengths are laid out here. From sm up
 * the four parts share one line; below, the bar and the figures go under the
 * name. Bars in the first chart colour on the chart grid track; the threshold
 * mark in the body text colour. A mark per bar, since a line from one
 * fertilizer to the next would join unrelated products.
 */
function UnitChart({ unit, fertilizers }) {
    const { t } = useTranslation("plants");
    const symbol = formatUnit(unit);
    const largest = Math.max(
        0,
        ...fertilizers.flatMap((fertilizer) => [fertilizer.currentStock, fertilizer.reorderThreshold ?? 0]),
    );
    const shareOf = (quantity) => (largest > 0 ? `${(Math.max(quantity, 0) / largest) * 100}%` : "0%");

    return (
        <div className="flex flex-col gap-2">
            <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1">
                <h3 className="text-sm font-semibold text-text">{t("overview.fertilizer.stockIn", { unit: symbol })}</h3>
                <div aria-hidden="true" className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-text-muted">
                    <span className="flex items-center gap-1.5">
                        <span className="h-2.5 w-4 rounded-pill bg-chart-1" />
                        {t("overview.fertilizer.currentStock")}
                    </span>
                    <span className="flex items-center gap-1.5">
                        <span className="h-3 w-0.5 bg-text" />
                        {t("overview.fertilizer.alertThreshold")}
                    </span>
                </div>
            </div>
            <ul className="flex flex-col">
                {fertilizers.map((fertilizer) => {
                    const threshold =
                        fertilizer.reorderThreshold === null
                            ? t("overview.fertilizer.noThreshold")
                            : t("overview.fertilizer.threshold", {
                                  quantity: formatQuantity(fertilizer.reorderThreshold, fertilizer.unit),
                              });
                    return (
                        <li
                            key={fertilizer.id}
                            className="grid grid-cols-12 items-center gap-x-4 gap-y-1 border-b border-border-subtle py-2 last:border-b-0"
                        >
                            <span className="col-span-9 text-sm font-semibold break-words text-text sm:col-span-3">
                                {fertilizer.name}
                            </span>
                            <span className="col-span-3 justify-self-end sm:order-last sm:col-span-2">
                                <StockStatusBadge belowThreshold={fertilizer.belowThreshold} />
                            </span>
                            <div aria-hidden="true" className="relative col-span-12 h-2.5 rounded-pill bg-chart-grid sm:col-span-4">
                                <div
                                    className={`h-2.5 rounded-pill bg-chart-1 ${fertilizer.currentStock > 0 ? "min-w-1" : ""}`}
                                    style={{ width: shareOf(fertilizer.currentStock) }}
                                />
                                {fertilizer.reorderThreshold !== null && (
                                    <div
                                        className="absolute top-1/2 h-4 w-0.5 -translate-x-1/2 -translate-y-1/2 bg-text"
                                        style={{ left: shareOf(fertilizer.reorderThreshold) }}
                                    />
                                )}
                            </div>
                            <p className="col-span-12 text-xs text-text-muted sm:col-span-3">
                                <span className="font-semibold text-text">
                                    {t("overview.fertilizer.stock", {
                                        quantity: formatQuantity(fertilizer.currentStock, fertilizer.unit),
                                    })}
                                </span>
                                {threshold}
                            </p>
                        </li>
                    );
                })}
            </ul>
        </div>
    );
}

/**
 * Fertilizer Stock vs Alert Threshold: the catalogue of the API with its
 * stocks, one chart per unit. A fertilizer belongs to no block, so the card
 * covers every block.
 */
export default function FertilizerStockChart() {
    const { t } = useTranslation("plants");
    const query = useFertilizers();
    const fertilizers = query.data;

    return (
        <OverviewCard
            titleId="overview-fertilizer-title"
            title={t("overview.fertilizer.title")}
            caption={t("overview.fertilizer.caption")}
            query={query}
            loadingText={t("overview.fertilizer.loading")}
            errorText={t("overview.fertilizer.error")}
        >
            {fertilizers && fertilizers.length === 0 && (
                <p className="text-sm text-text">{t("overview.fertilizer.empty")}</p>
            )}

            {fertilizers &&
                groupsByUnit(fertilizers).map((group) => (
                    <UnitChart key={group.unit} unit={group.unit} fertilizers={group.fertilizers} />
                ))}
        </OverviewCard>
    );
}
