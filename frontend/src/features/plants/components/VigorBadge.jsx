import { formatText, NO_VALUE } from "../utils/format";

/*
 * Border: shared token from index.css (--color-success / warning / error).
 * Background and label: the same hues, lightened and darkened, to reach the
 * WCAG AA 4.5:1 contrast threshold against the badge background.
 *   #257e55 on #e5f7ef -> 4.50:1
 *   #8e661c on #f8efde -> 4.52:1
 *   #9a3f32 on #f7e9e7 -> 5.67:1
 * The shared tokens are kept as they are; none is redefined.
 * Reason: --color-warning (#d9a441) is only 2.25:1 on white, so the token
 * cannot carry label text on its own.
 */
const VIGOR_STYLES = {
    good: "border-success bg-[#e5f7ef] text-[#257e55]",
    strong: "border-success bg-[#e5f7ef] text-[#257e55]",
    high: "border-success bg-[#e5f7ef] text-[#257e55]",
    average: "border-warning bg-[#f8efde] text-[#8e661c]",
    medium: "border-warning bg-[#f8efde] text-[#8e661c]",
    moderate: "border-warning bg-[#f8efde] text-[#8e661c]",
    poor: "border-error bg-[#f7e9e7] text-[#9a3f32]",
    weak: "border-error bg-[#f7e9e7] text-[#9a3f32]",
    low: "border-error bg-[#f7e9e7] text-[#9a3f32]",
};

export default function VigorBadge({ value }) {
    const label = formatText(value);
    if (label === NO_VALUE) {
        return <span className="text-gray-400">{NO_VALUE}</span>;
    }

    // An unrecognised rating is still shown, in neutral styling — the value is
    // never dropped or reinterpreted.
    const style = VIGOR_STYLES[label.toLowerCase()] ?? "border-gray-300 bg-gray-100 text-gray-700";
    return (
        <span className={`inline-block rounded-full border px-2.5 py-0.5 text-xs font-bold ${style}`}>
            {label}
        </span>
    );
}
