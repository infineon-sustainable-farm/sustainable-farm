import { useTranslation } from "react-i18next";
import { Table } from "../../../shared/ui";
import { formatBlock, formatDate, formatEnum, formatNumber, formatText } from "../utils/format";
import Value from "./Value";

/**
 * Nursery events, one row per event, in the order the API sends them. Each
 * column holds what one event type records; the others show the dash.
 */
export default function NurseryEventsTable({ events = [], loading = false }) {
    const { t } = useTranslation("plants");
    const header = (key) => t(`nursery.history.columns.${key}`);
    const columns = [
        { key: "eventDate", header: header("date"), cell: (event) => formatDate(event.eventDate) },
        { key: "batchCode", header: header("batch"), cell: (event) => formatText(event.batchCode) },
        { key: "eventType", header: header("event"), cell: (event) => formatEnum("nurseryEvent", event.eventType) },
        {
            key: "stage",
            header: header("stage"),
            cell: (event) => {
                const stage = formatEnum("nurseryStage", event.stage);
                return <Value>{stage}</Value>;
            },
        },
        { key: "quantity", align: "right", header: header("quantity"), cell: (event) => <Value>{formatNumber(event.quantity)}</Value> },
        { key: "blockCode", header: header("block"), cell: (event) => <Value>{formatBlock(event.blockCode)}</Value> },
        { key: "reason", header: header("reason"), cell: (event) => <Value>{formatText(event.reason)}</Value> },
        { key: "source", header: header("source"), cell: (event) => formatText(event.source) },
    ];
    return <Table caption={t("nursery.history.caption")} columns={columns} rows={events} loading={loading} />;
}
