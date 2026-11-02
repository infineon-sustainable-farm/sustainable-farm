import { useEffect, useState } from "react";
import { usePlantsOverview } from "../hooks/usePlantsOverview";
import { useVarietyBlocks } from "../hooks/useVarieties";
import { formatDate } from "../utils/format";
import BlockFilter from "./BlockFilter";
import BlockYieldChart from "./BlockYieldChart";
import FertilizerStockChart from "./FertilizerStockChart";
import MaturityStatusDonut from "./MaturityStatusDonut";
import NurserySurvivalChart from "./NurserySurvivalChart";
import OverviewAlertsSection from "./OverviewAlertsSection";
import OverviewKpiCards from "./OverviewKpiCards";

/**
 * Overview, the home of the module: the figures of the day for the selected
 * block, then every alert of the day, the most urgent first, then the
 * mock-up's charts. Every figure comes from the API; nothing is counted or
 * summed here, and the charts only lay the figures out. The block filter
 * drives the figures, the alerts, the yield and the maturity status; the
 * nursery batches and the fertilizers cover every block, and say so.
 *
 * The mock-up's title names the farm, its variety and its area, which would
 * hard-code farm data into the interface: the page says "Overview", as its
 * entry in the sidebar, and the day the API computed the figures for. The
 * mock-up's Home buttons stay out of the other screens: the sidebar leads here.
 */
export default function OverviewPage() {
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");

    useEffect(() => {
        document.title = "Overview — Plants";
    }, []);

    const { data: blocks } = useVarietyBlocks();
    const overviewQuery = usePlantsOverview({ blockCode: selectedBlock || null });

    return (
        <div className="min-h-dvh bg-gray-50">
            {/* pt-20 under md: the fixed menu button would otherwise cover the title on a phone. */}
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 pt-20 pb-8 sm:px-6 md:pt-8">
                {/* mb-2 on top of gap-6: 32 px between the header and the content. The filter sits on the
                    title row from lg up, and under the title below. */}
                <header className="mb-2 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div>
                        <h1 className="font-heading text-2xl font-bold text-gray-900">Overview</h1>
                        <p className="mt-2 text-sm text-gray-600">Your orchard today.</p>
                        {/* gray-600: #4a5565 on the gray-50 page -> 7.23:1. */}
                        {overviewQuery.data && (
                            <p className="mt-1 text-xs text-gray-600">{`As of ${formatDate(overviewQuery.data.today)}`}</p>
                        )}
                    </div>
                    <BlockFilter bare blocks={blocks ?? []} value={selectedBlock} onChange={setSelectedBlock} />
                </header>

                <OverviewKpiCards query={overviewQuery} />

                <OverviewAlertsSection blockCode={selectedBlock || null} />

                {/* From lg up, the three cards share two grid rows through subgrid: the tallest header sets
                    the first row, so the three charts start at the same height. */}
                <div className="grid grid-cols-1 gap-6 lg:grid-cols-3 lg:grid-rows-[auto_1fr] lg:gap-y-4">
                    <BlockYieldChart blockCode={selectedBlock || null} />
                    <MaturityStatusDonut query={overviewQuery} />
                    <NurserySurvivalChart />
                </div>

                <FertilizerStockChart />
            </div>
        </div>
    );
}
