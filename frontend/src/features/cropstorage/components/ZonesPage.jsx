import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { EmptyState, PageHeader } from "../../../shared/ui";
import { createStorageZone, fetchBatches, fetchStorageZones } from "../api";
import { usePageTitle } from "../utils/usePageTitle";
import { withZoneUsage } from "../utils/storageZones";
import { fieldErrorsOf, submitErrorOf } from "../utils/mutationErrors";
import QueryState from "./QueryState";
import StorageZonesTable from "./StorageZonesTable";
import ZoneForm from "./ZoneForm";

export default function ZonesPage() {
    const { t } = useTranslation("cropstorage");
    const queryClient = useQueryClient();
    usePageTitle("zones.title");

    const zonesQuery = useQuery({
        queryKey: ["cropstorage", "zones"],
        queryFn: async () => {
            const [zones, batches] = await Promise.all([fetchStorageZones(), fetchBatches()]);
            return withZoneUsage(zones, batches);
        },
    });

    const createMutation = useMutation({
        mutationFn: createStorageZone,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cropstorage"] }),
    });

    return (
        <div className="flex flex-col gap-6">
            <PageHeader title={t("zones.title")} subtitle={t("zones.subtitle")} />
            <QueryState query={zonesQuery} errorKey="zones.loadError" />
            {zonesQuery.isSuccess &&
                (zonesQuery.data.length > 0 ? (
                    <StorageZonesTable zones={zonesQuery.data} />
                ) : (
                    <EmptyState message={t("zones.empty")} />
                ))}
            <ZoneForm
                isSubmitting={createMutation.isPending}
                fieldErrors={fieldErrorsOf(createMutation)}
                submitError={submitErrorOf(createMutation, t)}
                success={createMutation.isSuccess}
                onSubmit={(zone) => createMutation.mutate(zone)}
                onClearError={() => createMutation.reset()}
            />
        </div>
    );
}
