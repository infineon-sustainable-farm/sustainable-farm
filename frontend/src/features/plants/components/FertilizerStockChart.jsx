import { useFertilizers } from "../hooks/useFertilizers";
import { formatQuantity, formatUnit } from "../utils/format";
import OverviewCard from "./OverviewCard";
import { CARD_CAPTION } from "./overviewStyles";
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
 * name.
 *
 * Bars in primary, #0a8276 on the white card -> 4.70:1, on a light track
 * #eef2f1. The threshold mark is the mock-up's ink, #16241f, rather than its
 * orange line: 16.08:1 on the card and 3.42:1 across a bar (the orange gave
 * 2.25:1 and 1.22:1). A mark per bar, since a line from one fertilizer to the
 * next would join unrelated products.
 */
function UnitChart({ unit, fertilizers }) {
    const symbol = formatUnit(unit);
    const largest = Math.max(
        0,
        ...fertilizers.flatMap((fertilizer) => [fertilizer.currentStock, fertilizer.reorderThreshold ?? 0]),
    );
    const shareOf = (quantity) => (largest > 0 ? `${(Math.max(quantity, 0) / largest) * 100}%` : "0%");

    return (
        <div className="flex flex-col gap-2">
            <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1">
                <h3 className="text-sm font-semibold text-gray-900">{`Stock in ${symbol}`}</h3>
                <div aria-hidden="true" className={`flex flex-wrap gap-x-4 gap-y-1 ${CARD_CAPTION}`}>
                    <span className="flex items-center gap-1.5">
                        <span className="h-2.5 w-4 rounded-full bg-primary" />
                        Current stock
                    </span>
                    <span className="flex items-center gap-1.5">
                        <span className="h-3 w-0.5 bg-[#16241f]" />
                        Alert threshold
                    </span>
                </div>
            </div>
            <ul className="flex flex-col divide-y divide-gray-200">
                {fertilizers.map((fertilizer) => (
                    <li
                        key={fertilizer.id}
                        className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-4 gap-y-1 py-2 sm:grid-cols-[12rem_minmax(0,1fr)_14rem_5.5rem]"
                    >
                        <span className="text-sm font-semibold break-words text-gray-900">{fertilizer.name}</span>
                        <span className="justify-self-end sm:order-last">
                            <StockStatusBadge belowThreshold={fertilizer.belowThreshold} />
                        </span>
                        <div aria-hidden="true" className="relative col-span-2 h-2.5 rounded-full bg-[#eef2f1] sm:col-span-1">
                            <div
                                className={`h-2.5 rounded-full bg-primary ${fertilizer.currentStock > 0 ? "min-w-1" : ""}`}
                                style={{ width: shareOf(fertilizer.currentStock) }}
                            />
                            {fertilizer.reorderThreshold !== null && (
                                <div
                                    className="absolute -top-1 h-4.5 w-0.5 -translate-x-1/2 bg-[#16241f]"
                                    style={{ left: shareOf(fertilizer.reorderThreshold) }}
                                />
                            )}
                        </div>
                        <p className={`col-span-2 sm:col-span-1 ${CARD_CAPTION}`}>
                            <span className="font-semibold text-gray-900">
                                {`Stock ${formatQuantity(fertilizer.currentStock, fertilizer.unit)}`}
                            </span>
                            {fertilizer.reorderThreshold === null
                                ? " · No threshold"
                                : ` · Threshold ${formatQuantity(fertilizer.reorderThreshold, fertilizer.unit)}`}
                        </p>
                    </li>
                ))}
            </ul>
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
            caption="Stock for the whole farm, by unit."
            query={query}
            loadingText="Loading the fertilizer stock…"
            errorText="Fertilizer stock is unavailable."
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
