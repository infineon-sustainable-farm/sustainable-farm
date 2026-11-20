import { useTranslation } from "react-i18next";
import { Table } from "../../../shared/ui";
import { formatBlock, formatDate, formatNumber, formatQuantity, formatText, NO_VALUE } from "../utils/format";
import CellNote from "./CellNote";
import Value from "./Value";

/**
 * The treatment record, one row per treatment, in the order the API sends it.
 *
 * What a GLOBALG.A.P. plant protection record keeps for each treatment, on
 * eight columns: what decides the next harvest stays in view, and what only
 * documents the treatment sits in small muted text under the value it belongs
 * to (the type under the problem, the active ingredient under the product, the
 * equipment and the source under the applicator). Quantities and durations
 * line up on the right.
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
        { key: "date", header: t("health.treatments.columns.date"), className: "whitespace-nowrap align-top", cell: (row) => formatDate(row.treatedOn) },
        {
            key: "block",
            header: t("health.treatments.columns.block"),
            className: "whitespace-nowrap align-top font-semibold",
            cell: (row) => formatBlock(row.blockCode),
        },
        {
            // "Other" is named by the label typed with it, as in the findings.
            key: "problem",
            header: t("health.treatments.columns.problem"),
            className: "align-top",
            cell: (row) => (
                <>
                    {text(row.targetOtherLabel ?? row.targetIssueName)}
                    <CellNote>{t(row.findingId === null ? "health.treatments.preventive" : "health.treatments.finding")}</CellNote>
                </>
            ),
        },
        {
            key: "product",
            header: t("health.treatments.columns.product"),
            className: "align-top",
            cell: (row) => (
                <>
                    {text(row.productName)}
                    {text(row.activeIngredient) !== NO_VALUE && <CellNote>{text(row.activeIngredient)}</CellNote>}
                </>
            ),
        },
        {
            key: "quantity",
            align: "right",
            header: t("health.treatments.columns.quantity"),
            className: "whitespace-nowrap align-top",
            cell: (row) => formatQuantity(row.quantity, row.unit),
        },
        {
            key: "interval",
            align: "right",
            header: t("health.treatments.columns.interval"),
            className: "whitespace-nowrap align-top",
            cell: (row) => formatDays(row.preHarvestIntervalDays),
        },
        {
            key: "harvestAllowedFrom",
            header: t("health.treatments.columns.harvestAllowedFrom"),
            className: "whitespace-nowrap align-top",
            cell: (row) => <Value>{formatDate(row.harvestAllowedFrom)}</Value>,
        },
        {
            key: "appliedBy",
            header: t("health.treatments.columns.appliedBy"),
            className: "align-top",
            cell: (row) => (
                <>
                    {text(row.applicator)}
                    {text(row.equipment) !== NO_VALUE && <CellNote>{text(row.equipment)}</CellNote>}
                    <CellNote>{t("health.treatments.sourceNote", { source: text(row.source) })}</CellNote>
                </>
            ),
        },
    ];

    return <Table caption={t("health.treatments.title")} columns={columns} rows={treatments} />;
}
