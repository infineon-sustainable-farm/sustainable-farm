import { useState } from "react";
import { Loader2, Plus, TriangleAlert } from "lucide-react";
import { useAddHealthIssue } from "../hooks/useAddHealthIssue";
import { useHealthIssues } from "../hooks/useHealthIssues";
import { formatCode, formatText } from "../utils/format";
import AddHealthIssueModal from "./AddHealthIssueModal";
import PlantsEmptyState from "./PlantsEmptyState";
import { PRIMARY_BUTTON, SECONDARY_BUTTON, TABLE_CARD, TABLE_HEAD } from "./plantsStyles";

const COLUMNS = ["Name", "Kind", "Code", "Scientific name", "EPPO code", "Source"];

function AddHealthIssueButton({ onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className={SECONDARY_BUTTON}
        >
            <Plus size={16} />
            Add a pest or disease
        </button>
    );
}

/** The catalogue, in the order the API sends it: pests, diseases, then "Other". */
function HealthIssuesTable({ issues }) {
    return (
        <div className={TABLE_CARD}>
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className={TABLE_HEAD}
                            >
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {issues.map((issue) => (
                        <tr key={issue.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 font-semibold text-gray-800">{formatText(issue.name)}</td>
                            <td className="px-4 py-3 text-gray-700">{formatCode(issue.kind)}</td>
                            <td className="px-4 py-3 text-gray-700">{formatText(issue.code)}</td>
                            <td className="px-4 py-3 text-gray-700 italic">{formatText(issue.scientificName)}</td>
                            <td className="px-4 py-3 text-gray-700">{formatText(issue.eppoCode)}</td>
                            <td className="px-4 py-3 text-gray-700">{formatText(issue.source)}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

/**
 * Pests and diseases: the catalogue an inspection picks its problems from,
 * with the way to add one. It holds for every block, so the block filter does
 * not apply here.
 *
 * Outside the dev profile it starts empty, and until a pest or a disease is
 * added an inspection can report "Other" only. "Other" itself is listed once
 * an inspection has used it: the API adds it then.
 */
export default function HealthIssuesSection() {
    const issues = useHealthIssues();
    const addIssue = useAddHealthIssue();
    const [isAdding, setIsAdding] = useState(false);

    function openForm() {
        addIssue.reset();
        setIsAdding(true);
    }

    function closeForm() {
        addIssue.reset();
        setIsAdding(false);
    }

    function submitIssue(issue) {
        addIssue.mutate(issue, { onSuccess: closeForm });
    }

    return (
        <section aria-labelledby="health-issues-title" className="flex flex-col gap-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                    <h2 id="health-issues-title" className="font-heading text-lg font-bold text-on-background">
                        Pests and diseases
                    </h2>
                    <p className="mt-1 max-w-2xl text-sm text-gray-600">
                        The problems an inspection can report, for every block. “Other” covers a problem outside
                        this list; it is listed here once an inspection has used it.
                    </p>
                </div>
                <AddHealthIssueButton onClick={openForm} />
            </div>

            {issues.isPending && (
                <div className="flex items-center gap-3 rounded-2xl border border-gray-200 bg-white shadow-card px-5 py-4 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading the pests and diseases…
                </div>
            )}

            {issues.isError && (
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-error/30 bg-error/5 px-5 py-4">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-on-background">The pests and diseases could not be loaded</p>
                            {/* gray-600: #4a5565 on the tinted box (#f5f1f5) -> 6.76:1, as in the other sections. */}
                            <p className="mt-0.5 text-sm text-gray-600">The request to the server did not succeed.</p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={() => issues.refetch()}
                        disabled={issues.isFetching}
                        className={PRIMARY_BUTTON}
                    >
                        {issues.isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {!issues.isPending && !issues.isError && issues.data.length === 0 && (
                <PlantsEmptyState
                    title="No pest or disease in the catalogue yet"
                    hint="Until one is added, an inspection can report “Other” only."
                />
            )}

            {!issues.isPending && !issues.isError && issues.data.length > 0 && (
                <HealthIssuesTable issues={issues.data} />
            )}

            {isAdding && (
                <AddHealthIssueModal
                    isSubmitting={addIssue.isPending}
                    error={addIssue.error}
                    onSubmit={submitIssue}
                    onClose={closeForm}
                />
            )}
        </section>
    );
}
