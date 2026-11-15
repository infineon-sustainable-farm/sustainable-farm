import { useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import { useHealthFindings } from "../hooks/useHealthFindings";
import { formatCode } from "../utils/format";
import FindingsTable from "./FindingsTable";
import PlantsEmptyState from "./PlantsEmptyState";
import { FIELD_INPUT, PRIMARY_BUTTON } from "./plantsStyles";

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
    // API code ("UNTREATED"); an empty string means "every status".
    const [status, setStatus] = useState("");

    const { data: findings, isPending, isError, refetch, isFetching } = useHealthFindings({
        blockCode,
        status: status || null,
    });
    const isFiltered = Boolean(blockCode || status);

    return (
        <section aria-labelledby="findings-title" className="flex flex-col gap-4">
            <h2 id="findings-title" className="font-heading text-lg font-bold text-on-background">
                Findings
            </h2>

            <div className="flex flex-wrap items-center gap-x-6 gap-y-3 rounded-2xl border border-gray-200 bg-white shadow-card px-4 py-3">
                <div className="flex max-w-full min-w-0 items-center gap-3">
                    {/* Its accessible name joins the section title and its own label. */}
                    <label
                        id="findings-status-label"
                        htmlFor="findings-status"
                        className="shrink-0 text-xs font-semibold text-gray-500"
                    >
                        Status
                    </label>
                    <select
                        id="findings-status"
                        value={status}
                        onChange={(event) => setStatus(event.target.value)}
                        aria-labelledby="findings-title findings-status-label"
                        className={`min-w-0 max-w-full ${FIELD_INPUT}`}
                    >
                        <option value="">All statuses</option>
                        {FINDING_STATUSES.map((code) => (
                            <option key={code} value={code}>
                                {formatCode(code)}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-2xl border border-gray-200 bg-white shadow-card px-6 py-16 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading findings…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-2xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-lg font-bold text-on-background">
                            Findings could not be loaded
                        </p>
                        {/* gray-600: #4a5565 on the tinted box (#f5f1f5) -> 6.76:1; gray-500 gave 4.32:1. */}
                        <p className="mt-1 text-sm text-gray-600">
                            The request to the server did not succeed.
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className={PRIMARY_BUTTON}
                    >
                        {isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {!isPending && !isError && findings.length === 0 && (
                isFiltered ? (
                    <PlantsEmptyState
                        title="No finding matches these filters"
                        hint="Try another block or status, or select “All blocks”."
                    />
                ) : (
                    <PlantsEmptyState
                        title="No finding recorded yet"
                        hint="Findings are recorded with an inspection."
                        action={recordAction}
                    />
                )
            )}

            {!isPending && !isError && findings.length > 0 && (
                <FindingsTable findings={findings} onTreat={onTreat} onResolve={onResolve} />
            )}
        </section>
    );
}
