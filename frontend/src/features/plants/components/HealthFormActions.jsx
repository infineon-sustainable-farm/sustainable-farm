import { useTranslation } from "react-i18next";
import { Button } from "../../../shared/ui";

/** Cancel and the submit button at the bottom of a Diseases & Alerts form; the label changes while it sends. */
export default function HealthFormActions({ onCancel, isSubmitting, submitLabel, submittingLabel }) {
    const { t } = useTranslation("common");
    return (
        <div className="mt-6 flex justify-end gap-2">
            <Button variant="secondary" onClick={onCancel}>
                {t("actions.cancel")}
            </Button>
            <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? submittingLabel : submitLabel}
            </Button>
        </div>
    );
}
