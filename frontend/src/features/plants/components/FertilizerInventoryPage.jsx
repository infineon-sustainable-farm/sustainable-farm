import { useEffect } from "react";
import FertilizerMovementsSection from "./FertilizerMovementsSection";
import FertilizerStockSection from "./FertilizerStockSection";

/**
 * Fertilizer Inventory: the current stock of each fertilizer, then the history
 * of its purchases, applications and losses.
 *
 * The mock-up's last restock date and provisional-threshold note are left
 * out: the API records no restock date, and each threshold is the one entered
 * for its fertilizer. Its stock chart is left out too: the API gives no stock
 * history, and kilograms and litres cannot share one axis. Its block filter
 * moves into the history, the only part of the page kept by block.
 */
export default function FertilizerInventoryPage() {
    useEffect(() => {
        document.title = "Fertilizer Inventory — Plants";
    }, []);

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
                </header>

                <FertilizerStockSection />

                <FertilizerMovementsSection />
            </div>
        </div>
    );
}
