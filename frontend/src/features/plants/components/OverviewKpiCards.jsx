import { Apple, ChartNoAxesColumnIncreasing, Sprout, TriangleAlert } from "lucide-react";
import { formatNumber } from "../utils/format";
import { MATURITY_MEANINGS } from "../utils/overviewLabels";
import { CARD, ERROR_BOX, ICON_SIZE, PRIMARY_BUTTON, SKELETON } from "./plantsStyles";

/*
 * The mock-up's four tinted icon chips, in the hues of the module's badges.
 * The icons repeat the label beside them, so they are hidden from screen readers.
 */
const TONES = {
    teal: "bg-[#e4f2f0] text-primary-dark",
    green: "bg-[#e5f7ef] text-[#257e55]",
    amber: "bg-[#f8efde] text-[#8e661c]",
    red: "bg-[#f7e9e7] text-[#9a3f32]",
};

/**
 * The figures of the API, in the mock-up's order; its "Avg. Health Score" is
 * left out. Each says in one line what it counts, in the words of the API's
 * definitions: Ready for Harvest is the "Ready for harvest" of the ring, not
 * its "Ripe", which the ring explains.
 */
const FIGURES = [
    { field: "totalPlants", label: "Total Plants", note: () => "Field trees, nursery excluded", icon: Sprout, tone: "teal" },
    {
        field: "inGrowthStage",
        label: "In Growth Stage",
        note: () => "Establishment, 0–2 years",
        icon: ChartNoAxesColumnIncreasing,
        tone: "green",
    },
    {
        field: "readyForHarvest",
        label: "Ready for Harvest",
        note: () => MATURITY_MEANINGS.READY_FOR_HARVEST,
        icon: Apple,
        tone: "amber",
    },
    {
        field: "activeAlerts",
        label: "Active Alerts",
        note: (overview) => `${formatNumber(overview.criticalAlerts)} critical`,
        icon: TriangleAlert,
        tone: "red",
    },
];

/**
 * One figure, aligned left: value 24 px, label 12 px gray, then what it counts
 * in 12 px gray. gray-700 #364153 -> 10.30:1 and gray-600 #4a5565 -> 7.56:1 on
 * white.
 */
function FigureCard({ figure, overview }) {
    const Icon = figure.icon;
    return (
        // Two tiles per row on a phone, with the chip above the value; beside it from sm up.
        <li className={`flex flex-col items-start gap-2 sm:flex-row sm:gap-3 ${CARD}`}>
            <div className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${TONES[figure.tone]}`}>
                <Icon size={ICON_SIZE} aria-hidden="true" />
            </div>
            <div className="min-w-0">
                <p className="font-heading text-2xl leading-8 font-bold text-on-background">
                    {formatNumber(overview[figure.field])}
                </p>
                <p className="text-xs font-semibold text-gray-700">{figure.label}</p>
                <p className="text-xs text-gray-600">{figure.note(overview)}</p>
            </div>
        </li>
    );
}

/**
 * The four figures of the day, as the API computes them for the selected
 * block; none of them is computed or added up here. They do not add up to the
 * total; the line under each value says which trees it counts.
 */
export default function OverviewKpiCards({ query }) {
    const { data: overview, isPending, isError, refetch, isFetching } = query;

    if (isPending) {
        // Four tiles of gray blocks, laid out as the figures will be, so nothing moves when they arrive.
        return (
            <div role="status" className="grid grid-cols-2 gap-4 lg:grid-cols-4">
                <span className="sr-only">Loading the figures of the day…</span>
                {FIGURES.map((figure) => (
                    <div
                        key={figure.field}
                        aria-hidden="true"
                        className={`flex flex-col items-start gap-2 sm:flex-row sm:gap-3 ${CARD}`}
                    >
                        <div className={`size-8 shrink-0 ${SKELETON}`} />
                        <div className="flex w-full flex-col gap-2">
                            <div className={`h-6 w-16 ${SKELETON}`} />
                            <div className={`h-3 w-24 ${SKELETON}`} />
                            <div className={`h-3 w-32 max-w-full ${SKELETON}`} />
                        </div>
                    </div>
                ))}
            </div>
        );
    }

    if (isError) {
        return (
            <div className={ERROR_BOX}>
                <div className="flex items-start gap-2">
                    <TriangleAlert size={ICON_SIZE} className="mt-0.5 shrink-0 text-error" />
                    <div>
                        <p className="text-sm font-bold text-on-background">Today's figures are unavailable.</p>
                        <p className="mt-0.5 text-sm text-gray-600">Check your connection, then try again.</p>
                    </div>
                </div>
                <button type="button" onClick={() => refetch()} disabled={isFetching} className={PRIMARY_BUTTON}>
                    {isFetching ? "Retrying…" : "Retry"}
                </button>
            </div>
        );
    }

    return (
        <section aria-label="Figures of the day">
            <ul className="grid grid-cols-2 gap-4 lg:grid-cols-4">
                {FIGURES.map((figure) => (
                    <FigureCard key={figure.field} figure={figure} overview={overview} />
                ))}
            </ul>
        </section>
    );
}
