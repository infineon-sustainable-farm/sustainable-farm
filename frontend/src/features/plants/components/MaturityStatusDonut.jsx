import { formatCode, formatNumber, formatPercent } from "../utils/format";
import OverviewCard from "./OverviewCard";

/*
 * One hue per status, each at 3:1 or more on the white card: immature
 * #7d8a86 -> 3.59:1 (the mock-up's #c9d2cf gave 1.6:1), ripe #a87a1e -> 3.84:1,
 * ready for harvest #2e9e6b -> 3.38:1, no reference #3a7ca5 -> 4.56:1. The
 * words are those of the maturity badges. A status the API adds later is
 * shown in sentence case, in neutral #4a5565 -> 7.56:1.
 */
const SEGMENTS = {
    IMMATURE: { label: "Immature", colour: "#7d8a86" },
    RIPE: { label: "Ripe", colour: "#a87a1e" },
    READY_FOR_HARVEST: { label: "Ready for harvest", colour: "#2e9e6b" },
};
const NO_REFERENCE = { label: "No reference", colour: "#3a7ca5" };
const OTHER_COLOUR = "#4a5565";

const SIZE = 160;
const RADIUS = 62;
const STROKE = 22;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;
// White space left between two arcs, so that neighbouring hues stay apart.
const GAP = 2;

function segmentOf(entry) {
    if (entry.maturityStatus === null) return NO_REFERENCE;
    return SEGMENTS[entry.maturityStatus] ?? { label: formatCode(entry.maturityStatus), colour: OTHER_COLOUR };
}

function treesOf(count) {
    return `${formatNumber(count)} ${Number(count) === 1 ? "tree" : "trees"}`;
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
        <svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`} aria-hidden="true">
            {/* Arcs start at the top and run clockwise. */}
            <g transform={`rotate(-90 ${SIZE / 2} ${SIZE / 2})`}>
                <circle cx={SIZE / 2} cy={SIZE / 2} r={RADIUS} fill="none" stroke="#eef2f1" strokeWidth={STROKE} />
                {arcs.map(({ entry, start, length }) => (
                    <circle
                        key={entry.maturityStatus ?? "NO_REFERENCE"}
                        cx={SIZE / 2}
                        cy={SIZE / 2}
                        r={RADIUS}
                        fill="none"
                        stroke={segmentOf(entry).colour}
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
 * The mock-up's "Harvest Maturity Status": the trees of each maturity status
 * of the day, and those whose variety has no reference value, with the shares
 * the API computes. Every entry is listed, with no tree if need be. The
 * mock-up's "Harvested" has no status yet: the entries follow the API.
 */
export default function MaturityStatusDonut({ query }) {
    const overview = query.data;

    return (
        <OverviewCard
            titleId="overview-maturity-title"
            title="Harvest Maturity Status"
            caption="The maturity status of today, as on Harvest & Maturity."
            query={query}
            loadingText="Loading the maturity status…"
            errorText="The maturity status could not be loaded"
        >
            {overview && overview.totalPlants === 0 && (
                <p className="text-sm text-gray-700">No planted tree to show.</p>
            )}

            {overview && overview.totalPlants > 0 && (
                <>
                    <div className="flex flex-col items-center gap-5 sm:flex-row lg:flex-col">
                        <div className="relative shrink-0" style={{ width: SIZE, height: SIZE }}>
                            <Ring entries={overview.maturity} />
                            <div className="absolute inset-0 flex flex-col items-center justify-center">
                                <span className="font-heading text-2xl font-bold text-gray-900">
                                    {formatNumber(overview.totalPlants)}
                                </span>
                                <span className="text-xs text-gray-600">
                                    {Number(overview.totalPlants) === 1 ? "tree" : "trees"}
                                </span>
                            </div>
                        </div>
                        <ul className="flex w-full flex-col gap-2">
                            {overview.maturity.map((entry) => {
                                const segment = segmentOf(entry);
                                return (
                                    <li
                                        key={entry.maturityStatus ?? "NO_REFERENCE"}
                                        className="flex items-center justify-between gap-3 text-sm"
                                    >
                                        <span className="flex items-center gap-2 text-gray-700">
                                            <span
                                                aria-hidden="true"
                                                className="size-2.5 shrink-0 rounded-full"
                                                style={{ backgroundColor: segment.colour }}
                                            />
                                            {segment.label}
                                        </span>
                                        <span className="text-right whitespace-nowrap">
                                            <span className="font-semibold text-gray-900">
                                                {formatPercent(entry.sharePct)}
                                            </span>
                                            <span className="text-gray-600">{` · ${treesOf(entry.treeCount)}`}</span>
                                        </span>
                                    </li>
                                );
                            })}
                        </ul>
                    </div>
                    <p className="text-xs text-gray-500">
                        Immature counts every tree whose harvest window is not near, young or not, so it differs from
                        In Growth Stage.
                    </p>
                </>
            )}
        </OverviewCard>
    );
}
