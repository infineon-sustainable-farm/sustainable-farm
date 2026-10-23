import { TriangleAlert } from "lucide-react";
import { formatCode, formatDate } from "../utils/format";

/*
 * The mock-up's callout: a tinted box and a solid pill. Its red tint and border
 * hold the critical alerts; the warnings take the amber tint of the badges.
 * Pills in the darkened hues, white text: #9a3f32 -> 6.71:1, #8e661c -> 5.16:1
 * (white on the mock-up's #c1503f gave 4.68:1, and on #d9a441 2.25:1).
 */
const SEVERITY_STYLES = {
    CRITICAL: { box: "border-[#edcdc8] bg-[#f7e9e7]", icon: "text-error", pill: "bg-[#9a3f32]" },
    WARNING: { box: "border-[#ecd9b0] bg-[#f8efde]", icon: "text-[#8e661c]", pill: "bg-[#8e661c]" },
};
const NEUTRAL = { box: "border-gray-200 bg-white", icon: "text-gray-600", pill: "bg-gray-700" };

/**
 * One plant alert, as a list item: its kind and severity, the API's message as
 * it is, and its date. `title` names the kind of alert and `dateLabel` says
 * what its date is; each screen words them for the types it shows.
 */
export default function PlantAlertItem({ alert, title, dateLabel }) {
    const style = SEVERITY_STYLES[alert.severity] ?? NEUTRAL;
    return (
        <li className={`flex items-start gap-4 rounded-xl border px-4 py-4 ${style.box}`}>
            <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-white">
                <TriangleAlert size={20} aria-hidden="true" className={style.icon} />
            </div>
            <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                    <p className="text-sm font-bold text-gray-900">{title}</p>
                    <span
                        className={`rounded-full px-2.5 py-0.5 text-[11px] font-bold tracking-wide text-white uppercase ${style.pill}`}
                    >
                        {formatCode(alert.severity)}
                    </span>
                </div>
                <p className="mt-1 text-sm break-words text-gray-800">{alert.message}</p>
                <p className="mt-1 text-xs text-gray-600">{`${dateLabel} ${formatDate(alert.date)}`}</p>
            </div>
        </li>
    );
}
