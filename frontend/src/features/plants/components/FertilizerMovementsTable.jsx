import { useTranslation } from "react-i18next";
import { Card, Table } from "../../../shared/ui";
import CellNote from "./CellNote";
import {
    formatBlock,
    formatDate,
    formatEnum,
    formatEur,
    formatQuantity,
    formatText,
    formatXof,
    NO_VALUE,
} from "../utils/format";

/**
 * What the movement records besides its quantity: the supplier of a purchase,
 * the applicator of an application followed by its method when one was
 * entered, the reason of a loss.
 */
function movementDetails(movement, t) {
    switch (movement.movementType) {
        case "PURCHASE":
            return formatText(movement.supplier);
        case "APPLICATION": {
            const applicator = formatText(movement.applicator);
            const method = formatText(movement.method);
            return method === NO_VALUE ? applicator : t("fertilizer.movements.details", { applicator, method });
        }
        case "LOSS":
            return formatText(movement.reason);
        default:
            return NO_VALUE;
    }
}

/**
 * Fertilizer movements, one row per purchase, application or loss, in the
 * order the API sends them, on seven columns: the source sits in small muted
 * text under the details, and the cost in euros under the cost in FCFA. Both
 * costs are the API's own conversion; a movement without a cost shows one dash.
 */
export default function FertilizerMovementsTable({ movements }) {
    const { t } = useTranslation("plants");
    const header = (key) => t(`fertilizer.movements.columns.${key}`);
    const columns = [
        { key: "movementDate", header: header("date"), cell: (row) => formatDate(row.movementDate) },
        { key: "fertilizerName", header: header("fertilizer"), cell: (row) => formatText(row.fertilizerName) },
        { key: "movementType", header: header("type"), cell: (row) => formatEnum("movementType", row.movementType) },
        { key: "quantity", align: "right", header: header("quantity"), cell: (row) => formatQuantity(row.quantity, row.unit) },
        { key: "blockCode", header: header("block"), cell: (row) => formatBlock(row.blockCode) },
        {
            key: "details",
            header: header("details"),
            cell: (row) => (
                <>
                    {movementDetails(row, t)}
                    <CellNote>{t("fertilizer.movements.sourceNote", { source: formatText(row.source) })}</CellNote>
                </>
            ),
        },
        {
            key: "cost",
            align: "right",
            header: header("cost"),
            cell: (row) => {
                const costEur = formatEur(row.totalCostEur);
                return (
                    <>
                        {formatXof(row.totalCostXof)}
                        {costEur !== NO_VALUE && <CellNote>{costEur}</CellNote>}
                    </>
                );
            },
        },
    ];

    return (
        <Card>
            <Table
                caption={t("fertilizer.movements.title")}
                columns={columns.map((column) => ({ ...column, className: "whitespace-nowrap align-top" }))}
                rows={movements}
            />
        </Card>
    );
}
