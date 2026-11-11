import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ClipboardCheck, Loader2 } from "lucide-react";
import {
    createLossRecord,
    createQualityCheck,
    fetchBatches,
    fetchLossRecords,
    fetchQualityChecks,
} from "../api";
import QualityCheckForm from "./QualityCheckForm";
import LossRecordForm from "./LossRecordForm";
import { formatMoney, formatNumber, formatQuantity } from "../utils/formatters";

function batchId(record) {
    const id = record.batch?.id ?? record.batchId ?? record.batch_id;
    return id == null ? null : String(id);
}

function batchCode(record) {
    return record.batch?.batchCode ?? record.batch?.batch_code ?? `Batch ${batchId(record) ?? "—"}`;
}

function fieldErrors(mutation) {
    return mutation.error?.status === 400 ? mutation.error.data?.fieldErrors : null;
}

function submitError(mutation) {
    return mutation.error?.status === 400 ? null : mutation.error?.message ?? null;
}

function QualityChecksTable({ checks }) {
    return (
        <div className="overflow-x-auto rounded-xl border border-line bg-white">
            <table className="min-w-full divide-y divide-line text-left text-sm">
                <thead className="bg-gray-50 text-xs tracking-wide text-gray-500 uppercase">
                    <tr>
                        <th className="px-4 py-3 font-semibold">Batch</th>
                        <th className="px-4 py-3 font-semibold">Checked at</th>
                        <th className="px-4 py-3 font-semibold">Moisture</th>
                        <th className="px-4 py-3 font-semibold">Grade</th>
                        <th className="px-4 py-3 font-semibold">Result</th>
                        <th className="px-4 py-3 font-semibold">Inspector</th>
                        <th className="px-4 py-3 font-semibold">Notes</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-line">
                    {checks.map((check) => (
                        <tr key={check.id}>
                            <td className="whitespace-nowrap px-4 py-3 font-medium text-ink">
                                {batchCode(check)}
                            </td>
                            <td className="whitespace-nowrap px-4 py-3 text-muted">
                                {check.checkedAt ?? check.checked_at ?? "—"}
                            </td>
                            <td className="px-4 py-3 text-muted">
                                {check.moisturePercent == null && check.moisture_percent == null
                                    ? "—"
                                    : `${formatNumber(check.moisturePercent ?? check.moisture_percent)}%`}
                            </td>
                            <td className="px-4 py-3 text-muted">
                                {check.qualityGrade ?? check.quality_grade ?? "—"}
                            </td>
                            <td className="px-4 py-3 text-muted">
                                {check.passed ? "Passed" : "Failed"}
                            </td>
                            <td className="px-4 py-3 text-muted">{check.inspector ?? "—"}</td>
                            <td className="max-w-56 truncate px-4 py-3 text-muted">
                                {check.notes ?? "—"}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

function LossRecordsTable({ losses }) {
    return (
        <div className="overflow-x-auto rounded-xl border border-line bg-white">
            <table className="min-w-full divide-y divide-line text-left text-sm">
                <thead className="bg-gray-50 text-xs tracking-wide text-gray-500 uppercase">
                    <tr>
                        <th className="px-4 py-3 font-semibold">Batch</th>
                        <th className="px-4 py-3 font-semibold">Recorded at</th>
                        <th className="px-4 py-3 text-right font-semibold">Quantity</th>
                        <th className="px-4 py-3 font-semibold">Cause</th>
                        <th className="px-4 py-3 text-right font-semibold">Value</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-line">
                    {losses.map((loss) => (
                        <tr key={loss.id}>
                            <td className="whitespace-nowrap px-4 py-3 font-medium text-ink">
                                {batchCode(loss)}
                            </td>
                            <td className="whitespace-nowrap px-4 py-3 text-muted">
                                {loss.recordedAt ?? loss.recorded_at ?? "—"}
                            </td>
                            <td className="px-4 py-3 text-right text-muted">
                                {formatQuantity(loss.quantityKg ?? loss.quantity_kg ?? 0)}
                            </td>
                            <td className="px-4 py-3 text-muted">{loss.cause ?? "—"}</td>
                            <td className="px-4 py-3 text-right text-muted">
                                {formatMoney(loss.valueFcfa ?? loss.value_fcfa)}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

function QueryFeedback({ query, label }) {
    if (query.isPending) {
        return (
            <div className="flex items-center gap-3 rounded-xl border border-line bg-white px-6 py-10 text-sm text-muted">
                <Loader2 size={16} className="animate-spin" />
                Loading {label}…
            </div>
        );
    }
    if (query.isError) {
        return (
            <p role="alert" className="rounded-xl border border-error/30 bg-error/5 px-4 py-4 text-sm text-error">
                {label} could not be loaded: {query.error.message}
            </p>
        );
    }
    return null;
}

export default function QualityLossPage() {
    const [selectedBatchId, setSelectedBatchId] = useState("ALL");
    const queryClient = useQueryClient();

    useEffect(() => {
        document.title = "Quality & Losses — Crop Storage";
    }, []);

    const batchesQuery = useQuery({
        queryKey: ["cropstorage", "quality-batches"],
        queryFn: fetchBatches,
    });
    const checksQuery = useQuery({
        queryKey: ["cropstorage", "quality-checks"],
        queryFn: fetchQualityChecks,
    });
    const lossesQuery = useQuery({
        queryKey: ["cropstorage", "loss-records"],
        queryFn: fetchLossRecords,
    });

    const checkMutation = useMutation({
        mutationFn: createQualityCheck,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cropstorage", "quality-checks"] }),
    });
    const lossMutation = useMutation({
        mutationFn: createLossRecord,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cropstorage", "loss-records"] }),
    });

    const checks = (checksQuery.data ?? []).filter(
        (check) => selectedBatchId === "ALL" || batchId(check) === selectedBatchId,
    );

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header>
                    <p className="font-heading text-xs font-bold tracking-widest text-primary uppercase">
                        Crop Storage
                    </p>
                    <h1 className="font-heading mt-1 text-3xl font-bold text-gray-900">Quality &amp; Losses</h1>
                    <p className="mt-2 text-sm text-gray-500">
                        Track inspection results and recorded inventory losses.
                    </p>
                </header>

                <section className="flex flex-col gap-4">
                    <div className="flex items-center gap-2">
                        <ClipboardCheck size={18} className="text-primary" aria-hidden="true" />
                        <h2 className="font-heading text-lg font-semibold text-gray-900">Quality checks</h2>
                    </div>
                    <div className="max-w-xs">
                        <QueryFeedback query={batchesQuery} label="Batches" />
                        {!batchesQuery.isPending && !batchesQuery.isError && batchesQuery.data.length === 0 && (
                            <p className="mb-3 rounded-xl border border-dashed border-line bg-white px-5 py-6 text-sm text-muted">
                                No batches yet. Create a batch before recording a quality check.
                            </p>
                        )}
                        <label
                            htmlFor="quality-batch-filter"
                            className="mb-1.5 block text-xs tracking-wide text-primary uppercase"
                        >
                            Filter by batch
                        </label>
                        <select
                            id="quality-batch-filter"
                            value={selectedBatchId}
                            onChange={(event) => setSelectedBatchId(event.target.value)}
                            className="w-full rounded-md border border-line bg-white px-2.5 py-2 text-sm"
                        >
                            <option value="ALL">All batches</option>
                            {(batchesQuery.data ?? []).map((batch) => (
                                <option key={batch.id} value={String(batch.id)}>
                                    {batch.batchCode ?? batch.batch_code ?? `Batch ${batch.id}`}
                                </option>
                            ))}
                        </select>
                    </div>
                    <QueryFeedback query={checksQuery} label="Quality checks" />
                    {!checksQuery.isPending && !checksQuery.isError && (
                        checks.length ? (
                            <QualityChecksTable checks={checks} />
                        ) : (
                            <p className="rounded-xl border border-dashed border-line bg-white px-6 py-10 text-center text-sm text-muted">
                                No quality checks match this filter. Add a check below or choose another batch.
                            </p>
                        )
                    )}
                    <QualityCheckForm
                        batches={batchesQuery.data ?? []}
                        isSubmitting={checkMutation.isPending}
                        fieldErrors={fieldErrors(checkMutation)}
                        submitError={submitError(checkMutation)}
                        success={checkMutation.isSuccess}
                        onSubmit={(payload) => checkMutation.mutate(payload)}
                        onClearFeedback={() => checkMutation.reset()}
                    />
                </section>

                <section className="flex flex-col gap-4">
                    <h2 className="font-heading text-lg font-semibold text-gray-900">Loss records</h2>
                    <QueryFeedback query={lossesQuery} label="Loss records" />
                    {!lossesQuery.isPending && !lossesQuery.isError && (
                        lossesQuery.data.length ? (
                            <LossRecordsTable losses={lossesQuery.data} />
                        ) : (
                            <p className="rounded-xl border border-dashed border-line bg-white px-6 py-10 text-center text-sm text-muted">
                                No loss records yet. Add a record below to track inventory losses.
                            </p>
                        )
                    )}
                    <LossRecordForm
                        batches={batchesQuery.data ?? []}
                        isSubmitting={lossMutation.isPending}
                        fieldErrors={fieldErrors(lossMutation)}
                        submitError={submitError(lossMutation)}
                        success={lossMutation.isSuccess}
                        onSubmit={(payload) => lossMutation.mutate(payload)}
                        onClearFeedback={() => lossMutation.reset()}
                    />
                </section>
            </div>
        </div>
    );
}
