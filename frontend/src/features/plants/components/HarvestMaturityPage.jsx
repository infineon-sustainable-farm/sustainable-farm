import { useEffect, useState } from "react";
import { CircleCheck, Plus } from "lucide-react";
import { useVarietyBlocks } from "../hooks/useVarieties";
import { useRecordHarvest } from "../hooks/useRecordHarvest";
import BlockFilter from "./BlockFilter";
import HarvestStatusSection from "./HarvestStatusSection";
import HarvestsSection from "./HarvestsSection";
import RecordHarvestModal from "./RecordHarvestModal";
import ReferenceValuesSection from "./ReferenceValuesSection";
import UpcomingDeadlinesSection from "./UpcomingDeadlinesSection";
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

/** Confirmation of the harvest just recorded, with the lot number to write on its crates. */
function RecordedHarvestMessage({ harvest }) {
    return (
        <p className="mt-4 inline-flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-700">
            <CircleCheck size={18} aria-hidden="true" className="shrink-0 text-success" />
            {harvest.lotNumber ? (
                <span>
                    Harvest recorded — lot <span className="font-semibold text-gray-900">{harvest.lotNumber}</span>
                </span>
            ) : (
                "Harvest recorded"
            )}
        </p>
    );
}

/**
 * Harvest & Maturity: the maturity status of the day and the upcoming
 * deadlines first, as in the mock-up, then the expected yield by month, the
 * reference values they are computed from, and the recorded harvests. One
 * block filter drives the status, the deadlines, the forecast and the
 * harvests; the reference values hold for every block.
 *
 * Each recorded harvest has its own lot number, shown in the recorded
 * harvests and announced as soon as the harvest is recorded. The mock-up's
 * variance and harvested quantity are left out: the API computes neither.
 */
export default function HarvestMaturityPage() {
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");
    const [isRecordingHarvest, setIsRecordingHarvest] = useState(false);
    // The harvest just recorded, until the form is opened again.
    const [recordedHarvest, setRecordedHarvest] = useState(null);

    useEffect(() => {
        document.title = "Harvest & Maturity — Plants";
    }, []);

    const { data: blocks } = useVarietyBlocks();
    const recordHarvest = useRecordHarvest();

    function openHarvestForm() {
        recordHarvest.reset();
        setRecordedHarvest(null);
        setIsRecordingHarvest(true);
    }

    function closeHarvestForm() {
        recordHarvest.reset();
        setIsRecordingHarvest(false);
    }

    function submitHarvest(harvest) {
        recordHarvest.mutate(harvest, {
            onSuccess: (recorded) => {
                setRecordedHarvest(recorded);
                closeHarvestForm();
            },
        });
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
                            Maturity status, harvest deadlines, expected yield and recorded harvests, by block.
                        </p>
                        {/* Always in the page, empty until a harvest is recorded, so the message is announced. */}
                        <div role="status">
                            {recordedHarvest && <RecordedHarvestMessage harvest={recordedHarvest} />}
                        </div>
                    </div>
                    <RecordHarvestButton onClick={openHarvestForm} />
                </header>

                <BlockFilter blocks={blocks ?? []} value={selectedBlock} onChange={setSelectedBlock} />

                <HarvestStatusSection blockCode={selectedBlock || null} />

                <UpcomingDeadlinesSection blockCode={selectedBlock || null} />

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
