import { useState } from "react";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

/**
 * Tour stop create/edit form. The position is unique among active stops (the
 * API answers 409 otherwise), and there is no reactivation call, so a
 * deactivated stop stays out of the timeline.
 */
export default function TourStopForm({
    stop,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const isEditing = Boolean(stop);

    const [name, setName] = useState(stop?.name ?? "");
    const [position, setPosition] = useState(stop ? String(stop.position) : "");
    const [durationMinutes, setDurationMinutes] = useState(
        stop ? String(stop.durationMinutes) : "",
    );
    const [maxCapacity, setMaxCapacity] = useState(stop ? String(stop.maxCapacity) : "");
    const [location, setLocation] = useState(stop?.location ?? "");
    const [demo, setDemo] = useState(stop?.demo ?? "");
    const [safetyNotes, setSafetyNotes] = useState(stop?.safetyNotes ?? "");
    const [description, setDescription] = useState(stop?.description ?? "");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!name.trim()) {
            errors.name = "Name is required.";
        } else if (name.trim().length > 150) {
            errors.name = "Name must be at most 150 characters.";
        }
        const positionNumber = Number(position);
        if (!Number.isInteger(positionNumber) || positionNumber < 1) {
            errors.position = "Position must be a whole number of at least 1.";
        }
        const durationNumber = Number(durationMinutes);
        if (!Number.isInteger(durationNumber) || durationNumber < 1) {
            errors.durationMinutes = "Duration must be a whole number of at least 1 minute.";
        }
        const capacityNumber = Number(maxCapacity);
        if (!Number.isInteger(capacityNumber) || capacityNumber < 1) {
            errors.maxCapacity = "Capacity must be a whole number of at least 1.";
        }
        if (location.trim().length > 150) {
            errors.location = "Location must be at most 150 characters.";
        }
        if (demo.trim().length > 1000) {
            errors.demo = "Demo must be at most 1000 characters.";
        }
        if (safetyNotes.trim().length > 1000) {
            errors.safetyNotes = "Safety notes must be at most 1000 characters.";
        }
        if (description.trim().length > 2000) {
            errors.description = "Description must be at most 2000 characters.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            name: name.trim(),
            position: Number(position),
            durationMinutes: Number(durationMinutes),
            maxCapacity: Number(maxCapacity),
            location: location.trim() || null,
            demo: demo.trim() || null,
            safetyNotes: safetyNotes.trim() || null,
            description: description.trim() || null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form
            onSubmit={handleSubmit}
            className="mb-4.5 rounded-lg border border-dashed border-primary bg-[#F2FBF9] p-4.5"
        >
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                <div>
                    <label htmlFor="ts-name" className={LABEL_CLASS}>
                        Stop name
                    </label>
                    <input
                        id="ts-name"
                        type="text"
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                        placeholder="e.g. Mango orchard"
                        className={INPUT_CLASS}
                    />
                    {fieldError("name") && <p className={ERROR_CLASS}>{fieldError("name")}</p>}
                </div>

                <div>
                    <label htmlFor="ts-position" className={LABEL_CLASS}>
                        Position
                    </label>
                    <input
                        id="ts-position"
                        type="number"
                        min="1"
                        value={position}
                        onChange={(event) => setPosition(event.target.value)}
                        placeholder="e.g. 2"
                        className={INPUT_CLASS}
                    />
                    {fieldError("position") && (
                        <p className={ERROR_CLASS}>{fieldError("position")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="ts-duration" className={LABEL_CLASS}>
                        Duration (min)
                    </label>
                    <input
                        id="ts-duration"
                        type="number"
                        min="1"
                        value={durationMinutes}
                        onChange={(event) => setDurationMinutes(event.target.value)}
                        className={INPUT_CLASS}
                    />
                    {fieldError("durationMinutes") && (
                        <p className={ERROR_CLASS}>{fieldError("durationMinutes")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="ts-capacity" className={LABEL_CLASS}>
                        Max capacity
                    </label>
                    <input
                        id="ts-capacity"
                        type="number"
                        min="1"
                        value={maxCapacity}
                        onChange={(event) => setMaxCapacity(event.target.value)}
                        className={INPUT_CLASS}
                    />
                    {fieldError("maxCapacity") && (
                        <p className={ERROR_CLASS}>{fieldError("maxCapacity")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="ts-location" className={LABEL_CLASS}>
                        Location
                    </label>
                    <input
                        id="ts-location"
                        type="text"
                        value={location}
                        onChange={(event) => setLocation(event.target.value)}
                        placeholder="e.g. Orchard"
                        className={INPUT_CLASS}
                    />
                    {fieldError("location") && (
                        <p className={ERROR_CLASS}>{fieldError("location")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="ts-demo" className={LABEL_CLASS}>
                        Demo
                    </label>
                    <input
                        id="ts-demo"
                        type="text"
                        value={demo}
                        onChange={(event) => setDemo(event.target.value)}
                        placeholder="e.g. Drip line layout"
                        className={INPUT_CLASS}
                    />
                    {fieldError("demo") && <p className={ERROR_CLASS}>{fieldError("demo")}</p>}
                </div>
            </div>

            <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                    <label htmlFor="ts-safety" className={LABEL_CLASS}>
                        Safety notes
                    </label>
                    <input
                        id="ts-safety"
                        type="text"
                        value={safetyNotes}
                        onChange={(event) => setSafetyNotes(event.target.value)}
                        placeholder="e.g. Stay on the paths"
                        className={INPUT_CLASS}
                    />
                    {fieldError("safetyNotes") && (
                        <p className={ERROR_CLASS}>{fieldError("safetyNotes")}</p>
                    )}
                </div>
                <div>
                    <label htmlFor="ts-description" className={LABEL_CLASS}>
                        Description
                    </label>
                    <input
                        id="ts-description"
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
                    {isSubmitting ? "Saving…" : isEditing ? "Save stop" : "Create stop"}
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