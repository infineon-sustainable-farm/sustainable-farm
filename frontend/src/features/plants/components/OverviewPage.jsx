import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { PageHeader } from "../../../shared/ui";
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
 * charts. Every figure comes from the API; nothing is counted or summed here,
 * and the charts only lay the figures out. The block filter drives the
 * figures, the alerts, the yield and the maturity status; the nursery batches
 * and the fertilizers cover every block, and say so.
 *
 * The title says "Overview", as its entry in the sidebar, and the day the API
 * computed the figures for; no farm name is hard-coded.
 */
export default function OverviewPage() {
    const { t } = useTranslation("plants");
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");

    const documentTitle = t("overview.documentTitle");
    useEffect(() => {
        document.title = documentTitle;
    }, [documentTitle]);

    const { data: blocks } = useVarietyBlocks();
    const overviewQuery = usePlantsOverview({ blockCode: selectedBlock || null });
    const asOf = overviewQuery.data
        ? ` ${t("overview.asOf", { date: formatDate(overviewQuery.data.today) })}`
        : null;

    return (
        <div className="p-6 space-y-6">
            <PageHeader
                title={t("overview.title")}
                subtitle={
                    <>
                        {t("overview.subtitle")}
                        {asOf && <span className="text-xs">{asOf}</span>}
                    </>
                }
                actions={<BlockFilter bare blocks={blocks ?? []} value={selectedBlock} onChange={setSelectedBlock} />}
            />

            <OverviewKpiCards query={overviewQuery} />

            <OverviewAlertsSection blockCode={selectedBlock || null} />

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
                <BlockYieldChart blockCode={selectedBlock || null} />
                <MaturityStatusDonut query={overviewQuery} />
                <NurserySurvivalChart />
            </div>

            <FertilizerStockChart />
        </div>
    );
}
