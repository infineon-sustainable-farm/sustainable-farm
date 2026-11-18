import { useTranslation } from "react-i18next";
import { Card, Table } from "../../../shared/ui";
import { formatDate, formatEnum, formatQuantity, formatText } from "../utils/format";
import StockStatusBadge from "./StockStatusBadge";
import Value from "./Value";

/**
 * The catalogue with the stock of each fertilizer, in the order the API sends it. The unit sits in
 * each cell rather than in the header: a catalogue can mix kilograms and litres. "Last restock" is
 * the date of the latest purchase, as the API picks it.
 */
export default function FertilizerStockTable({ fertilizers }) {
    const { t } = useTranslation("plants");
    const columns = [
        { key: "name", header: t("fertilizer.stock.columns.fertilizer"), cell: (row) => formatText(row.name) },
        {
            key: "fertilizerType",
            header: t("fertilizer.stock.columns.type"),
            cell: (row) => formatEnum("fertilizerType", row.fertilizerType),
        },
        { key: "composition", header: t("fertilizer.stock.columns.composition"), cell: (row) => formatText(row.composition) },
        {
            key: "currentStock",
            header: t("fertilizer.stock.columns.stock"),
            cell: (row) => formatQuantity(row.currentStock, row.unit),
        },
        {
            key: "reorderThreshold",
            header: t("fertilizer.stock.columns.threshold"),
            cell: (row) => formatQuantity(row.reorderThreshold, row.unit),
        },
        {
            key: "belowThreshold",
            header: t("fertilizer.stock.columns.status"),
            cell: (row) => <StockStatusBadge belowThreshold={row.belowThreshold} />,
        },
        {
            key: "lastPurchaseDate",
            header: t("fertilizer.stock.columns.lastRestock"),
            cell: (row) => <Value>{formatDate(row.lastPurchaseDate)}</Value>,
        },
    ];

    return (
        <Card>
            <Table
                caption={t("fertilizer.stock.title")}
                columns={columns.map((column) => ({ ...column, className: "whitespace-nowrap" }))}
                rows={fertilizers}
            />
        </Card>
    );
}
