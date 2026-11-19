import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Plus } from "lucide-react";
import { Button, Card, EmptyState, ErrorState, PageHeader, Skeleton } from "../../../shared/ui";
import { useVarieties, useVarietyBlocks } from "../hooks/useVarieties";
import { useRecordPlanting } from "../hooks/useRecordPlanting";
import BlockFilter from "./BlockFilter";
import RecordPlantingModal from "./RecordPlantingModal";
import VarietiesTable from "./VarietiesTable";
import VarietyDetailModal from "./VarietyDetailModal";
import YieldComparisonSection from "./YieldComparisonSection";

function RecordPlantingButton({ onClick }) {
    const { t } = useTranslation("plants");
    return (
        <Button icon={Plus} onClick={onClick}>
            {t("varieties.recordPlanting")}
        </Button>
    );
}

export default function VarietiesPage() {
    const { t } = useTranslation("plants");
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");
    const [selectedVariety, setSelectedVariety] = useState(null);
    const [isRecordingPlanting, setIsRecordingPlanting] = useState(false);

    const documentTitle = t("varieties.documentTitle");
    useEffect(() => {
        document.title = documentTitle;
    }, [documentTitle]);

    const { data: varieties, isPending, isError, refetch, isFetching } = useVarieties({
        blockCode: selectedBlock || null,
    });
    const { data: blocks } = useVarietyBlocks();
    const recordPlanting = useRecordPlanting();

    function openPlantingForm() {
        recordPlanting.reset();
        setIsRecordingPlanting(true);
    }

    function closePlantingForm() {
        recordPlanting.reset();
        setIsRecordingPlanting(false);
    }

    function submitPlanting(planting) {
        recordPlanting.mutate(planting, { onSuccess: closePlantingForm });
    }

    return (
        <div className="p-6 space-y-6">
            <PageHeader
                title={t("varieties.title")}
                subtitle={t("varieties.subtitle")}
                actions={<RecordPlantingButton onClick={openPlantingForm} />}
            />

            <BlockFilter
                blocks={blocks ?? []}
                value={selectedBlock}
                onChange={setSelectedBlock}
                disabled={isPending}
            />

            {isPending && (
                <Card aria-busy="true">
                    <p role="status" className="sr-only">
                        {t("varieties.loading")}
                    </p>
                    <div className="flex flex-col gap-3">
                        {[0, 1, 2, 3].map((index) => (
                            <Skeleton key={index} className="h-6 w-full" />
                        ))}
                    </div>
                </Card>
            )}

            {isError && (
                <Card>
                    <ErrorState
                        message={
                            <>
                                <span className="block font-semibold">{t("varieties.loadError")}</span>
                                <span className="block">{t("varieties.requestFailed")}</span>
                            </>
                        }
                        onRetry={() => refetch()}
                        retrying={isFetching}
                        retryLabel={t("common.retry")}
                    />
                </Card>
            )}

            {!isPending && !isError && varieties.length === 0 && (
                <Card>
                    {selectedBlock ? (
                        <EmptyState
                            title={t("varieties.emptyFiltered")}
                            message={t("varieties.emptyFilteredHint")}
                        />
                    ) : (
                        <EmptyState
                            title={t("varieties.empty")}
                            message=""
                            action={<RecordPlantingButton onClick={openPlantingForm} />}
                        />
                    )}
                </Card>
            )}

            {!isPending && !isError && varieties.length > 0 && (
                <VarietiesTable varieties={varieties} onSelect={setSelectedVariety} />
            )}

            <YieldComparisonSection blockCode={selectedBlock || null} />

            {selectedVariety && (
                <VarietyDetailModal variety={selectedVariety} onClose={() => setSelectedVariety(null)} />
            )}

            {isRecordingPlanting && (
                <RecordPlantingModal
                    isSubmitting={recordPlanting.isPending}
                    error={recordPlanting.error}
                    onSubmit={submitPlanting}
                    onClose={closePlantingForm}
                />
            )}
        </div>
    );
}
