import { Loader2, TriangleAlert } from "lucide-react";
import { useBlockHealth } from "../hooks/useBlockHealth";
import { formatBlock, formatDate } from "../utils/format";
import HealthCategoryBadge from "./HealthCategoryBadge";
import PlantsEmptyState from "./PlantsEmptyState";

/*
 * Top border in the shared token, as in the mock-up; score in the darkened hue
 * of the badges: #257e55, #8e661c and #9a3f32 on white -> 5.01, 5.16 and
 * 6.71:1. The mock-up's green score (#2e9e6b) is 3.38:1.
 */
const GOOD = { border: "border-t-success", score: "text-[#257e55]" };
const FAIR = { border: "border-t-warning", score: "text-[#8e661c]" };
const POOR = { border: "border-t-error", score: "text-[#9a3f32]" };

const CATEGORY_TONES = {
    VERY_HEALTHY: GOOD,
    HEALTHY: GOOD,
    MODERATE: FAIR,
    WEAKENED: POOR,
    SEVERE_STRESS: POOR,
};
const NEUTRAL = { border: "border-t-gray-300", score: "text-gray-900" };

/** One inspected block: its latest score and category, and the day it was inspected. */
function BlockHealthCard({ block }) {
    const tone = CATEGORY_TONES[block.healthCategory] ?? NEUTRAL;
    return (
        <li className={`rounded-xl border border-t-4 border-gray-200 bg-white px-4 py-4 text-center ${tone.border}`}>
            <p className="text-sm font-semibold text-gray-600">{formatBlock(block.blockCode)}</p>
            <p className={`font-heading mt-1 text-3xl font-bold ${tone.score}`}>{`${block.healthScorePct}%`}</p>
            <div className="mt-2">
                <HealthCategoryBadge category={block.healthCategory} />
            </div>
            <p className="mt-2 text-xs text-gray-600">{`Inspected on ${formatDate(block.inspectedOn)}`}</p>
        </li>
    );
}

/**
 * Health by block: the score of the latest inspection of each inspected block,
 * with its category, both from the API.
 *
 * The API takes no block filter for this list, so the selected block is
 * picked out of it here; nothing else is computed. A block never inspected
 * has no card.
 *
 * `recordAction` is the page's "Record an inspection" button, offered again
 * when no block is inspected at all.
 */
export default function BlockHealthSection({ blockCode, recordAction }) {
    const { data: blocks, isPending, isError, refetch, isFetching } = useBlockHealth();
    const shown = blockCode ? (blocks ?? []).filter((block) => block.blockCode === blockCode) : blocks;

    return (
        <section aria-labelledby="block-health-title" className="flex flex-col gap-4">
            <h3 id="block-health-title" className="font-heading text-base font-bold text-gray-900">
                Health by block
            </h3>

            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading the health of the blocks…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-base font-bold text-gray-900">
                            The health of the blocks could not be loaded
                        </p>
                        {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1; gray-500 gave 4.32:1. */}
                        <p className="mt-1 text-sm text-gray-600">
                            The request to the server did not succeed.
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {!isPending && !isError && shown.length === 0 && (
                blockCode ? (
                    <PlantsEmptyState
                        title={`${formatBlock(blockCode)} has not been inspected yet`}
                        hint="Select “All blocks” to see the inspected blocks."
                    />
                ) : (
                    <PlantsEmptyState
                        title="No block inspected yet"
                        hint="The health of a block comes from its latest inspection."
                        action={recordAction}
                    />
                )
            )}

            {!isPending && !isError && shown.length > 0 && (
                <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
                    {shown.map((block) => (
                        <BlockHealthCard key={block.inspectionId} block={block} />
                    ))}
                </ul>
            )}
        </section>
    );
}
