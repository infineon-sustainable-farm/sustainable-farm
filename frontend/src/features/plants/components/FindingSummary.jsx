import { useTranslation } from "react-i18next";
import { formatDate, formatText } from "../utils/format";
import { findingPlace } from "../utils/healthIssues";

/**
 * The finding a dialog is about, recalled at its top: where it was seen, what
 * it is, and when. "Other" is named by the label typed with it.
 */
export default function FindingSummary({ finding }) {
    const { t } = useTranslation("plants");
    const details = t("health.summary.details", {
        problem: formatText(finding.otherLabel ?? finding.issueName),
        date: formatDate(finding.inspectedOn),
    });
    return (
        <p className="mb-5 rounded-control bg-surface-muted px-3 py-2 text-sm text-text">
            <span className="font-semibold">{findingPlace(finding)}</span>
            {details}
        </p>
    );
}
