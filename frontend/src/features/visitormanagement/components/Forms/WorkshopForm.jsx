import { useState } from "react";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

/**
 * Inline workshop form of the mockup, used both to create (status DRAFT) and
 * to edit a workshop. The mockup's "assign" action has no endpoint of its own:
 * assigning a facilitator is editing the facilitator field, which this form
 * does. Facilitators are the guides, so the field is a select built from the
 * active guides; a previously stored facilitator who is no longer in the list
 * keeps an option so the record can be saved unchanged.
 */
export default function WorkshopForm({
    workshop,
    guides,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const isEditing = Boolean(workshop);

    const [name, setName] = useState(workshop?.name ?? "");
    const [durationMinutes, setDurationMinutes] = useState(
        workshop ? String(workshop.durationMinutes) : "",
    );
    const [targetGroup, setTargetGroup] = useState(workshop?.targetGroup ?? "");
    const [facilitator, setFacilitator] = useState(workshop?.facilitator ?? "");
    const [description, setDescription] = useState(workshop?.description ?? "");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!name.trim()) {
            errors.name = "Name is required.";
        } else if (name.trim().length > 150) {
            errors.name = "Name must be at most 150 characters.";
        }
        const duration = Number(durationMinutes);
        if (!Number.isInteger(duration) || duration < 1) {
            errors.durationMinutes = "Duration must be a whole number of at least 1 minute.";
        }
        if (!targetGroup.trim()) {
            errors.targetGroup = "Target group is required.";
        } else if (targetGroup.trim().length > 60) {
            errors.targetGroup = "Target group must be at most 60 characters.";
        }
        if (description.trim().length > 1000) {
            errors.description = "Description must be at most 1000 characters.";
        }
        return errors;
    }

    const facilitatorInList = (guides ?? []).some(
        (guide) => guide.fullName === workshop?.facilitator,
    );
    const hasLegacyFacilitator = isEditing && workshop?.facilitator && !facilitatorInList;

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            name: name.trim(),
            durationMinutes: Number(durationMinutes),
            targetGroup: targetGroup.trim(),
            facilitator: facilitator || null,
            description: description.trim() || null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form
            onSubmit={handleSubmit}
           
        >
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                    <label htmlFor="ws-name" className={LABEL_CLASS}>
                        Workshop name
                    </label>
                    <input
                        id="ws-name"
                        type="text"
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                        placeholder="e.g. Mango tasting"
                        className={INPUT_CLASS}
                    />
                    {fieldError("name") && <p className={ERROR_CLASS}>{fieldError("name")}</p>}
                </div>

                <div>
                    <label htmlFor="ws-duration" className={LABEL_CLASS}>
                        Duration (min)
                    </label>
                    <input
                        id="ws-duration"
                        type="number"
                        min="1"
                        value={durationMinutes}
                        onChange={(event) => setDurationMinutes(event.target.value)}
                        placeholder="e.g. 40"
                        className={INPUT_CLASS}
                    />
                    {fieldError("durationMinutes") && (
                        <p className={ERROR_CLASS}>{fieldError("durationMinutes")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="ws-target" className={LABEL_CLASS}>
                        Target group
                    </label>
                    <input
                        id="ws-target"
                        type="text"
                        value={targetGroup}
                        onChange={(event) => setTargetGroup(event.target.value)}
                        placeholder="e.g. General public, School, Professional"
                        className={INPUT_CLASS}
                    />
                    {fieldError("targetGroup") && (
                        <p className={ERROR_CLASS}>{fieldError("targetGroup")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="ws-facilitator" className={LABEL_CLASS}>
                        Facilitator (guide)
                    </label>
                    <select
                        id="ws-facilitator"
                        value={facilitator}
                        onChange={(event) => setFacilitator(event.target.value)}
                        className={INPUT_CLASS}
                    >
                        <option value="">Select a guide…</option>
                        {hasLegacyFacilitator && (
                            <option value={workshop.facilitator}>{workshop.facilitator}</option>
                        )}
                        {(guides ?? []).map((guide) => (
                            <option key={guide.id} value={guide.fullName}>
                                {guide.fullName}
                            </option>
                        ))}
                    </select>
                    {fieldError("facilitator") && (
                        <p className={ERROR_CLASS}>{fieldError("facilitator")}</p>
                    )}
                </div>
            </div>

            <div className="mt-4">
                <label htmlFor="ws-description" className={LABEL_CLASS}>
                    Description
                </label>
                <input
                    id="ws-description"
                    type="text"
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    placeholder="Short description"
                    className={INPUT_CLASS}
                />
                {fieldError("description") && (
                    <p className={ERROR_CLASS}>{fieldError("description")}</p>
                )}
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
                    {isSubmitting
                        ? "Saving…"
                        : isEditing
                          ? "Save workshop"
                          : "Create workshop"}
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