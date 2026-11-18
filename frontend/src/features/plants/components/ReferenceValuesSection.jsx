import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Card, EmptyState } from "../../../shared/ui";
import { useGrowthPhaseShares } from "../hooks/useGrowthPhaseShares";
import { useSaveGrowthPhaseShare } from "../hooks/useSaveGrowthPhaseShare";
import { useSaveVarietyReference } from "../hooks/useSaveVarietyReference";
import { useVarietyReferences } from "../hooks/useVarietyReferences";
import GrowthPhaseShareItem from "./GrowthPhaseShareItem";
import { SectionError, SectionLoading } from "./HarvestSectionStates";
import VarietyReferenceModal from "./VarietyReferenceModal";
import VarietyReferencesTable from "./VarietyReferencesTable";

const SUBTITLE_CLASS = "text-sm font-semibold text-text";

/**
 * Reference values: the yield per tree and harvest season of each variety,
 * and the share of the full yield given in each growth phase. The forecast
 * and the plant alerts compute from them, for every farm and block, so the
 * block filter does not apply here.
 *
 * Outside the dev profile the varieties start empty, shown as "No reference
 * values yet", while the three phases always have a share: their sourced
 * default until it is corrected. Saving a value refreshes the forecast.
 */
export default function ReferenceValuesSection() {
    const { t } = useTranslation("plants");
    const references = useVarietyReferences();
    const shares = useGrowthPhaseShares();
    const saveReference = useSaveVarietyReference();
    const saveShare = useSaveGrowthPhaseShare();
    // undefined: no dialog; null: a new variety; otherwise the reference to correct.
    const [dialogReference, setDialogReference] = useState(undefined);
    const [editingShareCode, setEditingShareCode] = useState(null);

    function openReferenceForm(reference) {
        saveReference.reset();
        setDialogReference(reference);
    }

    function closeReferenceForm() {
        saveReference.reset();
        setDialogReference(undefined);
    }

    function submitReference(values) {
        saveReference.mutate(
            { id: dialogReference === null ? null : dialogReference.id, reference: values },
            { onSuccess: closeReferenceForm },
        );
    }

    function editShare(code) {
        saveShare.reset();
        setEditingShareCode(code);
    }

    function closeShareForm() {
        saveShare.reset();
        setEditingShareCode(null);
    }

    return (
        <section aria-labelledby="reference-values-title" className="flex flex-col gap-4">
            <div>
                <h2 id="reference-values-title" className="text-lg font-semibold">
                    {t("harvest.references.title")}
                </h2>
                <p className="max-w-2xl text-sm text-text-muted">{t("harvest.references.subtitle")}</p>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3">
                <h3 className={SUBTITLE_CLASS}>{t("harvest.references.varieties")}</h3>
                <Button variant="secondary" icon={Plus} onClick={() => openReferenceForm(null)}>
                    {t("harvest.references.add")}
                </Button>
            </div>

            {references.isPending && <SectionLoading label={t("harvest.references.loading")} />}

            {references.isError && <SectionError title={t("harvest.references.error")} query={references} />}

            {!references.isPending && !references.isError && references.data.length === 0 && (
                <Card>
                    <EmptyState title={t("harvest.references.emptyTitle")} message={t("harvest.references.emptyHint")} />
                </Card>
            )}

            {!references.isPending && !references.isError && references.data.length > 0 && (
                <VarietyReferencesTable references={references.data} onEdit={openReferenceForm} />
            )}

            <h3 className={SUBTITLE_CLASS}>{t("harvest.references.sharesTitle")}</h3>

            {shares.isPending && <SectionLoading label={t("harvest.references.sharesLoading")} />}

            {shares.isError && <SectionError title={t("harvest.references.error")} query={shares} />}

            {!shares.isPending && !shares.isError && (
                <Card>
                    <ul className="flex flex-col gap-6">
                        {shares.data.map((share) => (
                            <GrowthPhaseShareItem
                                key={share.code}
                                share={share}
                                isEditing={editingShareCode === share.code}
                                saveShare={saveShare}
                                onEdit={editShare}
                                onClose={closeShareForm}
                            />
                        ))}
                    </ul>
                </Card>
            )}

            {dialogReference !== undefined && (
                <VarietyReferenceModal
                    reference={dialogReference}
                    isSubmitting={saveReference.isPending}
                    error={saveReference.error}
                    onSubmit={submitReference}
                    onClose={closeReferenceForm}
                />
            )}
        </section>
    );
}
