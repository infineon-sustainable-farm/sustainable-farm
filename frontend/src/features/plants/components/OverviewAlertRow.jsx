import { OctagonAlert, TriangleAlert } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { formatDate, formatEnum } from "../utils/format";
import { alertPlace, alertTypeLabel, formatRelativeDays } from "../utils/overviewLabels";
import { ALERT_COLUMNS } from "./overviewAlertColumns";

/*
 * Priority as the API's severity, an icon and a word. A severity the API adds
 * later reads in the body text colour. Two shapes, so the colour is never the
 * only cue.
 */
const PRIORITY_STYLES = {
    CRITICAL: { icon: OctagonAlert, colour: "text-error-fg" },
    WARNING: { icon: TriangleAlert, colour: "text-warning-fg" },
};
const OTHER_PRIORITY = { icon: TriangleAlert, colour: "text-text" };

const LINK =
    "text-sm font-semibold text-text-accent underline underline-offset-2 hover:no-underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring";

/**
 * One alert on one line: its priority, where it applies, the issue in a few
 * words, when, and a link to the screen that lists it. The API's full message
 * follows in gray, unchanged. Below lg the parts wrap, the message on its own
 * line.
 *
 * `batchCode` and `treeLabel` come from the batch and the finding the alert
 * points at, when the page has them.
 */
export default function OverviewAlertRow({ alert, batchCode, treeLabel }) {
    const { t } = useTranslation("plants");
    const priority = PRIORITY_STYLES[alert.severity] ?? OTHER_PRIORITY;
    const PriorityIcon = priority.icon;
    const severity = formatEnum("severity", alert.severity);
    const type = alertTypeLabel(alert.type);
    const place = alertPlace(alert, { batchCode, treeLabel });
    const relative = formatRelativeDays(alert.daysFromToday);
    const relativeText = relative ? ` · ${relative}` : null;

    return (
        <li className="flex flex-wrap items-baseline gap-x-3 gap-y-1 border-b border-border-subtle py-2 last:border-b-0 lg:grid lg:grid-cols-12 lg:gap-x-4">
            <span className={`flex items-center gap-1.5 self-center text-xs font-semibold ${priority.colour} ${ALERT_COLUMNS.priority}`}>
                <PriorityIcon size={16} aria-hidden="true" className="shrink-0" />
                {severity}
            </span>

            <span className={`text-sm font-semibold text-text ${ALERT_COLUMNS.place}`}>{place}</span>
            <span className={`text-sm text-text ${ALERT_COLUMNS.issue}`}>{type.issue}</span>

            <p className={`text-sm text-text ${ALERT_COLUMNS.when}`}>
                {alert.date
                    ? formatDate(alert.date)
                    : (type.undated ?? <span className="text-text-muted">{t("overview.alerts.noDate")}</span>)}
                {relativeText && <span className="text-xs text-text-muted">{relativeText}</span>}
            </p>

            {/* The name starts with the visible word, then says which alert and where it leads. */}
            <Link
                to={type.path}
                aria-label={t("overview.alerts.viewLabel", { issue: type.issue, place, screen: type.screen })}
                className={`ml-auto lg:ml-0 lg:justify-self-end ${ALERT_COLUMNS.action} ${LINK}`}
            >
                {t("overview.alerts.view")}
            </Link>

            <p className={`basis-full text-xs break-words text-text-muted ${ALERT_COLUMNS.message}`}>{alert.message}</p>
        </li>
    );
}
