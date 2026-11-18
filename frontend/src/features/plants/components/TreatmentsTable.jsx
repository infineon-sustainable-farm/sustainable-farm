import { useTranslation } from "react-i18next";
import { Table } from "../../../shared/ui";
import { formatBlock, formatDate, formatNumber, formatQuantity, formatText, NO_VALUE } from "../utils/format";
import Value from "./Value";

/**
 * The treatment record, one row per treatment, in the order the API sends it.
 *
 * What a GLOBALG.A.P. plant protection record keeps for each treatment. What
 * decides the next harvest comes first, so that the pre-harvest interval and
 * the date it ends stay in view on a desktop screen; the other details follow,
 * with Source last as in the other tables.
 */
export default function TreatmentsTable({ treatments }) {
    const { t } = useTranslation("plants");

    /** A whole number of days, as the API stores the pre-harvest interval: "14 days", "1 day". */
    function formatDays(days) {
        if (days === null || days === undefined) return NO_VALUE;
        return t("health.treatments.days", { count: Number(days), days: formatNumber(days) });
    }

    const text = (value) => formatText(value);
    const columns = [
        { key: "date", header: t("health.treatments.columns.date"), className: "whitespace-nowrap", cell: (row) => formatDate(row.treatedOn) },
        { key: "block", header: t("health.treatments.columns.block"), className: "whitespace-nowrap", cell: (row) => formatBlock(row.blockCode) },
        {
            // "Other" is named by the label typed with it, as in the findings.
            key: "problem",
            header: t("health.treatments.columns.problem"),
            cell: (row) => text(row.targetOtherLabel ?? row.targetIssueName),
        },
        { key: "product", header: t("health.treatments.columns.product"), cell: (row) => text(row.productName) },
        {
            key: "interval",
            header: t("health.treatments.columns.interval"),
            className: "whitespace-nowrap",
            cell: (row) => formatDays(row.preHarvestIntervalDays),
        },
        {
            key: "harvestAllowedFrom",
            header: t("health.treatments.columns.harvestAllowedFrom"),
            className: "whitespace-nowrap",
            cell: (row) => <Value>{formatDate(row.harvestAllowedFrom)}</Value>,
        },
        {
            key: "type",
            header: t("health.treatments.columns.type"),
            cell: (row) => t(row.findingId === null ? "health.treatments.preventive" : "health.treatments.finding"),
        },
        { key: "ingredient", header: t("health.treatments.columns.ingredient"), cell: (row) => text(row.activeIngredient) },
        {
            key: "quantity",
            header: t("health.treatments.columns.quantity"),
            className: "whitespace-nowrap",
            cell: (row) => formatQuantity(row.quantity, row.unit),
        },
        { key: "applicator", header: t("health.treatments.columns.applicator"), cell: (row) => text(row.applicator) },
        {
            key: "equipment",
            header: t("health.treatments.columns.equipment"),
            cell: (row) => <Value>{text(row.equipment)}</Value>,
        },
        { key: "source", header: t("health.treatments.columns.source"), cell: (row) => text(row.source) },
    ];

    return <Table caption={t("health.treatments.title")} columns={columns} rows={treatments} />;
}
