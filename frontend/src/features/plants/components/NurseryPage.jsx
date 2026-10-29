import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useAddNurseryBatch } from "../hooks/useAddNurseryBatch";
import { useNurseryPlannedBlocks } from "../hooks/useNurseryBatches";
import { useRecordNurseryEvent } from "../hooks/useRecordNurseryEvent";
import AddNurseryBatchModal from "./AddNurseryBatchModal";
import BlockFilter from "./BlockFilter";
import NurseryAlertsSection from "./NurseryAlertsSection";
import NurseryBatchesSection from "./NurseryBatchesSection";
import NurseryHistorySection from "./NurseryHistorySection";
import RecordNurseryEventModal from "./RecordNurseryEventModal";

function AddBatchButton({ onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
        >
            <Plus size={16} />
            Add a batch
        </button>
    );
}

function RecordEventButton({ onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90"
        >
            <Plus size={16} />
            Record an event
        </button>
    );
}

/**
 * Nursery: the batches to transplant today, then the batches with their
 * survival rate, filtered by destination block, and last what happened to
 * them, which the mock-up does not show.
 *
 * The filter offers the blocks the batches are planned for. The mock-up's
 * Home button is left out, as on the other screens, and its "Lot" reads
 * "Batch", as the API's messages shown on screen say.
 */
export default function NurseryPage() {
    // Raw stored block value ("E"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");
    const [isAddingBatch, setIsAddingBatch] = useState(false);
    const [isRecordingEvent, setIsRecordingEvent] = useState(false);

    useEffect(() => {
        document.title = "Nursery — Plants";
    }, []);

    const { data: blocks, isPending: isLoadingBlocks, isError: hasBlocksError } = useNurseryPlannedBlocks();
    const addBatch = useAddNurseryBatch();
    const recordEvent = useRecordNurseryEvent();

    function openBatchForm() {
        addBatch.reset();
        setIsAddingBatch(true);
    }

    function closeBatchForm() {
        addBatch.reset();
        setIsAddingBatch(false);
    }

    function submitBatch(batch) {
        addBatch.mutate(batch, { onSuccess: closeBatchForm });
    }

    function openEventForm() {
        recordEvent.reset();
        setIsRecordingEvent(true);
    }

    function closeEventForm() {
        recordEvent.reset();
        setIsRecordingEvent(false);
    }

    function submitEvent(nurseryEvent) {
        recordEvent.mutate(nurseryEvent, { onSuccess: closeEventForm });
    }

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                        {/* primary-dark, not primary: #065e55 on the gray-50 page -> 7.33:1.
                            primary measured 4.498:1, just under the AA minimum of 4.5:1. */}
                        <p className="font-heading text-xs font-bold tracking-widest text-primary-dark uppercase">
                            Task 6
                        </p>
                        <h2 className="font-heading mt-1 text-3xl font-bold text-gray-900">
                            Nursery
                        </h2>
                        <p className="mt-2 max-w-xl text-sm text-gray-500">
                            Seedling batches before transplanting to their destination block.
                        </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        <AddBatchButton onClick={openBatchForm} />
                        <RecordEventButton onClick={openEventForm} />
                    </div>
                </header>

                <NurseryAlertsSection />

                <BlockFilter
                    label="Destination block:"
                    blocks={blocks ?? []}
                    value={selectedBlock}
                    onChange={setSelectedBlock}
                    disabled={isLoadingBlocks || hasBlocksError}
                />

                <NurseryBatchesSection
                    blockCode={selectedBlock || null}
                    addAction={<AddBatchButton onClick={openBatchForm} />}
                />

                <NurseryHistorySection />
            </div>

            {isAddingBatch && (
                <AddNurseryBatchModal
                    isSubmitting={addBatch.isPending}
                    error={addBatch.error}
                    onSubmit={submitBatch}
                    onResetError={addBatch.reset}
                    onClose={closeBatchForm}
                />
            )}

            {isRecordingEvent && (
                <RecordNurseryEventModal
                    isSubmitting={recordEvent.isPending}
                    error={recordEvent.error}
                    onSubmit={submitEvent}
                    onResetError={recordEvent.reset}
                    onClose={closeEventForm}
                />
            )}
        </div>
    );
}
