import { useLayoutEffect, useRef, useState } from "react";
import { formatKilograms, formatMonth, formatMonthName, formatNumber } from "../utils/format";

/*
 * Geometry in CSS pixels. The chart is drawn at its real size, never scaled
 * down, so its labels keep their font size on a phone; when the months do not
 * fit, the chart scrolls sideways inside its card, as the module's tables do.
 */
const FONT_SIZE = 11;
const CHAR_WIDTH = 7; // generous width of one digit or comma at 11px
const MIN_SLOT_WIDTH = 40;
const MAX_BAR_WIDTH = 24;
const CORNER_RADIUS = 4;
const MIN_BAR_HEIGHT = 2; // a month above zero never looks like an empty one
const TOP = 34; // unit caption, then room for the tallest bar's value
const PLOT_HEIGHT = 180;
const BOTTOM = 40; // month name, and the year under January or the first month

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

/**
 * Text alternative of the chart: the period, every month above zero with its
 * value, and the total. The zero months are summed up in one phrase; each of
 * them is still listed in the monthly table.
 */
function describe(monthlyTotals, totalKg) {
    const first = formatMonth(monthlyTotals[0].month, "long");
    const last = formatMonth(monthlyTotals[monthlyTotals.length - 1].month, "long");
    const nonZero = monthlyTotals
        .filter((total) => total.expectedKg > 0)
        .map((total) => `${formatMonth(total.month, "long")} ${formatKilograms(total.expectedKg)}`);
    const zeroMonths = nonZero.length < monthlyTotals.length ? "; 0 kg in the other months" : "";
    return `Expected yield by month, ${first} to ${last}: ${nonZero.join(", ")}${zeroMonths}. `
        + `Total ${formatKilograms(totalKg)}.`;
}

/**
 * Expected yield by month, one column per month of the forecast, zero months
 * included. Every column carries its value, so the chart needs no value axis.
 *
 * The figures are the API's monthly totals, drawn as received; only the total
 * named in the text alternative is passed in by the caller.
 */
export default function YieldForecastChart({ monthlyTotals, totalKg }) {
    const [containerRef, width] = useContentWidth();

    const labels = monthlyTotals.map((total) => formatNumber(total.expectedKg));
    const longestLabel = Math.max(...labels.map((label) => label.length));
    const slotWidth = Math.max(
        MIN_SLOT_WIDTH,
        longestLabel * CHAR_WIDTH + 8,
        Math.floor(width / monthlyTotals.length),
    );
    const chartWidth = slotWidth * monthlyTotals.length;
    const barWidth = Math.min(MAX_BAR_WIDTH, Math.round(slotWidth * 0.6));
    const maxKg = Math.max(...monthlyTotals.map((total) => total.expectedKg));
    const baseline = TOP + PLOT_HEIGHT;

    return (
        <div ref={containerRef} className="overflow-x-auto">
            {width > 0 && (
                <svg
                    role="img"
                    aria-label={describe(monthlyTotals, totalKg)}
                    width={chartWidth}
                    height={baseline + BOTTOM}
                    className="block font-sans"
                >
                    <text x={0} y={FONT_SIZE} fontSize={FONT_SIZE} className="fill-gray-500">
                        kg
                    </text>

                    {monthlyTotals.map((total, index) => {
                        const center = slotWidth * index + slotWidth / 2;
                        const height = total.expectedKg > 0
                            ? Math.max(MIN_BAR_HEIGHT, (total.expectedKg / maxKg) * PLOT_HEIGHT)
                            : 0;
                        const showYear = index === 0 || total.month.endsWith("-01");
                        return (
                            <g key={total.month}>
                                {height > 0 && (
                                    <path
                                        d={barPath(center - barWidth / 2, baseline - height, barWidth, height)}
                                        className="fill-primary"
                                    />
                                )}
                                <text
                                    x={center}
                                    y={baseline - height - 6}
                                    textAnchor="middle"
                                    fontSize={FONT_SIZE}
                                    className="fill-gray-700 font-semibold"
                                >
                                    {labels[index]}
                                </text>
                                <text
                                    x={center}
                                    y={baseline + 16}
                                    textAnchor="middle"
                                    fontSize={FONT_SIZE}
                                    className="fill-gray-500"
                                >
                                    {formatMonthName(total.month)}
                                </text>
                                {showYear && (
                                    <text
                                        x={center}
                                        y={baseline + 31}
                                        textAnchor="middle"
                                        fontSize={FONT_SIZE}
                                        className="fill-gray-500"
                                    >
                                        {total.month.slice(0, 4)}
                                    </text>
                                )}
                            </g>
                        );
                    })}

                    <line
                        x1={0}
                        x2={chartWidth}
                        y1={baseline + 0.5}
                        y2={baseline + 0.5}
                        className="stroke-gray-300"
                    />
                </svg>
            )}
        </div>
    );
}
