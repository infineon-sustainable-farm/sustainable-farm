import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Card, EmptyState, Select } from "../../../shared/ui";
import { useHealthFindings } from "../hooks/useHealthFindings";
import { formatEnum } from "../utils/format";
import FindingsTable from "./FindingsTable";
import { HealthSectionError, HealthSectionLoading } from "./HealthSectionStates";

// The statuses the API computes, in the order a finding goes through them.
const FINDING_STATUSES = ["UNTREATED", "IN_PROGRESS", "TREATED", "CLOSED_WITHOUT_TREATMENT"];

/**
 * The problems seen during the inspections, for the selected block, with
 * their status. The API filters by one status at a time; every status is
 * listed by default, resolved findings included.
 *
 * `recordAction` is the page's "Record an inspection" button, offered again
 * when no finding is recorded at all. `onTreat` and `onResolve` open the
 * treatment and resolution dialogs for a finding.
 */
export default function FindingsSection({ blockCode, recordAction, onTreat, onResolve }) {
    const { t } = useTranslation("plants");
    // API code ("UNTREATED"); an empty string means "every status".
    const [status, setStatus] = useState("");

    const query = useHealthFindings({ blockCode, status: status || null });
    const { data: findings, isPending, isError } = query;
    const isFiltered = Boolean(blockCode || status);
    const statusOptions = FINDING_STATUSES.map((code) => ({ value: code, label: formatEnum("findingStatus", code) }));

    return (
        <Card aria-labelledby="findings-title" title={<span id="findings-title">{t("health.findings.title")}</span>}>
            <div className="mb-4 flex max-w-full min-w-0 items-center gap-3">
                {/* Its accessible name joins the section title and its own label. */}
                <label id="findings-status-label" htmlFor="findings-status" className="shrink-0 text-sm font-medium">
                    {t("health.findings.status")}
                </label>
                <div className="min-w-0">
                    <Select
                        id="findings-status"
                        value={status}
                        onChange={(event) => setStatus(event.target.value)}
                        aria-labelledby="findings-title findings-status-label"
                    >
                        <option value="">{t("health.findings.allStatuses")}</option>
                        {statusOptions.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </Select>
                </div>
            </div>

            {isPending && <HealthSectionLoading>{t("health.findings.loading")}</HealthSectionLoading>}

            {isError && (
                <HealthSectionError
                    title={t("health.findings.loadError")}
                    detail={t("health.requestFailed")}
                    query={query}
                />
            )}

            {!isPending && !isError && findings.length === 0 && (
                isFiltered ? (
                    <EmptyState
                        className="py-8"
                        title={t("health.findings.emptyFilteredTitle")}
                        message={t("health.findings.emptyFilteredHint")}
                    />
                ) : (
                    <EmptyState
                        className="py-8"
                        title={t("health.findings.emptyTitle")}
                        message={t("health.findings.emptyHint")}
                        action={recordAction}
                    />
                )
            )}

            {!isPending && !isError && findings.length > 0 && (
                <FindingsTable findings={findings} onTreat={onTreat} onResolve={onResolve} />
            )}
        </Card>
    );
}
