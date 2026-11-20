// Helpers shared by the nursery dialogs (kept out of the component files for fast refresh).

/** Id of the note under a field of the nursery dialogs. */
export function noteId(id) {
    return `${id}-note`;
}

/** Radios of the nursery dialogs (the kit has no radio): primary accent and the kit's keyboard focus mark. */
export const RADIO_CLASS =
    "accent-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring";
