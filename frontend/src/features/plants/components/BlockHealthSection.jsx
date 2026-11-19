import { useTranslation } from "react-i18next";
import { Card, EmptyState } from "../../../shared/ui";
import { useBlockHealth } from "../hooks/useBlockHealth";
import { formatBlock, formatDate, formatPercent } from "../utils/format";
import HealthCategoryBadge from "./HealthCategoryBadge";
import { HealthSectionError, HealthSectionLoading } from "./HealthSectionStates";

// Top border and score in the hue of the category badge.
const GOOD = { border: "border-t-success-fg", score: "text-success-fg" };
const FAIR = { border: "border-t-warning-fg", score: "text-warning-fg" };
const POOR = { border: "border-t-error-fg", score: "text-error-fg" };

const CATEGORY_TONES = {
    VERY_HEALTHY: GOOD,
    HEALTHY: GOOD,
    MODERATE: FAIR,
    WEAKENED: POOR,
    SEVERE_STRESS: POOR,
};
const NEUTRAL = { border: "border-t-border-strong", score: "text-text" };

/** One inspected block: its latest score and category, and the day it was inspected. */
function BlockHealthCard({ block }) {
    const { t } = useTranslation("plants");
    const tone = CATEGORY_TONES[block.healthCategory] ?? NEUTRAL;
    return (
        <li className={`rounded-tile border border-t-4 border-border-subtle bg-surface-raised p-4 text-center ${tone.border}`}>
            <p className="text-sm font-semibold text-text-muted">{formatBlock(block.blockCode)}</p>
            <p className={`mt-1 text-2xl font-bold ${tone.score}`}>{formatPercent(block.healthScorePct)}</p>
            <div className="mt-2">
                <HealthCategoryBadge category={block.healthCategory} />
            </div>
            <p className="mt-2 text-xs text-text-muted">
                {t("health.blocks.inspectedOn", { date: formatDate(block.inspectedOn) })}
            </p>
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
    const { t } = useTranslation("plants");
    const query = useBlockHealth();
    const { data: blocks, isPending, isError } = query;
    const shown = blockCode ? (blocks ?? []).filter((block) => block.blockCode === blockCode) : blocks;

    return (
        <Card aria-labelledby="block-health-title" title={<span id="block-health-title">{t("health.blocks.title")}</span>}>
            {isPending && <HealthSectionLoading>{t("health.blocks.loading")}</HealthSectionLoading>}

            {isError && (
                <HealthSectionError
                    title={t("health.blocks.loadError")}
                    detail={t("health.requestFailed")}
                    query={query}
                />
            )}

            {!isPending && !isError && shown.length === 0 && (
                blockCode ? (
                    <EmptyState
                        className="py-8"
                        title={t("health.blocks.emptyForBlock", { block: formatBlock(blockCode) })}
                        message={t("health.selectAllBlocks")}
                    />
                ) : (
                    <EmptyState
                        className="py-8"
                        title={t("health.blocks.emptyTitle")}
                        message={t("health.blocks.emptyHint")}
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
        </Card>
    );
}
