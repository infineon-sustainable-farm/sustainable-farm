import { Field } from "../../../shared/ui";
import { noteId } from "./nurseryForm";

/**
 * A field of the nursery dialogs: the kit Field (label, error) plus a note that
 * stays shown under the error, as the dialogs always did. The control passes
 * `aria-describedby={noteId(id)}` when there is a note; the Field adds the error.
 */
export default function NurseryField({ id, label, error, note, children }) {
    return (
        <div>
            <Field id={id} label={label} error={error}>
                {children}
            </Field>
            {note && (
                <p id={noteId(id)} className="mt-1 text-xs text-text-muted">
                    {note}
                </p>
            )}
        </div>
    );
}
