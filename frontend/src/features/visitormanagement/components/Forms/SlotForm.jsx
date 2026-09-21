import { useState } from "react";
import { addDays, formatTimeRange, formatWeekday, toIsoDate } from "../../utils/format";
import { SLOT_PRESETS } from "../../utils/scheduling";

function rangeKey(startTime, endTime) {
    return `${startTime}-${endTime}`;
}

/**
 * The inline create/edit form of the mockup. It is fully controlled by the
 * page, which owns the mutation and its pending/error state, so the form only
 * validates and reports a payload.
 */
export default function SlotForm({
    weekStart,
    guides,
    guidesError,
    slot,
    isSubmitting,
    submitError,
    onSubmit,
    onCancel,
}) {
    const isEditing = Boolean(slot);

    const ranges = [...SLOT_PRESETS];
    if (slot && !ranges.some((range) => range.key === rangeKey(slot.startTime, slot.endTime))) {
        ranges.push({
            key: rangeKey(slot.startTime, slot.endTime),
            startTime: slot.startTime,
            endTime: slot.endTime,
        });
    }

    const [date, setDate] = useState(slot ? slot.date : toIsoDate(weekStart));
    const [range, setRange] = useState(
        slot ? rangeKey(slot.startTime, slot.endTime) : SLOT_PRESETS[0].key,
    );
    const [maxCapacity, setMaxCapacity] = useState(slot ? String(slot.maxCapacity) : "10");
    const [guideId, setGuideId] = useState(slot?.guideId ? String(slot.guideId) : "");
    const [fieldErrors, setFieldErrors] = useState({});

    // Monday to Saturday of the displayed week, as the mockup's day selector.
    const days = Array.from({ length: 6 }, (_, index) => addDays(weekStart, index));

    function handleSubmit(event) {
        event.preventDefault();

        const errors = {};
        const capacity = Number(maxCapacity);
        if (!Number.isInteger(capacity) || capacity < 1 || capacity > 10) {
            errors.maxCapacity = "Capacity must be a whole number between 1 and 10.";
        }
        if (!date) {
            errors.date = "Pick a day.";
        }
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        const selectedRange = ranges.find((item) => item.key === range);
        onSubmit({
            date,
            startTime: selectedRange.startTime,
            endTime: selectedRange.endTime,
            maxCapacity: capacity,
            guideId: guideId === "" ? null : Number(guideId),
        });
    }

    return (
        <form
            onSubmit={handleSubmit}
           
        >
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
                <div>
                    <label
                        htmlFor="slot-day"
                        className="mb-1.5 block text-xs tracking-wide text-primary uppercase"
                    >
                        Day
                    </label>
                    <select
                        id="slot-day"
                        value={date}
                        onChange={(event) => setDate(event.target.value)}
                        className="w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                    >
                        {days.map((day) => (
                            <option key={toIsoDate(day)} value={toIsoDate(day)}>
                                {formatWeekday(day)} {day.getDate()}
                            </option>
                        ))}
                    </select>
                    {fieldErrors.date && (
                        <p className="mt-1 text-xs text-error">{fieldErrors.date}</p>
                    )}
                </div>

                <div>
                    <label
                        htmlFor="slot-range"
                        className="mb-1.5 block text-xs tracking-wide text-primary uppercase"
                    >
                        Time slot
                    </label>
                    <select
                        id="slot-range"
                        value={range}
                        onChange={(event) => setRange(event.target.value)}
                        className="w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                    >
                        {ranges.map((item) => (
                            <option key={item.key} value={item.key}>
                                {formatTimeRange(item.startTime, item.endTime)}
                            </option>
                        ))}
                    </select>
                </div>

                <div>
                    <label
                        htmlFor="slot-capacity"
                        className="mb-1.5 block text-xs tracking-wide text-primary uppercase"
                    >
                        Capacity
                    </label>
                    <input
                        id="slot-capacity"
                        type="number"
                        min="1"
                        max="10"
                        value={maxCapacity}
                        onChange={(event) => setMaxCapacity(event.target.value)}
                        className="w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                    />
                    {fieldErrors.maxCapacity && (
                        <p className="mt-1 text-xs text-error">{fieldErrors.maxCapacity}</p>
                    )}
                </div>

                <div>
                    <label
                        htmlFor="slot-guide"
                        className="mb-1.5 block text-xs tracking-wide text-primary uppercase"
                    >
                        Assigned guide
                    </label>
                    <select
                        id="slot-guide"
                        value={guideId}
                        onChange={(event) => setGuideId(event.target.value)}
                        disabled={Boolean(guidesError)}
                        className="w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm disabled:opacity-60"
                    >
                        <option value="">Unassigned</option>
                        {(guides ?? []).map((guide) => (
                            <option key={guide.id} value={guide.id}>
                                {guide.fullName}
                            </option>
                        ))}
                    </select>
                    {guidesError && (
                        <p className="mt-1 text-xs text-muted">
                            Guides could not be loaded — the slot will be saved unassigned.
                        </p>
                    )}
                </div>
            </div>

            {submitError && (
                <p className="mt-4 rounded-md border border-error/30 bg-error/5 px-3 py-2 text-sm text-error">
                    {submitError}
                </p>
            )}

            <div className="mt-4 flex gap-2.5">
                <button
                    type="submit"
                    disabled={isSubmitting}
                    className="rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {isSubmitting ? "Saving…" : isEditing ? "Save slot" : "Create slot"}
                </button>
                <button
                    type="button"
                    onClick={onCancel}
                    disabled={isSubmitting}
                    className="rounded-md border border-line bg-white px-4 py-2.5 text-xs text-primary hover:bg-[#F2FBF9] disabled:opacity-60"
                >
                    Cancel
                </button>
            </div>
        </form>
    );
}