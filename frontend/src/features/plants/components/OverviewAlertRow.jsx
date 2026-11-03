import { OctagonAlert, TriangleAlert } from "lucide-react";
import { Link } from "react-router-dom";
import { formatCode, formatDate } from "../utils/format";
import { alertPlace, alertTypeLabel, formatRelativeDays } from "../utils/overviewLabels";
import { ALERT_COLUMNS, CARD_LINK, ICON_SIZE } from "./overviewStyles";

/*
 * Priority as the API's severity, an icon and a word, in the darkened hues of
 * the badges on white: #9a3f32 -> 6.71:1, #8e661c -> 5.16:1. A severity the
 * API adds later reads in gray-700, #364153 -> 10.30:1. Two shapes, so the
 * colour is never the only cue.
 */
const PRIORITY_STYLES = {
    CRITICAL: { icon: OctagonAlert, colour: "text-[#9a3f32]" },
    WARNING: { icon: TriangleAlert, colour: "text-[#8e661c]" },
};
const OTHER_PRIORITY = { icon: TriangleAlert, colour: "text-gray-700" };

/**
 * One alert on one line: its priority, where it applies, the issue in a few
 * words, when, and a link to the screen that lists it. The API's full message
 * follows in gray, unchanged.
 *
 * `batchCode` and `treeLabel` come from the batch and the finding the alert
 * points at, when the page has them.
 */
export default function OverviewAlertRow({ alert, batchCode, treeLabel }) {
    const priority = PRIORITY_STYLES[alert.severity] ?? OTHER_PRIORITY;
    const PriorityIcon = priority.icon;
    const type = alertTypeLabel(alert.type);
    const place = alertPlace(alert, { batchCode, treeLabel });
    const relative = formatRelativeDays(alert.daysFromToday);

    return (
        // Below lg: where and issue on the first line, then priority, when and View on the second; the
        // priority stays first in the reading order and only moves on screen. From lg up: one grid row.
        <li
            className={`grid grid-cols-[auto_minmax(0,1fr)_auto] items-baseline gap-x-3 gap-y-1 py-2 lg:gap-x-4 ${ALERT_COLUMNS}`}
        >
            <span
                className={`row-start-2 flex items-center gap-1.5 self-center text-xs font-semibold lg:row-start-auto ${priority.colour}`}
            >
                <PriorityIcon size={ICON_SIZE} aria-hidden="true" className="shrink-0" />
                {formatCode(alert.severity)}
            </span>

            <div className="col-span-3 row-start-1 flex flex-wrap items-baseline gap-x-3 gap-y-1 lg:contents">
                <span className="text-sm font-semibold text-gray-900">{place}</span>
                <span className="text-sm text-gray-900">{type.issue}</span>
            </div>

            <p className="text-sm text-gray-900">
                {alert.date ? formatDate(alert.date) : (type.undated ?? <span className="text-gray-600">No date</span>)}
                {relative && <span className="text-xs text-gray-600">{` · ${relative}`}</span>}
            </p>

            {/* The name starts with the visible word, then says which alert and where it leads. */}
            <Link
                to={type.path}
                aria-label={`View: ${type.issue}, ${place}, on ${type.screen}`}
                className={`justify-self-end ${CARD_LINK}`}
            >
                View
            </Link>

            {/* gray-600 #4a5565 on white -> 7.56:1. */}
            <p className="col-span-3 text-xs break-words text-gray-600 lg:col-span-4 lg:col-start-2">{alert.message}</p>
        </li>
    );
}
