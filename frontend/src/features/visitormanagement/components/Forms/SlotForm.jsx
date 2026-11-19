import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Select } from "../../../../shared/ui";
import { addDays, toIsoDate, useVisitorFormat } from "../../utils/format";
import { SLOT_PRESETS } from "../../utils/scheduling";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";

function rangeKey(startTime, endTime) {
    return `${startTime}-${endTime}`;
}

/**
 * The create/edit form of a time slot. It is fully controlled by the page, which owns the
 * mutation and its pending/error state, so the form only validates and reports a payload.
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
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
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
    const [range, setRange] = useState(slot ? rangeKey(slot.startTime, slot.endTime) : SLOT_PRESETS[0].key);
    const [maxCapacity, setMaxCapacity] = useState(slot ? String(slot.maxCapacity) : "10");
    const [guideId, setGuideId] = useState(slot?.guideId ? String(slot.guideId) : "");
    const [fieldErrors, setFieldErrors] = useState({});

    // Monday to Saturday of the displayed week.
    const days = Array.from({ length: 6 }, (_, index) => addDays(weekStart, index));

    function handleSubmit(event) {
        event.preventDefault();

        const errors = {};
        const capacity = Number(maxCapacity);
        if (!Number.isInteger(capacity) || capacity < 1) {
            errors.maxCapacity = t("scheduling.capacityInvalid");
        }
        if (!date) {
            errors.date = t("scheduling.dayRequired");
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
        <form onSubmit={handleSubmit}>
            {submitError && <FormAlert className="mb-4">{submitError}</FormAlert>}

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Field label={t("scheduling.day")} error={fieldErrors.date}>
                    <Select value={date} onChange={(event) => setDate(event.target.value)}>
                        {days.map((day) => (
                            <option key={toIsoDate(day)} value={toIsoDate(day)}>
                                {t("scheduling.dayOption", { weekday: format.weekday(day), day: day.getDate() })}
                            </option>
                        ))}
                    </Select>
                </Field>

                <Field label={t("scheduling.timeSlot")}>
                    <Select value={range} onChange={(event) => setRange(event.target.value)}>
                        {ranges.map((item) => (
                            <option key={item.key} value={item.key}>
                                {format.timeRange(item.startTime, item.endTime)}
                            </option>
                        ))}
                    </Select>
                </Field>

                <Field label={t("scheduling.capacity")} error={fieldErrors.maxCapacity}>
                    <Input
                        type="number"
                        min="1"
                        value={maxCapacity}
                        onChange={(event) => setMaxCapacity(event.target.value)}
                    />
                </Field>

                <Field
                    label={t("scheduling.assignedGuide")}
                    hint={guidesError ? t("scheduling.guidesUnavailable") : undefined}
                    disabled={Boolean(guidesError)}
                >
                    <Select value={guideId} onChange={(event) => setGuideId(event.target.value)}>
                        <option value="">{t("scheduling.unassigned")}</option>
                        {(guides ?? []).map((guide) => (
                            <option key={guide.id} value={guide.id}>
                                {guide.fullName}
                            </option>
                        ))}
                    </Select>
                </Field>
            </div>

            <FormActions
                onCancel={onCancel}
                isSubmitting={isSubmitting}
                submitLabel={isEditing ? t("scheduling.saveSlot") : t("scheduling.createSlot")}
            />
        </form>
    );
}
