import { useTranslation } from "react-i18next";
import { Button } from "../../../../../shared/ui";
import { MODES } from "./rules";

/** The three registration targets: farm tour, event and activity booking. */
export default function ModeSwitch({ mode, onModeChange }) {
    const { t } = useTranslation("visitormanagement");
    return (
        <div className="mb-4 flex flex-wrap gap-2" role="group" aria-label={t("registration.modes.label")}>
            {MODES.map((choice) => (
                <Button
                    key={choice.value}
                    size="sm"
                    variant={mode === choice.value ? "primary" : "secondary"}
                    aria-pressed={mode === choice.value}
                    onClick={() => onModeChange(choice.value)}
                >
                    {t(choice.labelKey)}
                </Button>
            ))}
        </div>
    );
}
