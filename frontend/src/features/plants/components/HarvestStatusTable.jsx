import { useTranslation } from "react-i18next";
import { Card, Table } from "../../../shared/ui";
import { formatBlock, formatDate, formatDaysAhead, formatEnum, formatText, NO_VALUE } from "../utils/format";
import { formatObservationMethod } from "../utils/maturityObservations";
import MaturityStatusBadge from "./MaturityStatusBadge";
import Value from "./Value";

/**
 * Harvest window, from its first day to the end of the season, with the days
 * left before it opens as the API counts them. Both dates are null without an
 * agronomic reference, and the start is always the first day of a month.
 */
function HarvestWindow({ status }) {
    const { t } = useTranslation("plants");
    if (status.estimatedHarvestDate === null) {
        return <Value>{NO_VALUE}</Value>;
    }
    const daysAhead = formatDaysAhead(status.daysFromToday);
    return (
        <>
            {t("format.range", { start: formatDate(status.estimatedHarvestDate), end: formatDate(status.seasonEnd) })}
            {daysAhead && <span className="block text-xs text-text-muted">{daysAhead}</span>}
        </>
    );
}

/**
 * The field observation of the row, as the API sends it: its status, its day
 * and method, the observer, and until when it counts. Without an agronomic
 * reference the API sends the latest observation, which does not count. When
 * a running pre-harvest interval holds an observed "Ready for harvest" back,
 * the status that holds is said under it; nothing is decided here.
 */
function Observed({ status }) {
    const { t } = useTranslation("plants");
    const observation = status.observation;
    if (!observation) {
        return <Value>{NO_VALUE}</Value>;
    }
    const isHeldBack = observation.countsUntil !== null && status.maturityStatus !== observation.maturityStatus;
    const heldStatus = formatEnum("maturityStatus", status.maturityStatus);
    return (
        <>
            <MaturityStatusBadge status={observation.maturityStatus} />
            <span className="mt-1 block text-xs text-text-muted">
                {t("harvest.statusTable.observationLine", {
                    date: formatDate(observation.observedOn),
                    method: formatObservationMethod(observation.method, observation.measuredValue, observation.measuredUnit),
                })}
            </span>
            {observation.observer && <span className="block text-xs text-text-muted">{observation.observer}</span>}
            <span className="block text-xs text-text-muted">
                {observation.countsUntil === null
                    ? t("harvest.statusTable.notCounted")
                    : t("harvest.statusTable.countsUntil", { date: formatDate(observation.countsUntil) })}
            </span>
            {isHeldBack && (
                <span className="block text-xs font-semibold text-text">
                    {t("harvest.statusTable.heldAt", { status: heldStatus })}
                </span>
            )}
        </>
    );
}

/** Source of the harvest season, of the yield share that opens the window, and of the observation. */
function Sources({ status }) {
    const { t } = useTranslation("plants");
    const lines = [];
    if (status.seasonSource !== null || status.phaseShareSource !== null) {
        lines.push(
            t("harvest.statusTable.season", { value: formatText(status.seasonSource) }),
            t("harvest.statusTable.share", { value: formatText(status.phaseShareSource) })
        );
    }
    if (status.observation) {
        lines.push(t("harvest.statusTable.observation", { value: formatText(status.observation.source) }));
    }
    if (lines.length === 0) {
        return <Value>{NO_VALUE}</Value>;
    }
    return lines.map((line) => (
        <span key={line} className="block">
            {line}
        </span>
    ));
}

const CELL = "align-top whitespace-nowrap";

/** The maturity status of each planted variety, in the order the API sends it. */
export default function HarvestStatusTable({ statuses }) {
    const { t } = useTranslation("plants");
    const columns = [
        { key: "block", header: t("harvest.statusTable.block"), className: CELL, cell: (status) => formatBlock(status.blockCode) },
        { key: "variety", header: t("harvest.statusTable.variety"), className: CELL, cell: (status) => formatText(status.varietyName) },
        {
            key: "estimated",
            header: t("harvest.statusTable.estimated"),
            className: CELL,
            // Without a reference there is no season, hence no estimate: said, not dashed.
            cell: (status) =>
                status.estimatedStatus === null ? (
                    <span className="text-text-muted">{t("harvest.statusTable.noReference")}</span>
                ) : (
                    <MaturityStatusBadge status={status.estimatedStatus} />
                ),
        },
        { key: "observed", header: t("harvest.statusTable.observed"), className: CELL, cell: (status) => <Observed status={status} /> },
        {
            key: "window",
            header: t("harvest.statusTable.harvestWindow"),
            className: CELL,
            cell: (status) => <HarvestWindow status={status} />,
        },
        {
            key: "allowedFrom",
            header: t("harvest.statusTable.harvestAllowedFrom"),
            className: CELL,
            cell: (status) => <Value>{formatDate(status.harvestAllowedFrom)}</Value>,
        },
        {
            key: "firstHarvest",
            header: t("harvest.statusTable.firstHarvest"),
            className: CELL,
            cell: (status) => <Value>{formatDate(status.firstHarvestDate)}</Value>,
        },
        { key: "sources", header: t("harvest.statusTable.sources"), className: CELL, cell: (status) => <Sources status={status} /> },
    ];

    return (
        <Card>
            <Table
                columns={columns}
                rows={statuses}
                getRowKey={(status) => status.varietyId}
                caption={t("harvest.status.title")}
                className="text-sm"
            />
        </Card>
    );
}
