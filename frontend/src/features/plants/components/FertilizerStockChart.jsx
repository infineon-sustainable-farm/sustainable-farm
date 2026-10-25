import { useFertilizers } from "../hooks/useFertilizers";
import { formatQuantity, formatUnit } from "../utils/format";
import OverviewCard from "./OverviewCard";
import StockStatusBadge from "./StockStatusBadge";

const TRACK_HEIGHT = 160; // px

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
 * The fertilizers of one unit: a bar per fertilizer as tall as its stock, and
 * a mark at its alert threshold, both on the scale of the largest stock or
 * threshold of the unit. The stock, the threshold and the "Low stock" badge
 * are the API's, written out under each bar; only the heights are laid out
 * here.
 *
 * Bars in primary, #0a8276 on the white card -> 4.70:1. The threshold mark is
 * the mock-up's ink, #16241f, rather than its orange line: 16.08:1 on the card
 * and 3.42:1 across a bar (the orange gave 2.25:1 and 1.22:1). A mark per bar,
 * since a line from one fertilizer to the next would join unrelated products.
 */
function UnitChart({ unit, fertilizers }) {
    const symbol = formatUnit(unit);
    const largest = Math.max(
        0,
        ...fertilizers.flatMap((fertilizer) => [fertilizer.currentStock, fertilizer.reorderThreshold ?? 0]),
    );
    const heightOf = (quantity) => (largest > 0 ? `${(Math.max(quantity, 0) / largest) * 100}%` : "0%");

    return (
        <div className="flex flex-col gap-3">
            <h4 className="text-sm font-semibold text-gray-900">{`Stock in ${symbol}`}</h4>
            <div aria-hidden="true" className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-600">
                <span className="flex items-center gap-1.5">
                    <span className="size-2.5 rounded-sm bg-primary" />
                    {`Current stock (${symbol})`}
                </span>
                <span className="flex items-center gap-1.5">
                    <span className="h-0.5 w-4 bg-[#16241f]" />
                    {`Alert threshold (${symbol})`}
                </span>
            </div>
            <div className="relative overflow-x-auto">
                <ul className="flex min-w-max items-end gap-4 pt-2 pb-1">
                    {fertilizers.map((fertilizer) => (
                        <li key={fertilizer.id} className="flex w-28 shrink-0 flex-col items-center gap-1 text-center">
                            <span className="text-xs font-semibold break-words text-gray-900">{fertilizer.name}</span>
                            <span className="text-xs text-gray-600">
                                {fertilizer.reorderThreshold === null
                                    ? "No threshold"
                                    : `Threshold ${formatQuantity(fertilizer.reorderThreshold, fertilizer.unit)}`}
                            </span>
                            <StockStatusBadge belowThreshold={fertilizer.belowThreshold} />
                            <span className="order-first text-xs font-semibold text-gray-900">
                                {formatQuantity(fertilizer.currentStock, fertilizer.unit)}
                                <span className="sr-only"> in stock</span>
                            </span>
                            <div
                                aria-hidden="true"
                                className="relative order-first w-10 border-b border-gray-300"
                                style={{ height: TRACK_HEIGHT }}
                            >
                                <div
                                    className="absolute inset-x-0 bottom-0 rounded-t-md bg-primary"
                                    style={{ height: heightOf(fertilizer.currentStock) }}
                                />
                                {fertilizer.reorderThreshold !== null && (
                                    <div
                                        className="absolute -inset-x-2 h-0.5 translate-y-1/2 bg-[#16241f]"
                                        style={{ bottom: heightOf(fertilizer.reorderThreshold) }}
                                    />
                                )}
                            </div>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
}

/**
 * The mock-up's "Fertilizer Stock vs Alert Threshold": the catalogue of the
 * API with its stocks, one chart per unit. A fertilizer belongs to no block,
 * so the card covers every block.
 */
export default function FertilizerStockChart() {
    const query = useFertilizers();
    const fertilizers = query.data;

    return (
        <OverviewCard
            titleId="overview-fertilizer-title"
            title="Fertilizer Stock vs Alert Threshold"
            caption="Every block: a fertilizer belongs to no block. One chart per unit, so kg and L never share a scale."
            query={query}
            loadingText="Loading the fertilizer stock…"
            errorText="The fertilizer stock could not be loaded"
        >
            {fertilizers && fertilizers.length === 0 && (
                <p className="text-sm text-gray-700">No fertilizer in the catalogue yet.</p>
            )}

            {fertilizers &&
                groupsByUnit(fertilizers).map((group) => (
                    <UnitChart key={group.unit} unit={group.unit} fertilizers={group.fertilizers} />
                ))}
        </OverviewCard>
    );
}
