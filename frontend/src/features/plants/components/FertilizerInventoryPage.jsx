import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useAddFertilizer } from "../hooks/useAddFertilizer";
import { useRecordFertilizerMovement } from "../hooks/useRecordFertilizerMovement";
import AddFertilizerModal from "./AddFertilizerModal";
import ExchangeRateSection from "./ExchangeRateSection";
import FertilizerMovementsSection from "./FertilizerMovementsSection";
import FertilizerStockSection from "./FertilizerStockSection";
import RecordMovementModal from "./RecordMovementModal";

function AddFertilizerButton({ onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
        >
            <Plus size={16} />
            Add a fertilizer
        </button>
    );
}

function RecordMovementButton({ onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90"
        >
            <Plus size={16} />
            Record a movement
        </button>
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
    const [isAddingFertilizer, setIsAddingFertilizer] = useState(false);
    const [isRecordingMovement, setIsRecordingMovement] = useState(false);

    useEffect(() => {
        document.title = "Fertilizer Inventory — Plants";
    }, []);

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
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                        {/* primary-dark, not primary: #065e55 on the gray-50 page -> 7.33:1.
                            primary measured 4.498:1, just under the AA minimum of 4.5:1. */}
                        <p className="font-heading text-xs font-bold tracking-widest text-primary-dark uppercase">
                            Task 3
                        </p>
                        <h2 className="font-heading mt-1 text-3xl font-bold text-gray-900">
                            Fertilizer Inventory
                        </h2>
                        <p className="mt-2 max-w-xl text-sm text-gray-500">
                            Stock, alert thresholds, and the history of purchases, applications and losses.
                        </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        <AddFertilizerButton onClick={openFertilizerForm} />
                        <RecordMovementButton onClick={openMovementForm} />
                    </div>
                </header>

                <ExchangeRateSection />

                <FertilizerStockSection addAction={<AddFertilizerButton onClick={openFertilizerForm} />} />

                <FertilizerMovementsSection recordAction={<RecordMovementButton onClick={openMovementForm} />} />
            </div>

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
