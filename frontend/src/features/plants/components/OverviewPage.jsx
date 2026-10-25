import { useEffect, useState } from "react";
import { usePlantsOverview } from "../hooks/usePlantsOverview";
import { useVarietyBlocks } from "../hooks/useVarieties";
import { formatDate } from "../utils/format";
import BlockFilter from "./BlockFilter";
import OverviewAlertsSection from "./OverviewAlertsSection";
import OverviewKpiCards from "./OverviewKpiCards";

/**
 * Overview, the home of the module: the figures of the day for the selected
 * block, then every alert of the day, the most urgent first. Every figure
 * comes from the API; nothing is counted or summed here.
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
                <header>
                    <h2 className="font-heading text-3xl font-bold text-gray-900">Overview</h2>
                    <p className="mt-2 max-w-xl text-sm text-gray-500">
                        Trees, alerts, yield, maturity, nursery and fertilizer stock at a glance.
                    </p>
                    {overviewQuery.data && (
                        <p className="mt-1 text-sm text-gray-600">{`As of ${formatDate(overviewQuery.data.today)}`}</p>
                    )}
                </header>

                <BlockFilter blocks={blocks ?? []} value={selectedBlock} onChange={setSelectedBlock} />

                <OverviewKpiCards query={overviewQuery} />

                <OverviewAlertsSection blockCode={selectedBlock || null} />
            </div>
        </div>
    );
}
