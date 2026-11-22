import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { EmptyState, PageHeader } from "../../../shared/ui";
import { createBatch, fetchBatches, fetchStorageZones, fetchSuppliers, fetchVarieties } from "../api";
import { usePageTitle } from "../utils/usePageTitle";
import { fieldErrorsOf, submitErrorOf } from "../utils/mutationErrors";
import BatchesTable from "./BatchesTable";
import BatchForm from "./BatchForm";
import QueryState from "./QueryState";

export default function BatchesPage() {
    const { t } = useTranslation("cropstorage");
    const queryClient = useQueryClient();
    usePageTitle("batches.title");

    const batchesQuery = useQuery({
        queryKey: ["cropstorage", "batches"],
        queryFn: async () => {
            const [batches, zones, suppliers] = await Promise.all([fetchBatches(), fetchStorageZones(), fetchSuppliers()]);
            return { batches, zones, suppliers };
        },
    });
    const varietiesQuery = useQuery({ queryKey: ["cropstorage", "varieties"], queryFn: fetchVarieties });

    const createMutation = useMutation({
        mutationFn: createBatch,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cropstorage"] }),
    });

    return (
        <div className="flex flex-col gap-6">
            <PageHeader title={t("batches.title")} subtitle={t("batches.subtitle")} />
            <QueryState query={batchesQuery} errorKey="batches.loadError" />
            {batchesQuery.isSuccess &&
                (batchesQuery.data.batches.length > 0 ? (
                    <BatchesTable batches={batchesQuery.data.batches} />
                ) : (
                    <EmptyState message={t("batches.empty")} />
                ))}
            <BatchForm
                zones={batchesQuery.data?.zones ?? []}
                suppliers={batchesQuery.data?.suppliers ?? []}
                varieties={varietiesQuery}
                isSubmitting={createMutation.isPending}
                fieldErrors={fieldErrorsOf(createMutation)}
                submitError={submitErrorOf(createMutation, t)}
                success={createMutation.isSuccess}
                onSubmit={(batch) => createMutation.mutate(batch)}
                onClearError={() => createMutation.reset()}
            />
        </div>
    );
}
