import { Link } from "react-router-dom";
import { useYieldComparison } from "../hooks/useYieldComparison";
import { formatBlock, formatKilograms, formatMonth, formatNumber } from "../utils/format";
import OverviewCard from "./OverviewCard";

/*
 * Expected in the mock-up's orange, darkened so a bar keeps 3:1 on the white
 * card: #a87a1e -> 3.84:1 (#d9a441 gave 2.25:1). Actual in primary, #0a8276 ->
 * 4.70:1. Each bar also writes what it is and its value, so the colours are
 * never the only cue.
 */
const EXPECTED_BAR = "bg-[#a87a1e]";
const ACTUAL_BAR = "bg-primary";

function countOf(count, word) {
    return `${formatNumber(count)} ${Number(count) === 1 ? word : `${word}s`}`;
}

/**
 * One bar on the card's common scale: as long as its yield against the largest
 * yield written on the card. A yield above 0 always shows a sliver.
 */
function YieldBar({ label, kg, largestKg, barClass }) {
    const width = largestKg > 0 ? `${(kg / largestKg) * 100}%` : "0%";
    return (
        <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3">
            <div aria-hidden="true" className="h-3">
                <div className={`h-3 rounded-full ${barClass} ${kg > 0 ? "min-w-0.5" : ""}`} style={{ width }} />
            </div>
            <span className="text-xs whitespace-nowrap text-gray-700">{`${label} ${formatKilograms(kg)}`}</span>
        </div>
    );
}

function Legend() {
    return (
        <div aria-hidden="true" className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-600">
            <span className="flex items-center gap-1.5">
                <span className={`size-2.5 rounded-full ${EXPECTED_BAR}`} />
                Expected yield
            </span>
            <span className="flex items-center gap-1.5">
                <span className={`size-2.5 rounded-full ${ACTUAL_BAR}`} />
                Actual yield
            </span>
        </div>
    );
}

/**
 * The mock-up's "Expected vs Actual Yield by Block": the block lines of the
 * yield comparison, in the API's order, over the API's default period, the
 * calendar year. Each block has its expected bar and, once a harvest is
 * recorded, its actual one; both are written out in full. Every figure is
 * the API's, and only the length of the bars is laid out here.
 */
export default function BlockYieldChart({ blockCode }) {
    const query = useYieldComparison({ blockCode });
    const comparison = query.data;
    const caption = comparison
        ? `${countOf(comparison.months, "month")} from ${formatMonth(comparison.from, "long")}: the recorded harvests against the yield the forecast expects.`
        : null;
    // The common scale of the bars: the largest yield written on the card.
    const largestKg = comparison
        ? Math.max(0, ...comparison.blocks.flatMap((block) => [block.expectedKg, block.actualKg ?? 0]))
        : 0;

    return (
        <OverviewCard
            titleId="overview-yield-title"
            title="Expected vs Actual Yield by Block"
            caption={caption}
            query={query}
            loadingText="Loading the yield by block…"
            errorText="The yield by block could not be loaded"
        >
            {comparison && comparison.blocks.length === 0 && (
                <p className="text-sm text-gray-700">No planted variety with a reference value over this period.</p>
            )}

            {comparison && comparison.blocks.length > 0 && (
                <>
                    <Legend />
                    <ul className="flex flex-col gap-4">
                        {comparison.blocks.map((block) => (
                            <li key={`${block.farmId}-${block.blockCode}`}>
                                <p className="text-sm font-semibold text-gray-900">{formatBlock(block.blockCode)}</p>
                                <div className="mt-1.5 flex flex-col gap-1.5">
                                    <YieldBar
                                        label="Expected"
                                        kg={block.expectedKg}
                                        largestKg={largestKg}
                                        barClass={EXPECTED_BAR}
                                    />
                                    {block.actualKg === null ? (
                                        <p className="text-xs text-gray-600">Actual: awaiting harvest</p>
                                    ) : (
                                        <YieldBar
                                            label="Actual"
                                            kg={block.actualKg}
                                            largestKg={largestKg}
                                            barClass={ACTUAL_BAR}
                                        />
                                    )}
                                </div>
                            </li>
                        ))}
                    </ul>
                </>
            )}

            {comparison && comparison.notCompared.length > 0 && (
                <p className="text-xs text-gray-500">
                    Planted varieties without a reference value are not compared.
                </p>
            )}

            <Link
                to="/plants/harvest-maturity"
                className="text-sm font-semibold text-primary-dark underline underline-offset-2 hover:no-underline"
            >
                See Harvest &amp; Maturity
            </Link>
        </OverviewCard>
    );
}
