import { useEffect, useState } from "react";
import { Loader2, Plus, TriangleAlert } from "lucide-react";
import { useVarieties, useVarietyBlocks } from "../hooks/useVarieties";
import { useRecordPlanting } from "../hooks/useRecordPlanting";
import PlantsEmptyState from "./PlantsEmptyState";
import BlockFilter from "./BlockFilter";
import RecordPlantingModal from "./RecordPlantingModal";
import VarietiesTable from "./VarietiesTable";
import VarietyDetailModal from "./VarietyDetailModal";

function RecordPlantingButton({ onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90"
        >
            <Plus size={16} />
            Record a planting
        </button>
    );
}

export default function VarietiesPage() {
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");
    const [selectedVariety, setSelectedVariety] = useState(null);
    const [isRecordingPlanting, setIsRecordingPlanting] = useState(false);

    useEffect(() => {
        document.title = "Varieties — Plants";
    }, []);

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
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                        <p className="font-heading text-xs font-bold tracking-widest text-primary uppercase">
                            Task 1
                        </p>
                        <h2 className="font-heading mt-1 text-3xl font-bold text-gray-900">
                            Varieties
                        </h2>
                        <p className="mt-2 max-w-xl text-sm text-gray-500">
                            Trees, spacing, and expected vs actual yield by variety and block.
                        </p>
                    </div>
                    <RecordPlantingButton onClick={openPlantingForm} />
                </header>

                <BlockFilter
                    blocks={blocks ?? []}
                    value={selectedBlock}
                    onChange={setSelectedBlock}
                    disabled={isPending}
                />

                {isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                        <Loader2 size={18} className="animate-spin" />
                        Loading varieties…
                    </div>
                )}

                {isError && (
                    <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-gray-900">
                                Varieties could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-gray-500">
                                The request to the server did not succeed.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetch()}
                            disabled={isFetching}
                            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                        >
                            {isFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!isPending && !isError && varieties.length === 0 && (
                    selectedBlock ? (
                        <PlantsEmptyState
                            title="No variety records match this filter"
                            hint="Try another block, or select “All blocks”."
                        />
                    ) : (
                        <PlantsEmptyState
                            title="No planting recorded yet"
                            action={<RecordPlantingButton onClick={openPlantingForm} />}
                        />
                    )
                )}

                {!isPending && !isError && varieties.length > 0 && (
                    <VarietiesTable varieties={varieties} onSelect={setSelectedVariety} />
                )}
            </div>

            {selectedVariety && (
                <VarietyDetailModal
                    variety={selectedVariety}
                    onClose={() => setSelectedVariety(null)}
                />
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
