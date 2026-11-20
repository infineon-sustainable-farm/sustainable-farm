import { useEffect, useState } from "react";
import { ChevronLeft, ChevronRight, Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
    useCancelTimeSlot,
    useCreateTimeSlot,
    useGuides,
    useTimeSlots,
    useUpdateTimeSlot,
} from "../../hooks/useScheduling";
import { addDays, startOfWeek, toIsoDate, useVisitorFormat } from "../../utils/format";
import { SLOT_PRESETS } from "../../utils/scheduling";
import { Button, ConfirmDialog, Field, Modal, PageHeader, Select } from "../../../../shared/ui";
import SchedulingGrid from "../SchedulingGrid";
import SlotForm from "../Forms/SlotForm";
import { LoadError, LoadingBlock } from "../LoadState";

/*
 * Rows of the grid: the two canonical farm slots always show, and any slot stored with a
 * different range adds its own row, so the calendar never hides data the API returned.
 */
function buildRows(slots) {
    const rows = new Map(SLOT_PRESETS.map((preset) => [preset.key, preset]));
    for (const slot of slots) {
        const key = `${slot.startTime}-${slot.endTime}`;
        if (!rows.has(key)) {
            rows.set(key, { key, startTime: slot.startTime, endTime: slot.endTime });
        }
    }
    return [...rows.values()].sort((a, b) => a.startTime.localeCompare(b.startTime));
}

export default function SchedulingPage() {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const [cancelTarget, setCancelTarget] = useState(null);
    const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
    const [guideFilter, setGuideFilter] = useState("");
    const [formState, setFormState] = useState({ open: false, slot: null });
    const currentWeekStart = startOfWeek(new Date());

    useEffect(() => {
        document.title = t("scheduling.documentTitle");
    }, [t]);

    const { data: slots, isPending, isError, refetch, isFetching } = useTimeSlots();
    const { data: guides, isError: guidesError } = useGuides();

    const createMutation = useCreateTimeSlot();
    const updateMutation = useUpdateTimeSlot();
    const cancelMutation = useCancelTimeSlot();

    const days = Array.from({ length: 6 }, (_, index) => addDays(weekStart, index));
    const dayKeys = new Set(days.map(toIsoDate));

    const weekSlots = (slots ?? []).filter((slot) => dayKeys.has(slot.date));
    const visibleSlots = weekSlots.filter(
        (slot) =>
            slot.status !== "CANCELLED" &&
            (guideFilter === "" || String(slot.guideId) === guideFilter),
    );

    const rows = buildRows(visibleSlots);
    const slotsByCell = new Map(
        visibleSlots.map((slot) => [`${slot.date}|${slot.startTime}-${slot.endTime}`, slot]),
    );

    const formSlot = formState.slot;
    const formMutation = formSlot ? updateMutation : createMutation;

    function changeWeek(offset) {
        const target = addDays(weekStart, offset * 7);
        // Past weeks are read-only: no picking a closed week in the past.
        if (target < currentWeekStart) return;
        setWeekStart(target);
        setFormState({ open: false, slot: null });
        createMutation.reset();
        updateMutation.reset();
    }

    function openCreate() {
        createMutation.reset();
        updateMutation.reset();
        setFormState({ open: true, slot: null });
    }

    function openEdit(slot) {
        createMutation.reset();
        updateMutation.reset();
        setFormState({ open: true, slot });
    }

    function closeForm() {
        setFormState({ open: false, slot: null });
        createMutation.reset();
        updateMutation.reset();
    }

    function handleSubmit(payload) {
        if (formSlot) {
            updateMutation.mutate({ id: formSlot.id, data: payload }, { onSuccess: closeForm });
        } else {
            createMutation.mutate(payload, { onSuccess: closeForm });
        }
    }

    function requestCancel(slot) {
        cancelMutation.reset();
        setCancelTarget(slot);
    }

    function closeCancelDialog() {
        cancelMutation.reset();
        setCancelTarget(null);
    }

    function confirmCancel() {
        cancelMutation.mutate(cancelTarget.id, { onSuccess: () => setCancelTarget(null) });
    }

    const isCurrentWeek = weekStart <= currentWeekStart;

    return (
        <div className="space-y-4 p-6">
            <PageHeader
                title={t("scheduling.title")}
                actions={
                    <Button icon={Plus} onClick={openCreate}>
                        {t("scheduling.newSlot")}
                    </Button>
                }
            />

            <div className="flex flex-wrap items-end gap-4">
                <div className="flex w-full items-center gap-2 sm:w-auto">
                    <Button
                        variant="secondary"
                        size="sm"
                        icon={ChevronLeft}
                        onClick={() => changeWeek(-1)}
                        disabled={isCurrentWeek}
                        title={isCurrentWeek ? t("scheduling.pastWeeksReadOnly") : t("scheduling.previousWeek")}
                    >
                        {t("scheduling.prev")}
                    </Button>
                    <span className="min-w-0 flex-1 text-center text-sm font-semibold text-text sm:min-w-44 sm:flex-none">
                        {format.weekLabel(weekStart)}
                    </span>
                    <Button variant="secondary" size="sm" onClick={() => changeWeek(1)}>
                        {t("scheduling.next")}
                        <ChevronRight size={16} aria-hidden="true" />
                    </Button>
                </div>

                <Field className="min-w-40" label={t("scheduling.guide")} disabled={Boolean(guidesError)}>
                    <Select value={guideFilter} onChange={(event) => setGuideFilter(event.target.value)}>
                        <option value="">{t("scheduling.allGuides")}</option>
                        {(guides ?? []).map((guide) => (
                            <option key={guide.id} value={guide.id}>
                                {guide.fullName}
                            </option>
                        ))}
                    </Select>
                </Field>
            </div>

            {isPending && <LoadingBlock label={t("scheduling.loading")} />}

            {isError && (
                <LoadError title={t("scheduling.loadError")} onRetry={() => refetch()} retrying={isFetching} />
            )}

            {!isPending && !isError && (
                <>
                    <SchedulingGrid
                        rows={rows}
                        days={days}
                        slotsByCell={slotsByCell}
                        onEdit={openEdit}
                        onCancel={requestCancel}
                        cancelPendingId={cancelMutation.isPending ? cancelMutation.variables : null}
                    />
                    <p className="text-xs text-text-muted">{t("scheduling.footnote")}</p>
                </>
            )}

            <Modal
                open={formState.open}
                onClose={closeForm}
                size="xl"
                dismissible={!formMutation.isPending}
                title={formSlot ? t("scheduling.editSlot") : t("scheduling.newSlot")}
            >
                {formState.open && (
                    <SlotForm
                        key={`${toIsoDate(weekStart)}-${formSlot?.id ?? "new"}`}
                        weekStart={weekStart}
                        guides={guides ?? []}
                        guidesError={guidesError}
                        slot={formSlot}
                        isSubmitting={formMutation.isPending}
                        submitError={formMutation.error?.message}
                        onSubmit={handleSubmit}
                        onCancel={closeForm}
                    />
                )}
            </Modal>

            <ConfirmDialog
                open={Boolean(cancelTarget)}
                title={t("scheduling.cancelTitle")}
                message={t("scheduling.cancelMessage")}
                confirmLabel={t("scheduling.yes")}
                cancelLabel={t("scheduling.no")}
                onClose={closeCancelDialog}
                onConfirm={confirmCancel}
                loading={cancelMutation.isPending}
                error={cancelMutation.isError ? cancelMutation.error.message : null}
            />
        </div>
    );
}
