import { useTranslation } from "react-i18next";
import { formatCode, formatNumber, formatPercent } from "../utils/format";
import { MATURITY_STATUSES, maturityMeaning } from "../utils/overviewLabels";
import OverviewCard from "./OverviewCard";

/*
 * One colour per status, from the chart and state tokens: immature in the neutral chart colour,
 * ripe in the second chart colour, ready for harvest in the success colour, no reference in the
 * info colour. `stroke` paints the ring (an SVG attribute), `dot` the legend. A status the API adds
 * later is shown in sentence case, in the muted text colour.
 */
const SEGMENTS = {
    IMMATURE: { stroke: "var(--color-chart-3)", dot: "bg-chart-3" },
    RIPE: { stroke: "var(--color-chart-2)", dot: "bg-chart-2" },
    READY_FOR_HARVEST: { stroke: "var(--color-success-fg)", dot: "bg-success-fg" },
};
const NO_REFERENCE = { stroke: "var(--color-info-fg)", dot: "bg-info-fg" };
const OTHER = { stroke: "var(--color-text-muted)", dot: "bg-text-muted" };

// The ring is drawn in SVG units: a 112 unit square, shown at size-28.
const SIZE = 112;
const RADIUS = 44;
const STROKE = 16;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;
// White space left between two arcs, so that neighbouring hues stay apart.
const GAP = 2;

function segmentStyle(entry) {
    if (entry.maturityStatus === null) return NO_REFERENCE;
    return SEGMENTS[entry.maturityStatus] ?? OTHER;
}

function segmentLabel(t, entry) {
    if (entry.maturityStatus === null) return t("overview.maturity.labels.NO_REFERENCE");
    if (SEGMENTS[entry.maturityStatus]) return t(`overview.maturity.labels.${entry.maturityStatus}`);
    return formatCode(entry.maturityStatus);
}

/**
 * The ring: one arc per entry with trees, as long as the share the API gives
 * it, in the API's order from the top. Only where each arc starts is laid
 * out here. The legend beside it says everything the ring shows, so the ring
 * itself is hidden from screen readers.
 */
function Ring({ entries }) {
    const drawn = entries.filter((entry) => entry.sharePct > 0);
    const arcs = drawn.reduce((placed, entry) => {
        const previous = placed[placed.length - 1];
        const start = previous ? previous.start + previous.length : 0;
        return [...placed, { entry, start, length: (entry.sharePct / 100) * CIRCUMFERENCE }];
    }, []);
    const gap = arcs.length > 1 ? GAP : 0;

    return (
        <svg className="size-28" viewBox={`0 0 ${SIZE} ${SIZE}`} aria-hidden="true">
            {/* Arcs start at the top and run clockwise. */}
            <g transform={`rotate(-90 ${SIZE / 2} ${SIZE / 2})`}>
                <circle
                    cx={SIZE / 2}
                    cy={SIZE / 2}
                    r={RADIUS}
                    fill="none"
                    stroke="var(--color-chart-grid)"
                    strokeWidth={STROKE}
                />
                {arcs.map(({ entry, start, length }) => (
                    <circle
                        key={entry.maturityStatus ?? "NO_REFERENCE"}
                        cx={SIZE / 2}
                        cy={SIZE / 2}
                        r={RADIUS}
                        fill="none"
                        stroke={segmentStyle(entry).stroke}
                        strokeWidth={STROKE}
                        strokeDasharray={`${Math.max(length - gap, 0)} ${CIRCUMFERENCE}`}
                        strokeDashoffset={-start}
                    />
                ))}
            </g>
        </svg>
    );
}

/**
 * Harvest Maturity Status: the trees of each maturity status of the day, and
 * those whose variety has no reference value, with the shares the API
 * computes. Every entry is listed beside the ring, with no tree if need be.
 * Under them, what each status means, in the words of the server's
 * definitions: Ripe holds the trees whose harvest is near or on hold, so it
 * can count trees while Ready for Harvest counts none.
 */
export default function MaturityStatusDonut({ query }) {
    const { t } = useTranslation("plants");
    const overview = query.data;

    return (
        <OverviewCard
            titleId="overview-maturity-title"
            title={t("overview.maturity.title")}
            caption={t("overview.maturity.caption")}
            query={query}
            loadingText={t("overview.maturity.loading")}
            errorText={t("overview.maturity.error")}
        >
            {overview && overview.totalPlants === 0 && (
                <p className="text-sm text-text">{t("overview.maturity.empty")}</p>
            )}

            {overview && overview.totalPlants > 0 && (
                <>
                    <div className="flex items-center gap-4">
                        <div className="relative size-28 shrink-0">
                            <Ring entries={overview.maturity} />
                            <div className="absolute inset-0 flex flex-col items-center justify-center">
                                <span className="text-lg font-bold text-text">{formatNumber(overview.totalPlants)}</span>
                                <span className="text-xs text-text-muted">
                                    {t("overview.maturity.trees", { count: Number(overview.totalPlants) })}
                                </span>
                            </div>
                        </div>
                        <ul className="flex min-w-0 flex-1 flex-col gap-2">
                            {overview.maturity.map((entry) => {
                                const share = ` ${t("overview.maturity.share", {
                                    trees: t("overview.maturity.trees", { count: Number(entry.treeCount) }),
                                    percent: formatPercent(entry.sharePct),
                                })}`;
                                return (
                                    <li key={entry.maturityStatus ?? "NO_REFERENCE"} className="min-w-0">
                                        <span className="flex items-center gap-2 text-sm text-text">
                                            <span
                                                aria-hidden="true"
                                                className={`size-2.5 shrink-0 rounded-pill ${segmentStyle(entry).dot}`}
                                            />
                                            {segmentLabel(t, entry)}
                                        </span>
                                        <span className="block pl-4.5 text-xs text-text-muted">
                                            <span className="font-semibold text-text">{formatNumber(entry.treeCount)}</span>
                                            {share}
                                        </span>
                                    </li>
                                );
                            })}
                        </ul>
                    </div>
                    <ul className="flex flex-col gap-1 text-xs text-text-muted">
                        {MATURITY_STATUSES.map((status) => (
                            <li key={status}>
                                <span className="font-semibold text-text">
                                    {t("overview.maturity.meaningLabel", { label: t(`overview.maturity.labels.${status}`) })}
                                </span>
                                {t("overview.maturity.meaningSentence", { meaning: maturityMeaning(status) })}
                            </li>
                        ))}
                    </ul>
                </>
            )}
        </OverviewCard>
    );
}
