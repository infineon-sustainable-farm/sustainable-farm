import { useEffect, useState } from "react";
import { useNurseryPlannedBlocks } from "../hooks/useNurseryBatches";
import BlockFilter from "./BlockFilter";
import NurseryAlertsSection from "./NurseryAlertsSection";
import NurseryBatchesSection from "./NurseryBatchesSection";
import NurseryHistorySection from "./NurseryHistorySection";

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

    useEffect(() => {
        document.title = "Nursery — Plants";
    }, []);

    const { data: blocks, isPending: isLoadingBlocks, isError: hasBlocksError } = useNurseryPlannedBlocks();

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
                </header>

                <NurseryAlertsSection />

                <BlockFilter
                    label="Destination block:"
                    blocks={blocks ?? []}
                    value={selectedBlock}
                    onChange={setSelectedBlock}
                    disabled={isLoadingBlocks || hasBlocksError}
                />

                <NurseryBatchesSection blockCode={selectedBlock || null} />

                <NurseryHistorySection />
            </div>
        </div>
    );
}
