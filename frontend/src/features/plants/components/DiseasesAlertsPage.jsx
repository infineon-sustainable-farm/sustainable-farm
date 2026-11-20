import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, PageHeader } from "../../../shared/ui";
import { useInspectedBlocks } from "../hooks/useBlockHealth";
import { useRecordInspection } from "../hooks/useRecordInspection";
import { useRecordTreatment } from "../hooks/useRecordTreatment";
import { useResolveFinding } from "../hooks/useResolveFinding";
import { useTreatedBlocks } from "../hooks/useTreatments";
import { useVarietyBlocks } from "../hooks/useVarieties";
import BlockFilter from "./BlockFilter";
import BlockHealthSection from "./BlockHealthSection";
import FindingsSection from "./FindingsSection";
import HealthAlertsSection from "./HealthAlertsSection";
import HealthIssuesSection from "./HealthIssuesSection";
import RecordInspectionModal from "./RecordInspectionModal";
import ResolveFindingModal from "./ResolveFindingModal";
import TreatmentModal from "./TreatmentModal";
import TreatmentsSection from "./TreatmentsSection";

function RecordInspectionButton({ onClick }) {
    const { t } = useTranslation("plants");
    return (
        <Button icon={Plus} onClick={onClick}>
            {t("health.recordInspection")}
        </Button>
    );
}

function RecordPreventiveTreatmentButton({ onClick }) {
    const { t } = useTranslation("plants");
    return (
        <Button variant="secondary" icon={Plus} onClick={onClick}>
            {t("health.recordPreventiveTreatment")}
        </Button>
    );
}

/**
 * Diseases & Alerts: the health alerts of the day, then the health of each
 * block from its latest inspection, the problems found, the treatment record,
 * and last the catalogue of pests and diseases they are picked from.
 *
 * The health alerts sit above the block filter, as in the mock-up, and cover
 * every block. The filter offers the blocks of the recorded varieties, the
 * inspected blocks and the treated blocks, as an inspection or a preventive
 * treatment needs no planting.
 *
 * Left out of the mock-up: the chart of alerts by treatment status, which the
 * status filter of the findings replaces, and in the alert callout the health
 * score, the healthy target range and the View Details button. An alert
 * carries no score, the API gives no band, and there is no detail view.
 */
export default function DiseasesAlertsPage() {
    const { t } = useTranslation("plants");
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");
    const [isRecordingInspection, setIsRecordingInspection] = useState(false);
    // undefined: no dialog; null: a preventive treatment; otherwise the finding to treat.
    const [treatedFinding, setTreatedFinding] = useState(undefined);
    const [resolvedFinding, setResolvedFinding] = useState(null);

    const documentTitle = t("health.documentTitle");
    useEffect(() => {
        document.title = documentTitle;
    }, [documentTitle]);

    const { data: varietyBlocks } = useVarietyBlocks();
    const { data: inspectedBlocks } = useInspectedBlocks();
    const { data: treatedBlocks } = useTreatedBlocks();
    const blocks = [...new Set([...(varietyBlocks ?? []), ...(inspectedBlocks ?? []), ...(treatedBlocks ?? [])])].sort();
    const recordInspection = useRecordInspection();
    const recordTreatment = useRecordTreatment();
    const resolveFinding = useResolveFinding();

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

    function openTreatmentForm(finding) {
        recordTreatment.reset();
        setTreatedFinding(finding);
    }

    function closeTreatmentForm() {
        recordTreatment.reset();
        setTreatedFinding(undefined);
    }

    function submitTreatment(treatment) {
        recordTreatment.mutate(
            { findingId: treatedFinding === null ? null : treatedFinding.id, treatment },
            { onSuccess: closeTreatmentForm },
        );
    }

    function openResolutionForm(finding) {
        resolveFinding.reset();
        setResolvedFinding(finding);
    }

    function closeResolutionForm() {
        resolveFinding.reset();
        setResolvedFinding(null);
    }

    function submitResolution(resolution) {
        resolveFinding.mutate({ findingId: resolvedFinding.id, resolution }, { onSuccess: closeResolutionForm });
    }

    return (
        <div className="p-6 space-y-6">
            <PageHeader
                title={t("health.title")}
                subtitle={t("health.subtitle")}
                actions={
                    <>
                        <RecordPreventiveTreatmentButton onClick={() => openTreatmentForm(null)} />
                        <RecordInspectionButton onClick={openInspectionForm} />
                    </>
                }
            />

            <HealthAlertsSection />

            <BlockFilter blocks={blocks} value={selectedBlock} onChange={setSelectedBlock} />

            <BlockHealthSection
                blockCode={selectedBlock || null}
                recordAction={<RecordInspectionButton onClick={openInspectionForm} />}
            />

            <FindingsSection
                blockCode={selectedBlock || null}
                recordAction={<RecordInspectionButton onClick={openInspectionForm} />}
                onTreat={openTreatmentForm}
                onResolve={openResolutionForm}
            />

            <TreatmentsSection
                blockCode={selectedBlock || null}
                recordAction={<RecordPreventiveTreatmentButton onClick={() => openTreatmentForm(null)} />}
            />

            <HealthIssuesSection />

            {isRecordingInspection && (
                <RecordInspectionModal
                    isSubmitting={recordInspection.isPending}
                    error={recordInspection.error}
                    onSubmit={submitInspection}
                    onResetError={recordInspection.reset}
                    onClose={closeInspectionForm}
                />
            )}

            {treatedFinding !== undefined && (
                <TreatmentModal
                    finding={treatedFinding}
                    isSubmitting={recordTreatment.isPending}
                    error={recordTreatment.error}
                    onSubmit={submitTreatment}
                    onClose={closeTreatmentForm}
                />
            )}

            {resolvedFinding !== null && (
                <ResolveFindingModal
                    finding={resolvedFinding}
                    isSubmitting={resolveFinding.isPending}
                    error={resolveFinding.error}
                    onSubmit={submitResolution}
                    onClose={closeResolutionForm}
                />
            )}
        </div>
    );
}
