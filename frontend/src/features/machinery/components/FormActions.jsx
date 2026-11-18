import { useTranslation } from "react-i18next";
import { Button } from "../../../shared/ui";

/** Cancel and Save at the bottom of a machinery form, Save submits the form. */
function FormActions({ onCancel, submitDisabled = false }) {
  const { t } = useTranslation("common");
  return (
    <div className="mt-2 flex justify-end gap-2">
      <Button variant="secondary" onClick={onCancel}>
        {t("actions.cancel")}
      </Button>
      <Button type="submit" disabled={submitDisabled}>
        {t("actions.save")}
      </Button>
    </div>
  );
}
export default FormActions;
