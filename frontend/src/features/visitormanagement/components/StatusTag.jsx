import { StatusBadge } from "../../../shared/ui";
import { statusTone, useLabels } from "../utils/labels";

/**
 * The kit StatusBadge for a raw API status: "CHECKED_IN" reads "Checked In" in the tone of that
 * state. Nothing is rendered without a value.
 */
export default function StatusTag({ value, className }) {
    const labels = useLabels();
    if (!value) return null;
    return <StatusBadge tone={statusTone(value)} label={labels.enum(value)} className={className} />;
}
