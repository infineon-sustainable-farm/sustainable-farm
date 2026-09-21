import { useEffect, useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import {
    useCancelTimeSlot,
    useCreateTimeSlot,
    useGuides,
    useTimeSlots,
    useUpdateTimeSlot,
} from "../../hooks/useScheduling";
import { addDays, formatWeekLabel, startOfWeek, toIsoDate } from "../../utils/format";
import { SLOT_PRESETS } from "../../utils/scheduling";
import SchedulingGrid from "../SchedulingGrid";
import Modal from "../Modal";
import SlotForm from "../Forms/SlotForm";

/*
 * Rows of the grid: the two canonical farm slots always show, and any slot
 * stored with a different range adds its own row, so the calendar never hides
 * data the API returned.
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
    const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
    const [guideFilter, setGuideFilter] = useState("");
    const [formState, setFormState] = useState({ open: false, slot: null });
    const currentWeekStart = startOfWeek(new Date());

    useEffect(() => {
        document.title = "Farm Tour Scheduling — Visitor Management";
    }, []);

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

    function handleCancelSlot(slot) {
        cancelMutation.mutate(slot.id);
    }

    const cancelError = cancelMutation.isError
        ? { slotId: cancelMutation.variables, message: cancelMutation.error.message }
        : null;

    return (
        <div className="min-h-full bg-[#F5F7FA] px-8 py-7">
            <section className="rounded-lg border border-line bg-white p-7">
                <h2 className="font-heading mb-5 inline-block border-b-[3px] border-accent pb-2 text-2xl font-bold text-primary-dark">
                    Farm tour scheduling
                </h2>

                <div className="mb-4.5 flex flex-wrap items-center gap-3.5">
                    <div className="flex items-center gap-2 rounded-md border border-line bg-[#F2FBF9] px-2.5 py-1.5">
                        <button
                            type="button"
                            onClick={() => changeWeek(-1)}
                            disabled={weekStart <= currentWeekStart}
                            title={
                                weekStart <= currentWeekStart
                                    ? "Past weeks are read-only"
                                    : "Previous week"
                            }
                            className="rounded border border-line bg-white px-3.5 py-1.5 text-xs text-primary hover:bg-[#F2FBF9] disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            ‹ Prev
                        </button>
                        <span className="min-w-[170px] text-center text-[13px] font-semibold text-primary-dark">
                            {formatWeekLabel(weekStart)}
                        </span>
                        <button
                            type="button"
                            onClick={() => changeWeek(1)}
                            className="rounded border border-line bg-white px-3.5 py-1.5 text-xs text-primary hover:bg-[#F2FBF9]"
                        >
                            Next ›
                        </button>
                    </div>

                    <div className="flex items-end gap-2.5">
                        <div>
                            <label
                                htmlFor="guide-filter"
                                className="mb-1 block text-xs tracking-wide text-primary uppercase"
                            >
                                Guide
                            </label>
                            <select
                                id="guide-filter"
                                value={guideFilter}
                                onChange={(event) => setGuideFilter(event.target.value)}
                                disabled={Boolean(guidesError)}
                                className="min-w-[150px] rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm disabled:opacity-60"
                            >
                                <option value="">All guides</option>
                                {(guides ?? []).map((guide) => (
                                    <option key={guide.id} value={guide.id}>
                                        {guide.fullName}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <button
                        type="button"
                        onClick={openCreate}
                        className="ml-auto rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark"
                    >
                        + New slot
                    </button>
                </div>

                {formState.open && (
                    <Modal
                        title={formSlot ? "Edit slot" : "New slot"}
                        onClose={closeForm}
                    >
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
                    </Modal>
                )}

                {isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-16 text-sm text-muted">
                        <Loader2 size={18} className="animate-spin" />
                        Loading time slots…
                    </div>
                )}

                {isError && (
                    <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-ink">
                                Time slots could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-muted">
                                The request to the server did not succeed.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetch()}
                            disabled={isFetching}
                            className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:opacity-60"
                        >
                            {isFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!isPending && !isError && (
                    <>
                        <SchedulingGrid
                            rows={rows}
                            days={days}
                            slotsByCell={slotsByCell}
                            onEdit={openEdit}
                            onCancel={handleCancelSlot}
                            cancelPendingId={
                                cancelMutation.isPending ? cancelMutation.variables : null
                            }
                            cancelError={cancelError}
                        />
                        <p className="mt-6 text-[11px] text-muted">
                            Max capacity: 10 visitors/slot · 2 slots per day · closed on Sundays.
                            Past weeks are read-only. Cancelling a slot also cancels its
                            registrations and bookings.
                        </p>
                    </>
                )}
            </section>
        </div>
    );
}