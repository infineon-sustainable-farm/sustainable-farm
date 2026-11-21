import { useTranslation } from "react-i18next";
import { Card, Table } from "../../../shared/ui";
import { formatBlock, formatQuantity, formatText } from "../utils/format";
import { formatSignedPercent, formatSignedQuantity } from "../utils/fertilizerPlans";
import CellNote from "./CellNote";
import PlanCost from "./PlanCost";


/**
 * Planned and applied quantities by block and fertilizer, in the order the
 * API sends them. Every figure is the API's: the totals, the gap and both
 * costs at the last purchase price. The gap is shown as it is, with its sign
 * and no judgement: the application recommends no rate. What is still to do
 * sits under the planned quantity, what was applied without a plan under the
 * applied one, and the gap as a percentage under the gap.
 */
export default function FertilizerPlanComparisonTable({ rows }) {
    const { t } = useTranslation("plants");
    const header = (key) => t(`fertilizer.plans.comparison.columns.${key}`);
    const numeric = { align: "right", className: "whitespace-nowrap align-top" };

    const columns = [
        {
            key: "blockCode",
            header: header("block"),
            className: "whitespace-nowrap align-top font-semibold",
            cell: (row) => formatBlock(row.blockCode),
        },
        { key: "fertilizerName", header: header("fertilizer"), className: "align-top", cell: (row) => formatText(row.fertilizerName) },
        {
            key: "plannedQuantity",
            header: header("planned"),
            ...numeric,
            cell: (row) => (
                <>
                    {formatQuantity(row.plannedQuantity, row.unit)}
                    {row.remainingQuantity > 0 && (
                        <CellNote>
                            {t("fertilizer.plans.comparison.stillToDo", { quantity: formatQuantity(row.remainingQuantity, row.unit) })}
                        </CellNote>
                    )}
                </>
            ),
        },
        {
            key: "appliedQuantity",
            header: header("applied"),
            ...numeric,
            cell: (row) => (
                <>
                    {formatQuantity(row.appliedQuantity, row.unit)}
                    {row.unplannedQuantity > 0 && (
                        <CellNote>
                            {t("fertilizer.plans.comparison.notPlanned", { quantity: formatQuantity(row.unplannedQuantity, row.unit) })}
                        </CellNote>
                    )}
                </>
            ),
        },
        {
            key: "gapQuantity",
            header: header("gap"),
            ...numeric,
            cell: (row) => {
                const gapPercent = formatSignedPercent(row.gapPercent);
                return (
                    <>
                        {formatSignedQuantity(row.gapQuantity, row.unit)}
                        {gapPercent && <CellNote>{gapPercent}</CellNote>}
                    </>
                );
            },
        },
        {
            key: "plannedCost",
            header: header("plannedCost"),
            ...numeric,
            cell: (row) => <PlanCost xof={row.plannedCostXof} eur={row.plannedCostEur} />,
        },
        {
            key: "appliedCost",
            header: header("appliedCost"),
            ...numeric,
            cell: (row) => <PlanCost xof={row.appliedCostXof} eur={row.appliedCostEur} />,
        },
    ];

    return (
        <Card>
            <Table
                caption={t("fertilizer.plans.comparison.title")}
                columns={columns}
                rows={rows}
                getRowKey={(row) => `${row.blockCode}-${row.fertilizerId}`}
            />
        </Card>
    );
}
