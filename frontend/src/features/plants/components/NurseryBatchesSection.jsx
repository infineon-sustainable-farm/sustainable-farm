import { useTranslation } from "react-i18next";
import { Card, EmptyState, ErrorState } from "../../../shared/ui";
import { useNurseryBatches } from "../hooks/useNurseryBatches";
import NurseryBatchesTable from "./NurseryBatchesTable";
import NurserySurvivalCards from "./NurserySurvivalCards";

/**
 * The nursery batches, then the survival rate of each, for the selected
 * destination block. Counts, rates and stages all come from the API.
 *
 * The API takes no block filter for the batches, so the batches planned for
 * the selected block are picked out of the list here; nothing else is
 * computed. A batch with no planned block shows only with "All blocks".
 *
 * `addAction` is the page's "Add a batch" button, offered again when no batch
 * is recorded at all.
 */
export default function NurseryBatchesSection({ blockCode, addAction }) {
    const { t } = useTranslation("plants");
    const { data: batches, isPending, isError, refetch, isFetching } = useNurseryBatches();
    const shown = blockCode ? (batches ?? []).filter((batch) => batch.plannedBlockCode === blockCode) : batches;

    return (
        <Card
            aria-labelledby="nursery-batches-title"
            title={<span id="nursery-batches-title">{t("nursery.batches.title")}</span>}
        >
            {isPending && <NurseryBatchesTable loading />}

            {isError && (
                <ErrorState
                    message={`${t("nursery.batches.error")}. ${t("nursery.batches.errorHint")}`}
                    onRetry={() => refetch()}
                    retrying={isFetching}
                    retryLabel={t("common.retry")}
                />
            )}

            {!isPending && !isError && batches.length === 0 && (
                <EmptyState message={t("nursery.batches.empty")} action={addAction} />
            )}

            {!isPending && !isError && batches.length > 0 && shown.length === 0 && (
                <EmptyState
                    title={t("nursery.batches.emptyForBlock")}
                    message={t("nursery.batches.emptyForBlockHint")}
                />
            )}

            {!isPending && !isError && shown.length > 0 && (
                <div className="flex flex-col gap-6">
                    <NurseryBatchesTable batches={shown} />
                    <NurserySurvivalCards batches={shown} />
                </div>
            )}
        </Card>
    );
}
