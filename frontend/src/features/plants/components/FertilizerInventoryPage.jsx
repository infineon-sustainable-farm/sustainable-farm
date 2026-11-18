import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, PageHeader } from "../../../shared/ui";
import { useAddFertilizer } from "../hooks/useAddFertilizer";
import { useRecordFertilizerMovement } from "../hooks/useRecordFertilizerMovement";
import AddFertilizerModal from "./AddFertilizerModal";
import ExchangeRateSection from "./ExchangeRateSection";
import FertilizerMovementsSection from "./FertilizerMovementsSection";
import FertilizerStockSection from "./FertilizerStockSection";
import RecordMovementModal from "./RecordMovementModal";

function AddFertilizerButton({ onClick }) {
    const { t } = useTranslation("plants");
    return (
        <Button variant="secondary" icon={Plus} onClick={onClick}>
            {t("fertilizer.addFertilizer")}
        </Button>
    );
}

function RecordMovementButton({ onClick }) {
    const { t } = useTranslation("plants");
    return (
        <Button icon={Plus} onClick={onClick}>
            {t("fertilizer.recordMovement")}
        </Button>
    );
}

/**
 * Fertilizer Inventory: the EUR to XOF rate that converts the purchase costs,
 * the current stock of each fertilizer, then the history of its purchases,
 * applications and losses.
 *
 * The mock-up's last restock date and provisional-threshold note are left
 * out: the API records no restock date, and each threshold is the one entered
 * for its fertilizer. Its stock chart is left out too: the API gives no stock
 * history, and kilograms and litres cannot share one axis. Its block filter
 * moves into the history, the only part of the page kept by block.
 */
export default function FertilizerInventoryPage() {
    const { t } = useTranslation("plants");
    const [isAddingFertilizer, setIsAddingFertilizer] = useState(false);
    const [isRecordingMovement, setIsRecordingMovement] = useState(false);
    const pageTitle = t("fertilizer.pageTitle");

    useEffect(() => {
        document.title = pageTitle;
    }, [pageTitle]);

    const addFertilizer = useAddFertilizer();
    const recordMovement = useRecordFertilizerMovement();

    function openFertilizerForm() {
        addFertilizer.reset();
        setIsAddingFertilizer(true);
    }

    function closeFertilizerForm() {
        addFertilizer.reset();
        setIsAddingFertilizer(false);
    }

    function submitFertilizer(fertilizer) {
        addFertilizer.mutate(fertilizer, { onSuccess: closeFertilizerForm });
    }

    function openMovementForm() {
        recordMovement.reset();
        setIsRecordingMovement(true);
    }

    function closeMovementForm() {
        recordMovement.reset();
        setIsRecordingMovement(false);
    }

    function submitMovement(movement) {
        recordMovement.mutate(movement, { onSuccess: closeMovementForm });
    }

    return (
        <div className="p-6 space-y-6">
            <PageHeader
                title={t("fertilizer.title")}
                subtitle={t("fertilizer.subtitle")}
                actions={
                    <div className="flex flex-wrap gap-2">
                        <AddFertilizerButton onClick={openFertilizerForm} />
                        <RecordMovementButton onClick={openMovementForm} />
                    </div>
                }
            />

            <ExchangeRateSection />

            <FertilizerStockSection addAction={<AddFertilizerButton onClick={openFertilizerForm} />} />

            <FertilizerMovementsSection recordAction={<RecordMovementButton onClick={openMovementForm} />} />

            {isAddingFertilizer && (
                <AddFertilizerModal
                    isSubmitting={addFertilizer.isPending}
                    error={addFertilizer.error}
                    onSubmit={submitFertilizer}
                    onClose={closeFertilizerForm}
                />
            )}

            {isRecordingMovement && (
                <RecordMovementModal
                    isSubmitting={recordMovement.isPending}
                    error={recordMovement.error}
                    onSubmit={submitMovement}
                    onResetError={recordMovement.reset}
                    onClose={closeMovementForm}
                />
            )}
        </div>
    );
}
