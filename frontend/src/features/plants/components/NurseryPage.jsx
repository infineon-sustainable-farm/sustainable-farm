import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, PageHeader } from "../../../shared/ui";
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
    const { t } = useTranslation("plants");
    return (
        <Button variant="secondary" icon={Plus} onClick={onClick}>
            {t("nursery.addBatch")}
        </Button>
    );
}

function RecordEventButton({ onClick }) {
    const { t } = useTranslation("plants");
    return (
        <Button icon={Plus} onClick={onClick}>
            {t("nursery.recordEvent")}
        </Button>
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
    const { t } = useTranslation("plants");
    // Raw stored block value ("E"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");
    const [isAddingBatch, setIsAddingBatch] = useState(false);
    const [isRecordingEvent, setIsRecordingEvent] = useState(false);

    useEffect(() => {
        document.title = t("nursery.documentTitle");
    }, [t]);

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
        <div className="p-6 space-y-6">
            <PageHeader
                title={t("nursery.title")}
                subtitle={t("nursery.subtitle")}
                actions={
                    <>
                        <AddBatchButton onClick={openBatchForm} />
                        <RecordEventButton onClick={openEventForm} />
                    </>
                }
            />

            <NurseryAlertsSection />

            <BlockFilter
                label={t("nursery.destinationBlock")}
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
