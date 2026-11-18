import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";

/*
 * Tone of the kit StatusBadge per raw domain value, shared by every screen of the module. An
 * unknown value falls back to neutral instead of pretending it is a known state.
 */
export const STATUS_TONES = {
    PENDING: "warning",
    DRAFT: "neutral",
    PUBLISHED: "success",
    ACTIVE: "success",
    INACTIVE: "neutral",
    CONFIRMED: "success",
    COMPLETED: "info",
    CHECKED_IN: "info",
    REJECTED: "error",
    CANCELLED: "error",
    PAID: "success",
    UNPAID: "warning",
    REFUNDED: "info",
    AVAILABLE: "success",
    RESERVED: "info",
    FULL: "warning",
    SENT: "info",
    RECEIVED: "success",
    DONE: "success",
    NOT_DONE: "error",
};

/** Tone of the kit StatusBadge for a raw status value. */
export function statusTone(value) {
    return STATUS_TONES[value] ?? "neutral";
}

/**
 * Translated labels of the module's API values: `labels.enum("CHECKED_IN")` gives "Checked In",
 * `labels.language("Moore")` gives "Moore". Every enum of the backend shares the flat `enums`
 * map (the same value reads the same in every enum); an unknown value keeps the old
 * "Checked In" style label, an unknown language its raw value.
 */
export function useLabels() {
    const { t } = useTranslation("visitormanagement");
    return useMemo(
        () => ({
            enum(value) {
                if (!value) return t("format.none");
                return t(`enums.${value}`, { defaultValue: formatEnumLabel(value) });
            },
            language(value) {
                if (!value) return t("format.none");
                return t(`languages.${value}`, { defaultValue: value });
            },
        }),
        [t],
    );
}
