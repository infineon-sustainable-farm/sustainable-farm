import { Apple, ChartNoAxesColumnIncreasing, Loader2, Sprout, TriangleAlert } from "lucide-react";
import { formatNumber } from "../utils/format";

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

/** The figures of the API, in the mock-up's order; its "Avg. Health Score" is left out. */
const FIGURES = [
    { field: "totalPlants", label: "Total Plants", icon: Sprout, tone: "teal" },
    { field: "inGrowthStage", label: "In Growth Stage", icon: ChartNoAxesColumnIncreasing, tone: "green" },
    { field: "readyForHarvest", label: "Ready for Harvest", icon: Apple, tone: "amber" },
    { field: "activeAlerts", label: "Active Alerts", icon: TriangleAlert, tone: "red" },
];

function FigureCard({ figure, overview }) {
    const Icon = figure.icon;
    return (
        <li className="flex items-center gap-4 rounded-xl border border-gray-200 bg-white px-5 py-4">
            <div className={`flex size-11 shrink-0 items-center justify-center rounded-lg ${TONES[figure.tone]}`}>
                <Icon size={22} aria-hidden="true" />
            </div>
            <div className="min-w-0">
                <p className="font-heading text-2xl font-bold text-gray-900">{formatNumber(overview[figure.field])}</p>
                <p className="text-sm text-gray-600">{figure.label}</p>
                {figure.field === "activeAlerts" && (
                    <p className="mt-0.5 text-xs text-gray-600">{`${formatNumber(overview.criticalAlerts)} critical`}</p>
                )}
            </div>
        </li>
    );
}

/**
 * The four figures of the day, as the API computes them for the selected
 * block; none of them is computed or added up here. They do not add up to the
 * total, and the note under them says why.
 */
export default function OverviewKpiCards({ query }) {
    const { data: overview, isPending, isError, refetch, isFetching } = query;

    if (isPending) {
        return (
            <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-500">
                <Loader2 size={18} className="animate-spin" />
                Loading the figures of the day…
            </div>
        );
    }

    if (isError) {
        return (
            <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-error/30 bg-error/5 px-5 py-4">
                <div className="flex items-start gap-3">
                    <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                    <div>
                        <p className="text-sm font-bold text-gray-900">The figures of the day could not be loaded</p>
                        {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1, as in the other sections. */}
                        <p className="mt-0.5 text-sm text-gray-600">The request to the server did not succeed.</p>
                    </div>
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
        );
    }

    return (
        <section aria-label="Figures of the day" className="flex flex-col gap-2">
            <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {FIGURES.map((figure) => (
                    <FigureCard key={figure.field} figure={figure} overview={overview} />
                ))}
            </ul>
            <p className="px-1 text-xs text-gray-500">
                Trees in the field; the nursery is not counted. A tree in production outside its harvest window is
                in neither In Growth Stage nor Ready for Harvest.
            </p>
        </section>
    );
}
