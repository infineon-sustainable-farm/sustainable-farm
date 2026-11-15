import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, TriangleAlert } from "lucide-react";
import {
    createStockMovement,
    fetchBatches,
    fetchStorageZones,
    fetchStockMovementsByBatch,
} from "../api";
import MovementForm from "./MovementForm";
import MovementsTable from "./MovementsTable";

export default function MovementsPage() {
    const [selectedBatchId, setSelectedBatchId] = useState("");
    const queryClient = useQueryClient();

    useEffect(() => {
        document.title = "Movements — Crop Storage";
    }, []);

    const batchesQuery = useQuery({
        queryKey: ["cropstorage", "movement-batches"],
        queryFn: fetchBatches,
    });
    const zonesQuery = useQuery({
        queryKey: ["cropstorage", "zones-options"],
        queryFn: fetchStorageZones,
    });
    const activeBatchId = selectedBatchId || String(batchesQuery.data?.[0]?.id ?? "");

    const historyQuery = useQuery({
        queryKey: ["cropstorage", "movements", activeBatchId],
        queryFn: () => fetchStockMovementsByBatch(activeBatchId),
        enabled: Boolean(activeBatchId),
    });

    const createMutation = useMutation({
        mutationFn: createStockMovement,
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: ["cropstorage"] });
        },
    });

    const errorStatus = createMutation.error?.status;
    const fieldErrors = errorStatus === 400 ? createMutation.error?.data?.fieldErrors : null;
    const apiErrorMessage = createMutation.error?.message ?? "";
    const submitError =
        errorStatus === 409
            ? "Zone capacity exceeded."
            : errorStatus === 422 && apiErrorMessage.toLowerCase().includes("not enough stock")
                ? "Insufficient stock for this movement."
                : errorStatus && errorStatus !== 400
                    ? apiErrorMessage
                    : null;

    function registerMovement(movement) {
        createMutation.mutate(movement, {
            onSuccess: () => {
                setSelectedBatchId(String(movement.batchId));
            },
        });
    }

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header>
                    <p className="font-heading text-xs font-bold tracking-widest text-primary uppercase">
                        Crop Storage
                    </p>
                    <h1 className="font-heading mt-1 text-3xl font-bold text-gray-900">Stock Movements</h1>
                    <p className="mt-2 text-sm text-gray-500">
                        Review batch movement history and register stock changes.
                    </p>
                </header>

                <section className="flex flex-col gap-4">
                    <h2 className="font-heading text-lg font-semibold text-gray-900">Movement history</h2>
                    {batchesQuery.isPending && (
                        <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-6 text-sm text-gray-500">
                            <Loader2 size={18} className="animate-spin" />
                            Loading batches…
                        </div>
                    )}
                    {batchesQuery.isError && (
                        <p role="alert" className="rounded-xl border border-error/30 bg-error/5 p-4 text-sm text-error">
                            Batches could not be loaded: {batchesQuery.error.message}
                        </p>
                    )}
                    {!batchesQuery.isPending && !batchesQuery.isError && batchesQuery.data.length === 0 && (
                        <p className="rounded-xl border border-dashed border-gray-300 bg-white px-5 py-8 text-center text-sm text-gray-600">
                            Create a batch before viewing or registering its movements.
                        </p>
                    )}
                    {!batchesQuery.isPending && !batchesQuery.isError && batchesQuery.data.length > 0 && (
                        <>
                            <div className="max-w-md">
                                <label
                                    htmlFor="movement-history-batch"
                                    className="mb-1.5 block text-xs tracking-wide text-primary uppercase"
                                >
                                    Batch
                                </label>
                                <select
                                    id="movement-history-batch"
                                    value={activeBatchId}
                                    onChange={(event) => setSelectedBatchId(event.target.value)}
                                    className="w-full rounded-md border border-line bg-white px-2.5 py-2 text-sm"
                                >
                                    {batchesQuery.data.map((batch) => (
                                        <option key={batch.id} value={batch.id}>
                                            {batch.batchCode ?? batch.batch_code ?? `Batch ${batch.id}`}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            {historyQuery.isPending && (
                                <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-8 text-sm text-gray-500">
                                    <Loader2 size={18} className="animate-spin" />
                                    Loading movement history…
                                </div>
                            )}
                            {historyQuery.isError && (
                                <div role="alert" className="flex items-center gap-3 rounded-xl border border-error/30 bg-error/5 p-4 text-sm text-error">
                                    <TriangleAlert size={20} />
                                    Movement history could not be loaded: {historyQuery.error.message}
                                    <button
                                        type="button"
                                        onClick={() => historyQuery.refetch()}
                                        className="ml-auto font-semibold underline"
                                    >
                                        Retry
                                    </button>
                                </div>
                            )}
                            {!historyQuery.isPending && !historyQuery.isError && (
                                historyQuery.data.length > 0 ? (
                                    <MovementsTable movements={historyQuery.data} />
                                ) : (
                                    <p className="rounded-xl border border-dashed border-gray-300 bg-white px-5 py-8 text-center text-sm text-gray-600">
                                        No movements recorded for this batch yet. Register one below to build its history.
                                    </p>
                                )
                            )}
                        </>
                    )}
                </section>

                {zonesQuery.isPending && (
                    <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-6 text-sm text-gray-500">
                        <Loader2 size={18} className="animate-spin" />
                        Loading storage zones…
                    </div>
                )}
                {zonesQuery.isError && (
                    <p role="alert" className="rounded-xl border border-error/30 bg-error/5 p-4 text-sm text-error">
                        Storage zones could not be loaded: {zonesQuery.error.message}
                    </p>
                )}
                {!zonesQuery.isPending && !zonesQuery.isError && zonesQuery.data.length === 0 && (
                    <p className="rounded-xl border border-dashed border-gray-300 bg-white px-5 py-6 text-sm text-gray-600">
                        No storage zones available. Create a zone before moving stock into storage.
                    </p>
                )}
                <MovementForm
                    batches={batchesQuery.data ?? []}
                    zones={zonesQuery.data ?? []}
                    isSubmitting={createMutation.isPending}
                    fieldErrors={fieldErrors}
                    submitError={submitError}
                    successNote={createMutation.isSuccess ? "Movement registered successfully." : null}
                    onSubmit={registerMovement}
                    onClearFeedback={() => createMutation.reset()}
                />
            </div>
        </div>
    );
}
