import { useLayoutEffect, useRef, useState } from "react";
import { useScenarioAnalysis } from "../hooks/useScenarioAnalysis";
import { formatKilograms, formatNumber } from "../utils/format";
import OverviewCard from "./OverviewCard";

/*
 * Geometry in CSS pixels, as in the yield forecast chart: drawn at its real
 * size, so the labels keep their font size on a phone.
 */
const FONT_SIZE = 11;
const MIN_SLOT_WIDTH = 72;
const MAX_BAR_WIDTH = 48;
const CORNER_RADIUS = 4;
const TOP = 34; // unit caption, then room for the tallest column's value
const PLOT_HEIGHT = 180;
const BOTTOM = 26; // the step under each column
/** The mock-up's steps of the yield increase. */
const STEPS = [10, 20, 30];
// Darkened amber of the module: #a87a1e on white -> 3.6:1 for the dashed mark.
const REFERENCE_COLOUR = "#a87a1e";

/** Width of the element's content box, kept up to date as it resizes. */
function useContentWidth() {
    const ref = useRef(null);
    const [width, setWidth] = useState(0);

    useLayoutEffect(() => {
        const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width));
        observer.observe(ref.current);
        return () => observer.disconnect();
    }, []);

    return [ref, width];
}

/** A column rounded at its data end and square on the baseline. */
function barPath(x, y, width, height) {
    const radius = Math.min(CORNER_RADIUS, width / 2, height);
    const bottom = y + height;
    return `M${x},${bottom} V${y + radius} Q${x},${y} ${x + radius},${y} `
        + `H${x + width - radius} Q${x + width},${y} ${x + width},${y + radius} V${bottom} Z`;
}

/** The columns, from 0 to the largest value, each with its value written above it. */
function Chart({ columns }) {
    const [containerRef, width] = useContentWidth();
    const slotWidth = Math.max(MIN_SLOT_WIDTH, Math.floor(width / columns.length));
    const chartWidth = slotWidth * columns.length;
    const barWidth = Math.min(MAX_BAR_WIDTH, Math.round(slotWidth * 0.5));
    const maxKg = Math.max(...columns.map((column) => column.harvestKg));
    const baseline = TOP + PLOT_HEIGHT;
    const heightOf = (kg) => (maxKg > 0 ? (kg / maxKg) * PLOT_HEIGHT : 0);
    const referenceY = baseline - heightOf(columns[0].harvestKg);
    const description = "Annual yield in full production: "
        + columns.map((column) => `${column.label} ${formatKilograms(column.harvestKg)}`).join(", ") + ".";

    return (
        <div ref={containerRef} className="overflow-x-auto">
            {width > 0 && (
                <svg role="img" aria-label={description} width={chartWidth} height={baseline + BOTTOM}
                    className="block font-sans">
                    <text x={0} y={FONT_SIZE} fontSize={FONT_SIZE} className="fill-gray-500">
                        kg
                    </text>
                    {columns.map((column, index) => {
                        const center = slotWidth * index + slotWidth / 2;
                        const height = heightOf(column.harvestKg);
                        return (
                            <g key={column.label}>
                                {height > 0 && (
                                    <path
                                        d={barPath(center - barWidth / 2, baseline - height, barWidth, height)}
                                        className="fill-primary"
                                    />
                                )}
                                <text x={center} y={baseline - height - 6} textAnchor="middle" fontSize={FONT_SIZE}
                                    className="fill-gray-700 font-semibold">
                                    {formatNumber(column.harvestKg)}
                                </text>
                                <text x={center} y={baseline + 18} textAnchor="middle" fontSize={FONT_SIZE}
                                    className="fill-gray-500">
                                    {column.label}
                                </text>
                            </g>
                        );
                    })}
                    <line x1={0} x2={chartWidth} y1={referenceY} y2={referenceY} stroke={REFERENCE_COLOUR}
                        strokeWidth={2} strokeDasharray="5 4" />
                    <line x1={0} x2={chartWidth} y1={baseline + 0.5} y2={baseline + 0.5} className="stroke-gray-300" />
                </svg>
            )}
        </div>
    );
}

/**
 * The mock-up's yield growth projection: the annual yield in full production
 * at the baseline and with 10, 20 and 30 % more per tree, as the API computes
 * each variant. The columns start from 0 and carry their value, and a dashed
 * mark keeps the height of the baseline across, so the growth reads at its
 * true size.
 */
export default function ScenarioYieldChart({ scenario }) {
    const queries = [
        useScenarioAnalysis(scenario.id, { yieldPerTreeChangePct: STEPS[0] }),
        useScenarioAnalysis(scenario.id, { yieldPerTreeChangePct: STEPS[1] }),
        useScenarioAnalysis(scenario.id, { yieldPerTreeChangePct: STEPS[2] }),
    ];
    const combined = {
        isPending: queries.some((query) => query.isPending),
        isError: queries.some((query) => query.isError),
        isFetching: queries.some((query) => query.isFetching),
        refetch: () => Promise.all(queries.filter((query) => query.isError).map((query) => query.refetch())),
    };
    const columns = combined.isPending || combined.isError
        ? null
        : [
            { label: "Baseline", harvestKg: queries[0].data.analysis.baseline.harvestKg },
            ...queries.map((query, index) => ({
                label: `+${STEPS[index]}%`,
                harvestKg: query.data.analysis.variant.harvestKg,
            })),
        ];

    return (
        <OverviewCard
            titleId="yield-growth-title"
            title="Yield Growth Projection"
            caption="Annual yield in full production, at the baseline and with 10, 20 and 30% more yield per tree."
            query={combined}
            loadingText="Computing the projection…"
            errorText="The projection could not be computed"
        >
            {columns && (
                <>
                    <Chart columns={columns} />
                    <div className="flex flex-wrap gap-x-6 gap-y-2 text-xs text-gray-600">
                        <span className="flex items-center gap-2">
                            <span aria-hidden="true" className="inline-block size-3 rounded-sm bg-primary" />
                            Projected yield (kg)
                        </span>
                        <span className="flex items-center gap-2">
                            <svg aria-hidden="true" width="18" height="6">
                                <line x1="0" x2="18" y1="3" y2="3" stroke={REFERENCE_COLOUR} strokeWidth="2"
                                    strokeDasharray="5 4" />
                            </svg>
                            Baseline reference
                        </span>
                    </div>
                </>
            )}
        </OverviewCard>
    );
}
