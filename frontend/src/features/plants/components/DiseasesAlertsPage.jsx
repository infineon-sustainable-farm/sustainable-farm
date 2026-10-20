import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useInspectedBlocks } from "../hooks/useBlockHealth";
import { useRecordInspection } from "../hooks/useRecordInspection";
import { useVarietyBlocks } from "../hooks/useVarieties";
import BlockFilter from "./BlockFilter";
import BlockHealthSection from "./BlockHealthSection";
import FindingsSection from "./FindingsSection";
import HealthAlertsSection from "./HealthAlertsSection";
import HealthIssuesSection from "./HealthIssuesSection";
import RecordInspectionModal from "./RecordInspectionModal";

function RecordInspectionButton({ onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90"
        >
            <Plus size={16} />
            Record an inspection
        </button>
    );
}

/**
 * Diseases & Alerts: the health alerts of the day, then the health of each
 * block from its latest inspection, then the problems found, and last the
 * catalogue of pests and diseases they are picked from.
 *
 * The health alerts sit above the block filter, as in the mock-up, and cover
 * every block. The filter offers the blocks of the recorded varieties and the
 * inspected blocks, as an inspection needs no planting.
 *
 * Left out of the mock-up: the chart of alerts by treatment status, which the
 * status filter of the findings replaces, and in the alert callout the health
 * score, the healthy target range and the View Details button. An alert
 * carries no score, the API gives no band, and there is no detail view.
 */
export default function DiseasesAlertsPage() {
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");
    const [isRecordingInspection, setIsRecordingInspection] = useState(false);

    useEffect(() => {
        document.title = "Diseases & Alerts — Plants";
    }, []);

    const { data: varietyBlocks } = useVarietyBlocks();
    const { data: inspectedBlocks } = useInspectedBlocks();
    const blocks = [...new Set([...(varietyBlocks ?? []), ...(inspectedBlocks ?? [])])].sort();
    const recordInspection = useRecordInspection();

    function openInspectionForm() {
        recordInspection.reset();
        setIsRecordingInspection(true);
    }

    function closeInspectionForm() {
        recordInspection.reset();
        setIsRecordingInspection(false);
    }

    function submitInspection(inspection) {
        recordInspection.mutate(inspection, { onSuccess: closeInspectionForm });
    }

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                        {/* primary-dark, not primary: #065e55 on the gray-50 page -> 7.33:1.
                            primary measured 4.498:1, just under the AA minimum of 4.5:1. */}
                        <p className="font-heading text-xs font-bold tracking-widest text-primary-dark uppercase">
                            Task 4
                        </p>
                        <h2 className="font-heading mt-1 text-3xl font-bold text-gray-900">
                            Diseases &amp; Alerts
                        </h2>
                        <p className="mt-2 max-w-xl text-sm text-gray-500">
                            Health by block, inspections, treatments and health alerts.
                        </p>
                    </div>
                    <RecordInspectionButton onClick={openInspectionForm} />
                </header>

                <HealthAlertsSection />

                <BlockFilter blocks={blocks} value={selectedBlock} onChange={setSelectedBlock} />

                <BlockHealthSection
                    blockCode={selectedBlock || null}
                    recordAction={<RecordInspectionButton onClick={openInspectionForm} />}
                />

                <FindingsSection
                    blockCode={selectedBlock || null}
                    recordAction={<RecordInspectionButton onClick={openInspectionForm} />}
                />

                <HealthIssuesSection />
            </div>

            {isRecordingInspection && (
                <RecordInspectionModal
                    blocks={blocks}
                    isSubmitting={recordInspection.isPending}
                    error={recordInspection.error}
                    onSubmit={submitInspection}
                    onResetError={recordInspection.reset}
                    onClose={closeInspectionForm}
                />
            )}
        </div>
    );
}
