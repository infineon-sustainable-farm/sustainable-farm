import { useState } from "react";
import { Loader2, Plus, TriangleAlert } from "lucide-react";
import { useAddHealthIssue } from "../hooks/useAddHealthIssue";
import { useHealthIssues } from "../hooks/useHealthIssues";
import { formatCode, formatText } from "../utils/format";
import AddHealthIssueModal from "./AddHealthIssueModal";
import PlantsEmptyState from "./PlantsEmptyState";

const COLUMNS = ["Name", "Kind", "Code", "Scientific name", "EPPO code", "Source"];

function AddHealthIssueButton({ onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
        >
            <Plus size={16} />
            Add a pest or disease
        </button>
    );
}

/** The catalogue, in the order the API sends it: pests, diseases, then "Other". */
function HealthIssuesTable({ issues }) {
    return (
        <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className="border-b border-gray-200 bg-gray-50 px-4 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase"
                            >
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {issues.map((issue) => (
                        <tr key={issue.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 font-medium text-gray-800">{formatText(issue.name)}</td>
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
                    <h3 id="health-issues-title" className="font-heading text-base font-bold text-gray-900">
                        Pests and diseases
                    </h3>
                    <p className="mt-1 max-w-2xl text-sm text-gray-600">
                        The problems an inspection can report, for every block. “Other” covers a problem outside
                        this list; it is listed here once an inspection has used it.
                    </p>
                </div>
                <AddHealthIssueButton onClick={openForm} />
            </div>

            {issues.isPending && (
                <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading the pests and diseases…
                </div>
            )}

            {issues.isError && (
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-error/30 bg-error/5 px-5 py-4">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-gray-900">The pests and diseases could not be loaded</p>
                            {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1, as in the other sections. */}
                            <p className="mt-0.5 text-sm text-gray-600">The request to the server did not succeed.</p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={() => issues.refetch()}
                        disabled={issues.isFetching}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
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
