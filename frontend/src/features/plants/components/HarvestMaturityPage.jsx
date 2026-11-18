import { useEffect, useState } from "react";
import { CircleCheck, Plus } from "lucide-react";
import { Trans, useTranslation } from "react-i18next";
import { Button, PageHeader } from "../../../shared/ui";
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
    const { t } = useTranslation("plants");
    return (
        <Button icon={Plus} onClick={onClick}>
            {t("harvest.page.recordHarvest")}
        </Button>
    );
}

/** Confirmation of the harvest just recorded, with the lot number to write on its crates. */
function RecordedHarvestMessage({ harvest }) {
    const { t } = useTranslation("plants");
    return (
        <p className="inline-flex items-center gap-3 rounded-tile border border-border-subtle bg-surface-raised px-4 py-3 text-sm text-text">
            <CircleCheck size={18} aria-hidden="true" className="shrink-0 text-success-fg" />
            {harvest.lotNumber ? (
                <span>
                    <Trans
                        t={t}
                        i18nKey="harvest.page.recordedLot"
                        values={{ lot: harvest.lotNumber }}
                        components={{ lot: <span className="font-semibold" /> }}
                    />
                </span>
            ) : (
                t("harvest.page.recorded")
            )}
        </p>
    );
}

/**
 * Harvest & Maturity: the maturity status of the day and the upcoming
 * deadlines first, then the expected yield by month, the reference values
 * they are computed from, and the recorded harvests. One block filter drives
 * the status, the deadlines, the forecast and the harvests; the reference
 * values hold for every block.
 *
 * Each recorded harvest has its own lot number, shown in the recorded
 * harvests and announced as soon as the harvest is recorded.
 */
export default function HarvestMaturityPage() {
    const { t } = useTranslation("plants");
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");
    const [isRecordingHarvest, setIsRecordingHarvest] = useState(false);
    // The harvest just recorded, until the form is opened again.
    const [recordedHarvest, setRecordedHarvest] = useState(null);

    const documentTitle = t("harvest.page.documentTitle");
    useEffect(() => {
        document.title = documentTitle;
    }, [documentTitle]);

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
        <div className="p-6 space-y-6">
            <div className="space-y-4">
                <PageHeader
                    title={t("harvest.page.title")}
                    subtitle={t("harvest.page.subtitle")}
                    actions={<RecordHarvestButton onClick={openHarvestForm} />}
                />
                {/* Always in the page, empty until a harvest is recorded, so the message is announced. */}
                <div role="status">{recordedHarvest && <RecordedHarvestMessage harvest={recordedHarvest} />}</div>
            </div>

            <BlockFilter blocks={blocks ?? []} value={selectedBlock} onChange={setSelectedBlock} />

            <HarvestStatusSection blockCode={selectedBlock || null} />

            <UpcomingDeadlinesSection blockCode={selectedBlock || null} />

            <YieldForecastSection blockCode={selectedBlock || null} />

            <ReferenceValuesSection />

            <HarvestsSection
                blockCode={selectedBlock || null}
                recordAction={<RecordHarvestButton onClick={openHarvestForm} />}
            />

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
