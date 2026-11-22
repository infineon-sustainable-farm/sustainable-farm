import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Card, EmptyState, Field, PageHeader, Select } from "../../../shared/ui";
import { createStockMovement, fetchBatches, fetchStockMovementsByBatch, fetchStorageZones } from "../api";
import { usePageTitle } from "../utils/usePageTitle";
import { fieldErrorsOf, submitErrorOf } from "../utils/mutationErrors";
import MovementForm from "./MovementForm";
import MovementsTable from "./MovementsTable";
import QueryState from "./QueryState";

export default function MovementsPage() {
    const { t } = useTranslation("cropstorage");
    const [selectedBatchId, setSelectedBatchId] = useState("");
    const queryClient = useQueryClient();
    usePageTitle("movements.title");

    const batchesQuery = useQuery({ queryKey: ["cropstorage", "movement-batches"], queryFn: fetchBatches });
    const zonesQuery = useQuery({ queryKey: ["cropstorage", "zones-options"], queryFn: fetchStorageZones });
    const activeBatchId = selectedBatchId || String(batchesQuery.data?.[0]?.id ?? "");

    const historyQuery = useQuery({
        queryKey: ["cropstorage", "movements", activeBatchId],
        queryFn: () => fetchStockMovementsByBatch(activeBatchId),
        enabled: Boolean(activeBatchId),
    });

    const createMutation = useMutation({
        mutationFn: createStockMovement,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cropstorage"] }),
    });

    const submitError =
        createMutation.error?.status === 409 ? t("movements.form.capacityExceeded") : submitErrorOf(createMutation, t);

    return (
        <div className="flex flex-col gap-6">
            <PageHeader title={t("movements.title")} subtitle={t("movements.subtitle")} />

            <Card title={t("movements.history")}>
                <QueryState query={batchesQuery} errorKey="batches.loadError" />
                {batchesQuery.isSuccess && batchesQuery.data.length === 0 && <EmptyState message={t("movements.noBatch")} />}
                {batchesQuery.isSuccess && batchesQuery.data.length > 0 && (
                    <div className="flex flex-col gap-4">
                        <Field label={t("movements.form.batch")} className="max-w-md">
                            <Select value={activeBatchId} onChange={(event) => setSelectedBatchId(event.target.value)}>
                                {batchesQuery.data.map((batch) => (
                                    <option key={batch.id} value={batch.id}>
                                        {batch.batchCode}
                                    </option>
                                ))}
                            </Select>
                        </Field>
                        <QueryState query={historyQuery} errorKey="movements.loadError" />
                        {historyQuery.isSuccess &&
                            (historyQuery.data.length > 0 ? (
                                <MovementsTable movements={historyQuery.data} />
                            ) : (
                                <EmptyState message={t("movements.empty")} />
                            ))}
                    </div>
                )}
            </Card>

            <QueryState query={zonesQuery} errorKey="zones.loadError" rows={1} />
            {zonesQuery.isSuccess && zonesQuery.data.length === 0 && <EmptyState message={t("movements.noZone")} />}
            <MovementForm
                batches={batchesQuery.data ?? []}
                zones={zonesQuery.data ?? []}
                isSubmitting={createMutation.isPending}
                fieldErrors={fieldErrorsOf(createMutation)}
                submitError={submitError}
                success={createMutation.isSuccess}
                onSubmit={(movement) =>
                    createMutation.mutate(movement, { onSuccess: () => setSelectedBatchId(String(movement.batchId)) })
                }
                onClearFeedback={() => createMutation.reset()}
            />
        </div>
    );
}
