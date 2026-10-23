import { formatBlock, formatKilograms, formatNumber, formatPercent, formatText } from "../utils/format";
import Value from "./Value";

function countOf(count, word) {
    return `${formatNumber(count)} ${Number(count) === 1 ? word : `${word}s`}`;
}

/**
 * The mock-up's comparison cards, one per planted variety row, in the order
 * the API sends them: actual against expected yield, and the share achieved,
 * all computed by the API.
 *
 * The bar is as wide as that share, held at 100% for the display when the
 * harvests exceed the expectation; the share itself is written beside it. One
 * colour, primary, which keeps 4.16:1 against the track (the mock-up's green
 * gave 2.99:1).
 */
export default function YieldComparisonCards({ lines }) {
    return (
        <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {lines.map((line) => (
                <li key={line.varietyId} className="rounded-xl border border-gray-200 bg-white px-4 py-3">
                    <h4 className="text-sm font-semibold text-gray-900">
                        {`${formatText(line.varietyName)} (${formatBlock(line.blockCode)})`}
                    </h4>
                    <div className="mt-2 flex items-baseline justify-between gap-3 text-sm">
                        <span className="text-gray-600">
                            {line.actualKg === null
                                ? `No harvest recorded · ${formatKilograms(line.expectedKg)} expected`
                                : `${formatNumber(line.actualKg)} of ${formatKilograms(line.expectedKg)}`}
                        </span>
                        <span className="font-bold text-gray-900">
                            <Value>{formatPercent(line.achievedPct)}</Value>
                        </span>
                    </div>
                    {line.achievedPct !== null && (
                        <div aria-hidden="true" className="mt-2 h-2 rounded-full bg-[#eef2f1]">
                            <div
                                className="h-2 rounded-full bg-primary"
                                style={{ width: `${Math.min(line.achievedPct, 100)}%` }}
                            />
                        </div>
                    )}
                    <p className="mt-2 text-xs text-gray-500">
                        {`${countOf(line.treeCount, "tree")} · ${countOf(line.harvestCount, "harvest")}`}
                    </p>
                </li>
            ))}
        </ul>
    );
}
