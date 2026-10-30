import { useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, TriangleAlert } from "lucide-react";
import {
    createBatch,
    fetchBatches,
    fetchStorageZones,
    fetchSuppliers,
} from "../api";
import BatchesTable from "./BatchesTable";
import BatchForm from "./BatchForm";

export default function BatchesPage() {
    const queryClient = useQueryClient();

    useEffect(() => {
        document.title = "Batches — Crop Storage";
    }, []);

    const batchesQuery = useQuery({
        queryKey: ["cropstorage", "batches"],
        queryFn: async () => {
            const [batches, zones, suppliers] = await Promise.all([
                fetchBatches(),
                fetchStorageZones(),
                fetchSuppliers(),
            ]);
            return { batches, zones, suppliers };
        },
    });

    const createMutation = useMutation({
        mutationFn: createBatch,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cropstorage"] }),
    });

    const fieldErrors =
        createMutation.error?.status === 400 ? createMutation.error.data?.fieldErrors : null;
    const submitError =
        createMutation.error?.status !== 400 ? createMutation.error?.message : null;

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header>
                    <p className="font-heading text-xs font-bold tracking-widest text-primary uppercase">
                        Crop Storage
                    </p>
                    <h1 className="font-heading mt-1 text-3xl font-bold text-gray-900">Batches</h1>
                    <p className="mt-2 text-sm text-gray-500">
                        Inventory batch records and traceability details.
                    </p>
                </header>

                {batchesQuery.isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-12 text-sm text-gray-500">
                        <Loader2 size={18} className="animate-spin" />
                        Loading batches…
                    </div>
                )}
                {batchesQuery.isError && (
                    <div role="alert" className="flex items-center gap-3 rounded-xl border border-error/30 bg-error/5 p-4 text-sm text-error">
                        <TriangleAlert size={20} />
                        Batches could not be loaded: {batchesQuery.error.message}
                        <button
                            type="button"
                            onClick={() => batchesQuery.refetch()}
                            className="ml-auto font-semibold underline"
                        >
                            Retry
                        </button>
                    </div>
                )}
                {!batchesQuery.isPending && !batchesQuery.isError && (
                    batchesQuery.data.batches.length > 0 ? (
                        <BatchesTable batches={batchesQuery.data.batches} />
                    ) : (
                        <div className="rounded-xl border border-dashed border-gray-300 bg-white px-6 py-10 text-center text-sm text-gray-600">
                            No batches in inventory yet. Create one below to get started.
                        </div>
                    )
                )}

                <BatchForm
                    zones={batchesQuery.data?.zones ?? []}
                    suppliers={batchesQuery.data?.suppliers ?? []}
                    isSubmitting={createMutation.isPending}
                    fieldErrors={fieldErrors}
                    submitError={submitError}
                    success={createMutation.isSuccess}
                    onSubmit={(batch) => createMutation.mutate(batch)}
                    onClearError={() => createMutation.reset()}
                />
            </div>
        </div>
    );
}
