import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Card, EmptyState, Field, PageHeader, Select, StatusBadge, Table } from "../../../shared/ui";
import { createLossRecord, createQualityCheck, fetchBatches, fetchLossRecords, fetchQualityChecks } from "../api";
import { EMPTY, formatDateTime, formatMoney, formatNumber, formatQuantity } from "../utils/formatters";
import { usePageTitle } from "../utils/usePageTitle";
import { fieldErrorsOf, submitErrorOf } from "../utils/mutationErrors";
import LossRecordForm from "./LossRecordForm";
import QualityCheckForm from "./QualityCheckForm";
import QueryState from "./QueryState";

function batchCode(record) {
    return record.batch?.batchCode ?? EMPTY;
}

export default function QualityLossPage() {
    const { t } = useTranslation("cropstorage");
    const [selectedBatchId, setSelectedBatchId] = useState("ALL");
    const queryClient = useQueryClient();
    usePageTitle("quality.title");

    const batchesQuery = useQuery({ queryKey: ["cropstorage", "quality-batches"], queryFn: fetchBatches });
    const checksQuery = useQuery({ queryKey: ["cropstorage", "quality-checks"], queryFn: fetchQualityChecks });
    const lossesQuery = useQuery({ queryKey: ["cropstorage", "loss-records"], queryFn: fetchLossRecords });

    const checkMutation = useMutation({
        mutationFn: createQualityCheck,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cropstorage", "quality-checks"] }),
    });
    const lossMutation = useMutation({
        mutationFn: createLossRecord,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cropstorage", "loss-records"] }),
    });

    const checks = (checksQuery.data ?? []).filter(
        (check) => selectedBatchId === "ALL" || String(check.batch?.id) === selectedBatchId,
    );

    const checkColumns = [
        { key: "batch", header: t("quality.columns.batch"), cell: batchCode },
        { key: "checkedAt", header: t("quality.columns.checkedAt"), cell: (c) => formatDateTime(c.checkedAt) },
        {
            key: "moisturePercent",
            header: t("quality.columns.moisture"),
            cell: (c) => (c.moisturePercent == null ? EMPTY : t("percent", { value: formatNumber(c.moisturePercent) })),
        },
        { key: "qualityGrade", header: t("quality.columns.grade"), cell: (c) => c.qualityGrade ?? EMPTY },
        {
            key: "passed",
            header: t("quality.columns.result"),
            cell: (c) => (
                <StatusBadge tone={c.passed ? "success" : "error"} label={t(c.passed ? "quality.passed" : "quality.failed")} withIcon />
            ),
        },
        { key: "inspector", header: t("quality.columns.inspector"), cell: (c) => c.inspectorName ?? EMPTY },
        { key: "notes", header: t("quality.columns.notes"), cell: (c) => c.notes ?? EMPTY, className: "max-w-56 truncate" },
    ];
    const lossColumns = [
        { key: "batch", header: t("losses.columns.batch"), cell: batchCode },
        { key: "recordedAt", header: t("losses.columns.recordedAt"), cell: (l) => formatDateTime(l.recordedAt) },
        { key: "quantityKg", header: t("losses.columns.quantity"), align: "right", cell: (l) => formatQuantity(l.quantityKg ?? 0) },
        { key: "cause", header: t("losses.columns.cause"), cell: (l) => l.cause ?? EMPTY },
        { key: "valueFcfa", header: t("losses.columns.value"), align: "right", cell: (l) => formatMoney(l.valueFcfa) },
    ];

    return (
        <div className="flex flex-col gap-6">
            <PageHeader title={t("quality.title")} subtitle={t("quality.subtitle")} />

            <Card title={t("quality.checks")}>
                <div className="flex flex-col gap-4">
                    <QueryState query={batchesQuery} errorKey="batches.loadError" rows={1} />
                    {batchesQuery.isSuccess && batchesQuery.data.length === 0 && <EmptyState message={t("quality.noBatch")} />}
                    <Field label={t("quality.filter")} className="max-w-xs">
                        <Select value={selectedBatchId} onChange={(event) => setSelectedBatchId(event.target.value)}>
                            <option value="ALL">{t("quality.allBatches")}</option>
                            {(batchesQuery.data ?? []).map((batch) => (
                                <option key={batch.id} value={String(batch.id)}>
                                    {batch.batchCode}
                                </option>
                            ))}
                        </Select>
                    </Field>
                    <QueryState query={checksQuery} errorKey="quality.loadError" />
                    {checksQuery.isSuccess &&
                        (checks.length ? (
                            <Table columns={checkColumns} rows={checks} caption={t("quality.tableCaption")} />
                        ) : (
                            <EmptyState message={t("quality.empty")} />
                        ))}
                </div>
            </Card>
            <QualityCheckForm
                batches={batchesQuery.data ?? []}
                isSubmitting={checkMutation.isPending}
                fieldErrors={fieldErrorsOf(checkMutation)}
                submitError={submitErrorOf(checkMutation, t)}
                success={checkMutation.isSuccess}
                onSubmit={(payload) => checkMutation.mutate(payload)}
                onClearFeedback={() => checkMutation.reset()}
            />

            <Card title={t("losses.title")}>
                <QueryState query={lossesQuery} errorKey="losses.loadError" />
                {lossesQuery.isSuccess &&
                    (lossesQuery.data.length ? (
                        <Table columns={lossColumns} rows={lossesQuery.data} caption={t("losses.tableCaption")} />
                    ) : (
                        <EmptyState message={t("losses.empty")} />
                    ))}
            </Card>
            <LossRecordForm
                batches={batchesQuery.data ?? []}
                isSubmitting={lossMutation.isPending}
                fieldErrors={fieldErrorsOf(lossMutation)}
                submitError={submitErrorOf(lossMutation, t)}
                success={lossMutation.isSuccess}
                onSubmit={(payload) => lossMutation.mutate(payload)}
                onClearFeedback={() => lossMutation.reset()}
            />
        </div>
    );
}
