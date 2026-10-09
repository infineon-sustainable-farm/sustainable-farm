import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useVarietyBlocks } from "../hooks/useVarieties";
import { useRecordHarvest } from "../hooks/useRecordHarvest";
import BlockFilter from "./BlockFilter";
import HarvestsSection from "./HarvestsSection";
import RecordHarvestModal from "./RecordHarvestModal";
import ReferenceValuesSection from "./ReferenceValuesSection";
import YieldForecastSection from "./YieldForecastSection";

function RecordHarvestButton({ onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90"
        >
            <Plus size={16} />
            Record a harvest
        </button>
    );
}

/**
 * Harvest & Maturity: the expected yield by month first, as it is what the
 * screen is for, then the reference values it is computed from, then the
 * recorded harvests. One block filter drives the forecast and the harvests;
 * the reference values hold for every block.
 *
 * The mock-up's maturity status, estimated harvest date, variance, lot number
 * and upcoming deadlines are left out: the API records none of them.
 */
export default function HarvestMaturityPage() {
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");
    const [isRecordingHarvest, setIsRecordingHarvest] = useState(false);

    useEffect(() => {
        document.title = "Harvest & Maturity — Plants";
    }, []);

    const { data: blocks } = useVarietyBlocks();
    const recordHarvest = useRecordHarvest();

    function openHarvestForm() {
        recordHarvest.reset();
        setIsRecordingHarvest(true);
    }

    function closeHarvestForm() {
        recordHarvest.reset();
        setIsRecordingHarvest(false);
    }

    function submitHarvest(harvest) {
        recordHarvest.mutate(harvest, { onSuccess: closeHarvestForm });
    }

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                        {/* primary-dark, not primary: #065e55 on the gray-50 page -> 7.33:1.
                            primary measured 4.498:1, just under the AA minimum of 4.5:1. */}
                        <p className="font-heading text-xs font-bold tracking-widest text-primary-dark uppercase">
                            Task 5
                        </p>
                        <h2 className="font-heading mt-1 text-3xl font-bold text-gray-900">
                            Harvest &amp; Maturity
                        </h2>
                        <p className="mt-2 max-w-xl text-sm text-gray-500">
                            Recorded harvests and expected yield by month, by block.
                        </p>
                    </div>
                    <RecordHarvestButton onClick={openHarvestForm} />
                </header>

                <BlockFilter blocks={blocks ?? []} value={selectedBlock} onChange={setSelectedBlock} />

                <YieldForecastSection blockCode={selectedBlock || null} />

                <ReferenceValuesSection />

                <HarvestsSection
                    blockCode={selectedBlock || null}
                    recordAction={<RecordHarvestButton onClick={openHarvestForm} />}
                />
            </div>

            {isRecordingHarvest && (
                <RecordHarvestModal
                    blocks={blocks ?? []}
                    isSubmitting={recordHarvest.isPending}
                    error={recordHarvest.error}
                    onSubmit={submitHarvest}
                    onClose={closeHarvestForm}
                />
            )}
        </div>
    );
}
