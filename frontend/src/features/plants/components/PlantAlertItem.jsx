import { TriangleAlert } from "lucide-react";
import { Badge } from "../../../shared/ui";
import { formatDate, formatEnum } from "../utils/format";

// Critical alerts take the error tint, warnings the warning tint, any other severity stays neutral.
const SEVERITY_STYLES = {
    CRITICAL: { box: "bg-error-tint", icon: "text-error-fg", tone: "error" },
    WARNING: { box: "bg-warning-tint", icon: "text-warning-fg", tone: "warning" },
};
const NEUTRAL = { box: "bg-surface-muted", icon: "text-text-muted", tone: "neutral" };

/**
 * One plant alert, as a list item: its kind and severity, the API's message as
 * it is, and its date. `title` names the kind of alert and `dateLabel` says
 * what its date is; each screen words them for the types it shows. A type
 * whose alerts have no date, such as a stock alert, gets no `dateLabel` and so
 * no date line.
 */
export default function PlantAlertItem({ alert, title, dateLabel }) {
    const style = SEVERITY_STYLES[alert.severity] ?? NEUTRAL;
    const severity = formatEnum("severity", alert.severity);
    return (
        <li className={`flex items-start gap-4 rounded-tile border border-border-subtle p-4 ${style.box}`}>
            <div className="flex size-10 shrink-0 items-center justify-center rounded-control bg-surface-raised">
                <TriangleAlert size={20} aria-hidden="true" className={style.icon} />
            </div>
            <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                    <p className="text-sm font-semibold text-text">{title}</p>
                    <Badge tone={style.tone}>{severity}</Badge>
                </div>
                <p className="mt-1 text-sm break-words text-text">{alert.message}</p>
                {dateLabel && <p className="mt-1 text-xs text-text-muted">{`${dateLabel} ${formatDate(alert.date)}`}</p>}
            </div>
        </li>
    );
}
