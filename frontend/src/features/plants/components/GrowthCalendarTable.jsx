import { useTranslation } from "react-i18next";
import { Card, Table } from "../../../shared/ui";
import {
    formatAge,
    formatBlock,
    formatDate,
    formatPhaseBand,
    formatList,
    formatMillimeters,
    formatText,
} from "../utils/format";

// Mockup order, with Precision after Planting Date and Source last. The mockup's
// Stage is left out: no screen records it, so it could only show a dash.
const COLUMNS = [
    { key: "block", cell: (entry) => formatBlock(entry.blockCode) },
    { key: "variety", cell: (entry) => formatList(entry.varieties) },
    { key: "plantingDate", cell: (entry) => formatDate(entry.plantingDate) },
    { key: "precision", cell: (entry) => formatText(entry.datePrecision) },
    { key: "age", cell: (entry) => formatAge(entry.ageYears, entry.ageMonths) },
    { key: "phase", cell: (entry) => formatPhaseBand(entry.growthPhase, entry.phaseYearsBand) },
    { key: "rainfall", cell: (entry) => formatMillimeters(entry.localRainfallMm) },
    { key: "source", cell: (entry) => formatText(entry.source) },
];

export default function GrowthCalendarTable({ entries }) {
    const { t } = useTranslation("plants");
    const columns = COLUMNS.map((column) => ({
        ...column,
        header: t(`calendar.columns.${column.key}`),
        className: "whitespace-nowrap",
    }));
    return (
        <Card>
            <Table columns={columns} rows={entries} caption={t("calendar.title")} />
        </Card>
    );
}
