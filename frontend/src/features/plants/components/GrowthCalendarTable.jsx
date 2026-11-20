import { useTranslation } from "react-i18next";
import { Card, Table } from "../../../shared/ui";
import { useBlockRainfall } from "../hooks/useBlockRainfall";
import {
    formatAge,
    formatBlock,
    formatDate,
    formatPhaseBand,
    formatList,
    formatMillimeters,
    formatText,
    NO_VALUE,
} from "../utils/format";
import { RAINFALL_WINDOW_DAYS } from "../utils/rainfall";

/**
 * The rain of a block over the last 30 days, measured by the rain gauges of
 * Water Supply on the farm of the block. It is read apart from the rows, so
 * each cell says where its own read stands: a figure, the module's dash when
 * no rain gauge covers the block (no value, not an error), or that the rain
 * could not be read.
 */
function RainfallCell({ state }) {
    const { t } = useTranslation("plants");
    switch (state?.kind) {
        case "value":
            return formatMillimeters(state.rainfallMm);
        case "loading":
            return <span className="text-text-muted">{t("calendar.rainfallLoading")}</span>;
        case "unavailable":
            return <span className="text-text-muted">{t("calendar.rainfallUnavailable")}</span>;
        default:
            return NO_VALUE;
    }
}

// Mockup order, with Precision after Planting Date and Source last. The mockup's
// Stage is left out: no screen records it, so it could only show a dash.
const COLUMNS = [
    { key: "block", cell: (entry) => formatBlock(entry.blockCode) },
    { key: "variety", cell: (entry) => formatList(entry.varieties) },
    { key: "plantingDate", cell: (entry) => formatDate(entry.plantingDate) },
    { key: "precision", cell: (entry) => formatText(entry.datePrecision) },
    { key: "age", align: "right", cell: (entry) => formatAge(entry.ageYears, entry.ageMonths) },
    { key: "phase", cell: (entry) => formatPhaseBand(entry.growthPhase, entry.phaseYearsBand) },
    { key: "rainfall", align: "right", cell: (entry, rainfall) => <RainfallCell state={rainfall[entry.blockCode]} /> },
    { key: "source", cell: (entry) => formatText(entry.source) },
];

export default function GrowthCalendarTable({ entries }) {
    const { t } = useTranslation("plants");
    const rainfall = useBlockRainfall(entries.map((entry) => entry.blockCode));
    const columns = COLUMNS.map((column) => ({
        ...column,
        // The rain is the one measured over the last 30 days; the header says so.
        header: t(`calendar.columns.${column.key}`, { days: RAINFALL_WINDOW_DAYS }),
        className: "whitespace-nowrap",
        cell: (entry) => column.cell(entry, rainfall),
    }));
    return (
        <Card>
            <Table columns={columns} rows={entries} caption={t("calendar.title")} />
        </Card>
    );
}
