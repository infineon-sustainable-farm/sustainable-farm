import { useRef, useState } from "react";
import { ChevronRight, Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Card, EmptyState } from "../../../shared/ui";
import { useHarvestStatus } from "../hooks/useHarvestStatus";
import { useRecordMaturityObservation } from "../hooks/useRecordMaturityObservation";
import { useVarietyBlocks } from "../hooks/useVarieties";
import { formatDate } from "../utils/format";
import { SectionError, SectionLoading } from "./HarvestSectionStates";
import HarvestStatusTable from "./HarvestStatusTable";
import RecordMaturityObservationModal from "./RecordMaturityObservationModal";

/**
 * How the statuses are read, folded under the table: closed by default, opened
 * by a real button (keyboard, aria-expanded), as the rules matter once, not on
 * every visit. The day and the look-ahead are the ones the API used.
 */
function EstimationRules({ today, withinDays }) {
    const { t } = useTranslation("plants");
    const [isOpen, setIsOpen] = useState(false);
    return (
        <div>
            <button
                type="button"
                aria-expanded={isOpen}
                aria-controls="maturity-status-rules"
                onClick={() => setIsOpen((open) => !open)}
                className="inline-flex cursor-pointer items-center gap-1 rounded-control text-sm font-semibold text-text-accent hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring"
            >
                <ChevronRight size={16} aria-hidden="true" className={isOpen ? "rotate-90" : ""} />
                {t("harvest.status.rulesToggle")}
            </button>
            <div id="maturity-status-rules" hidden={!isOpen} className="mt-2 flex max-w-3xl flex-col gap-2 text-sm text-text-muted">
                <p>{t("harvest.status.rulesEstimated", { date: formatDate(today), days: withinDays })}</p>
                <p>{t("harvest.status.rulesObserved")}</p>
                <p>{t("harvest.status.rulesWindow")}</p>
            </div>
        </div>
    );
}

/**
 * The maturity status of the day of the planted varieties of the selected
 * block, as the API gives it: the estimate, computed from the harvest window
 * of each variety and the pre-harvest intervals of its block, and the field
 * observation that takes precedence over it. The rules are stated with the
 * day and the look-ahead the API used; nothing of the status is computed here.
 *
 * The section also records the maturity seen in the field: its dialog lives
 * here, beside the statuses it changes, and focus goes back to its button
 * when the dialog closes.
 */
export default function HarvestStatusSection({ blockCode }) {
    const { t } = useTranslation("plants");
    const query = useHarvestStatus({ blockCode });
    const { data, isPending, isError } = query;
    const { data: blocks } = useVarietyBlocks();
    const recordObservation = useRecordMaturityObservation();
    const [isRecordingObservation, setIsRecordingObservation] = useState(false);
    const recordButtonRef = useRef(null);

    function openObservationForm() {
        recordObservation.reset();
        setIsRecordingObservation(true);
    }

    function closeObservationForm() {
        recordObservation.reset();
        setIsRecordingObservation(false);
        recordButtonRef.current?.focus();
    }

    function submitObservation(observation) {
        recordObservation.mutate(observation, { onSuccess: closeObservationForm });
    }

    return (
        <section aria-labelledby="maturity-status-title" className="flex flex-col gap-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                    <h2 id="maturity-status-title" className="text-lg font-semibold">
                        {t("harvest.status.title")}
                    </h2>
                    <p className="text-sm text-text-muted">{t("harvest.status.subtitle")}</p>
                </div>
                <Button ref={recordButtonRef} variant="secondary" icon={Plus} onClick={openObservationForm}>
                    {t("harvest.status.record")}
                </Button>
            </div>

            {isPending && <SectionLoading label={t("harvest.status.loading")} />}

            {isError && <SectionError title={t("harvest.status.error")} query={query} />}

            {!isPending && !isError && data.varieties.length === 0 && (
                <Card>
                    {blockCode ? (
                        <EmptyState
                            title={t("harvest.status.emptyBlockTitle")}
                            message={t("harvest.status.emptyBlockHint")}
                        />
                    ) : (
                        <EmptyState title={t("harvest.status.emptyTitle")} message={t("harvest.status.emptyHint")} />
                    )}
                </Card>
            )}

            {!isPending && !isError && data.varieties.length > 0 && (
                <>
                    <HarvestStatusTable statuses={data.varieties} />
                    <EstimationRules today={data.today} withinDays={data.withinDays} />
                </>
            )}

            {isRecordingObservation && (
                <RecordMaturityObservationModal
                    blocks={blocks ?? []}
                    isSubmitting={recordObservation.isPending}
                    error={recordObservation.error}
                    onSubmit={submitObservation}
                    onClose={closeObservationForm}
                />
            )}
        </section>
    );
}
