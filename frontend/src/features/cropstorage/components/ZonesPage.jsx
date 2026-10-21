import { useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, TriangleAlert } from "lucide-react";
import { createStorageZone, fetchBatches, fetchStorageZones } from "../api";
import StorageZonesTable from "./StorageZonesTable";
import ZoneForm from "./ZoneForm";
import { withZoneUsage } from "../utils/storageZones";

export default function ZonesPage() {
    const queryClient = useQueryClient();

    useEffect(() => {
        document.title = "Storage Zones — Crop Storage";
    }, []);

    const zonesQuery = useQuery({
        queryKey: ["cropstorage", "zones"],
        queryFn: async () => {
            const [zones, batches] = await Promise.all([fetchStorageZones(), fetchBatches()]);
            return withZoneUsage(zones, batches);
        },
    });

    const createMutation = useMutation({
        mutationFn: createStorageZone,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["cropstorage"] });
        },
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
                    <h1 className="font-heading mt-1 text-3xl font-bold text-gray-900">Storage Zones</h1>
                    <p className="mt-2 text-sm text-gray-500">
                        Review zone capacity and add storage areas.
                    </p>
                </header>

                {zonesQuery.isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-12 text-sm text-gray-500">
                        <Loader2 size={18} className="animate-spin" />
                        Loading storage zones…
                    </div>
                )}
                {zonesQuery.isError && (
                    <div role="alert" className="flex items-center gap-3 rounded-xl border border-error/30 bg-error/5 p-4 text-sm text-error">
                        <TriangleAlert size={20} />
                        Storage zones could not be loaded: {zonesQuery.error.message}
                        <button
                            type="button"
                            onClick={() => zonesQuery.refetch()}
                            className="ml-auto font-semibold underline"
                        >
                            Retry
                        </button>
                    </div>
                )}
                {!zonesQuery.isPending && !zonesQuery.isError && (
                    zonesQuery.data.length > 0 ? (
                        <StorageZonesTable zones={zonesQuery.data} />
                    ) : (
                        <div className="rounded-xl border border-dashed border-gray-300 bg-white px-6 py-10 text-center text-sm text-gray-600">
                            No storage zones yet. Create one below to see its capacity here.
                        </div>
                    )
                )}

                <ZoneForm
                    isSubmitting={createMutation.isPending}
                    fieldErrors={fieldErrors}
                    submitError={submitError}
                    onSubmit={(zone) => createMutation.mutate(zone)}
                    onClearError={() => createMutation.reset()}
                />
            </div>
        </div>
    );
}
