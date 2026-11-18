import { useTranslation } from "react-i18next";
import { Card, Table } from "../../../shared/ui";
import { formatBlock, formatKilograms, formatMonth, formatNumber, formatText } from "../utils/format";

const CELL = "whitespace-nowrap";

/**
 * The forecast line by line: one row per month and variety with a yield above
 * zero, as the API lists them. The sources are shown as the raw codes the API
 * sends, one per reference value.
 */
export default function YieldForecastTable({ entries }) {
    const { t } = useTranslation("plants");
    const columns = [
        { key: "month", header: t("harvest.forecastTable.month"), className: CELL, cell: (e) => formatMonth(e.month) },
        { key: "block", header: t("harvest.forecastTable.block"), className: CELL, cell: (e) => formatBlock(e.blockCode) },
        { key: "variety", header: t("harvest.forecastTable.variety"), className: CELL, cell: (e) => formatText(e.varietyName) },
        { key: "trees", header: t("harvest.forecastTable.trees"), className: CELL, cell: (e) => formatNumber(e.treeCount) },
        { key: "phase", header: t("harvest.forecastTable.phase"), className: CELL, cell: (e) => formatText(e.growthPhase) },
        {
            key: "expected",
            header: t("harvest.forecastTable.expectedYield"),
            className: CELL,
            cell: (e) => formatKilograms(e.expectedKg),
        },
        {
            key: "sources",
            header: t("harvest.forecastTable.sources"),
            // The only cell allowed to wrap: three sources on one line would push the table into a
            // sideways scroll even on a wide screen.
            className: "min-w-72",
            cell: (e) =>
                t("harvest.forecastTable.sourcesValue", {
                    yield: formatText(e.yieldSource),
                    season: formatText(e.seasonSource),
                    share: formatText(e.phaseShareSource),
                }),
        },
    ];
    return (
        <Card>
            <Table
                columns={columns}
                rows={entries}
                getRowKey={(e) => `${e.month}-${e.varietyId}`}
                caption={t("harvest.forecast.title")}
                className="text-sm"
            />
        </Card>
    );
}
