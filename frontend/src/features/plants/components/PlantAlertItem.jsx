import { TriangleAlert } from "lucide-react";
import { formatCode, formatDate } from "../utils/format";
import PlantsBadge from "./PlantsBadge";
import { CARD } from "./plantsStyles";

/*
 * A white card like the others, with the severity said twice: by the tinted
 * tile of the icon and by the badge, in the tones of PlantsBadge (critical
 * #9a3f32 on #f7e9e7 -> 5.67:1, warning #8a631b on #f8efde -> 4.74:1). The
 * icon repeats the badge and is hidden from screen readers; it takes the
 * colour of the badge text on the same tint, so the same ratios hold (3:1 is
 * required for a graphic). A severity the API adds later reads neutral, with
 * a gray-600 icon on gray-100 -> 6.87:1.
 */
const SEVERITY_STYLES = {
    CRITICAL: { tone: "error", tile: "bg-[#f7e9e7] text-[#9a3f32]" },
    WARNING: { tone: "warning", tile: "bg-[#f8efde] text-[#8a631b]" },
};
const NEUTRAL = { tone: "neutral", tile: "bg-gray-100 text-gray-600" };

/**
 * One plant alert, as a list item: its kind and severity, the API's message as
 * it is, and its date. `title` names the kind of alert and `dateLabel` says
 * what its date is; each screen words them for the types it shows. A type
 * whose alerts have no date, such as a stock alert, gets no `dateLabel` and so
 * no date line.
 */
export default function PlantAlertItem({ alert, title, dateLabel }) {
    const style = SEVERITY_STYLES[alert.severity] ?? NEUTRAL;
    return (
        <li className={`flex items-start gap-4 ${CARD}`}>
            <div className={`flex size-10 shrink-0 items-center justify-center rounded-lg ${style.tile}`}>
                <TriangleAlert size={20} aria-hidden="true" />
            </div>
            <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                    <p className="text-sm font-bold text-on-background">{title}</p>
                    <PlantsBadge tone={style.tone}>{formatCode(alert.severity)}</PlantsBadge>
                </div>
                <p className="mt-1 text-sm break-words text-gray-800">{alert.message}</p>
                {dateLabel && <p className="mt-1 text-xs text-gray-600">{`${dateLabel} ${formatDate(alert.date)}`}</p>}
            </div>
        </li>
    );
}
